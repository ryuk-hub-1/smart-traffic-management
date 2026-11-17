package com.smarttraffic;

import com.smarttraffic.model.*;
import com.smarttraffic.model.enums.*;
import com.smarttraffic.service.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PipelineIntegrationTest {

    private static AuthService authService;
    private static OfficerService officerService;
    private static JunctionService junctionService;
    private static DutyService dutyService;
    private static ChallanService challanService;
    private static ComplaintService complaintService;

    @BeforeAll
    public static void setUp() {
        authService = AuthService.getInstance();
        officerService = OfficerService.getInstance();
        junctionService = JunctionService.getInstance();
        dutyService = DutyService.getInstance();
        challanService = ChallanService.getInstance();
        complaintService = ComplaintService.getInstance();
    }

    /**
     * PIPELINE SCENARIO 1:
     * Officer logs violation and issues e-Challan.
     * Citizen logs in, views the pending fine, and pays online.
     * Verification: Status changes to PAID, payment reference recorded, balance cleared.
     */
    @Test
    @Order(1)
    public void testPipeline_OfficerIssueChallan_CitizenPayOnline() throws Exception {
        // Step 1: Officer logs in
        User officerUser = authService.login("officer1", "officer123");
        assertNotNull(officerUser);
        assertTrue(officerUser instanceof Officer);
        Officer officer = (Officer) officerUser;

        // Step 2: Officer issues a new citation for Citizen Amit Patel's car "KA-01-AB-1234"
        List<ViolationType> vTypes = challanService.getAllViolationTypes();
        ViolationType redLight = vTypes.stream()
                .filter(v -> v.getTitle().toLowerCase().contains("red light") || v.getViolationCode().contains("SIG"))
                .findFirst().orElse(vTypes.get(0));

        Challan newChallan = new Challan(
                0, null, "KA-01-AB-1234", VehicleType.FOUR_WHEELER_CAR,
                "Amit Patel", "+91 97444 55667",
                officer.getOfficerId(), 1, redLight.getId(),
                LocalDateTime.now(), 1000.0,
                ChallanStatus.PENDING, null, null,
                "camera_proof_pipeline.jpg", "Crossed stop line at red light"
        );

        Challan issued = challanService.issueChallan(newChallan);
        assertNotNull(issued);
        assertNotNull(issued.getChallanNumber());
        assertEquals(ChallanStatus.PENDING, issued.getPaymentStatus());

        // Step 3: Citizen Amit Patel logs in
        User citizenUser = authService.login("citizen1", "citizen123");
        assertNotNull(citizenUser);
        assertTrue(citizenUser instanceof Citizen);
        Citizen citizen = (Citizen) citizenUser;
        assertEquals("KA-01-AB-1234", citizen.getRegisteredVehicleNumber());

        // Step 4: Citizen fetches their challans
        List<Challan> citizenChallans = challanService.getChallansForVehicle(citizen.getRegisteredVehicleNumber());
        assertTrue(citizenChallans.stream().anyMatch(c -> c.getChallanNumber().equals(issued.getChallanNumber())));

        // Step 5: Citizen pays the fine online
        String txnRef = "UPI-GATEWAY-TXN-" + System.currentTimeMillis();
        boolean paidSuccess = challanService.payChallan(issued.getId(), txnRef);
        assertTrue(paidSuccess);

        // Step 6: Verify Challan is now settled across the entire database
        Optional<Challan> settled = challanService.getChallanById(issued.getId());
        assertTrue(settled.isPresent());
        assertEquals(ChallanStatus.PAID, settled.get().getPaymentStatus());
        assertEquals(txnRef, settled.get().getPaymentReference());
        assertNotNull(settled.get().getPaymentDate());
    }

    /**
     * PIPELINE SCENARIO 2:
     * Citizen files a traffic grievance (e.g. Illegal Parking).
     * Admin reviews grievance and assigns Officer.
     * Officer investigates on the ground, takes action, and marks RESOLVED.
     * Citizen checks Complaint History and sees resolution notes.
     */
    @Test
    @Order(2)
    public void testPipeline_CitizenComplaint_AdminAssign_OfficerResolve() throws Exception {
        // Step 1: Citizen logs in and files complaint
        User citizenUser = authService.login("citizen1", "citizen123");
        Citizen citizen = (Citizen) citizenUser;

        Complaint complaint = new Complaint();
        complaint.setCitizenId(citizen.getId());
        complaint.setComplaintType("Illegal Parking / Footpath Obstruction");
        complaint.setVehicleNumber("KA-04-MH-7711");
        complaint.setJunctionId(1);
        complaint.setDescription("Delivery truck parked on pedestrian sidewalk blocking pram access.");
        complaint.setIncidentDate(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.PENDING);

        Complaint registered = complaintService.fileComplaint(complaint);
        assertNotNull(registered);
        assertNotNull(registered.getComplaintNumber());
        assertEquals(ComplaintStatus.PENDING, registered.getStatus());

        // Step 2: Admin logs in and assigns Officer Vikram Sharma
        User admin = authService.login("admin", "admin123");
        assertEquals(Role.ADMIN, admin.getRole());

        List<Officer> officers = officerService.getAllOfficers();
        Officer assignedOfficer = officers.get(0);

        boolean assigned = complaintService.updateComplaintStatus(
                registered.getId(),
                ComplaintStatus.INVESTIGATING,
                assignedOfficer.getOfficerId(),
                "Dispatching field patrol unit to inspect parking obstruction."
        );
        assertTrue(assigned);

        // Step 3: Officer logs in and checks assigned complaints
        List<Complaint> officerGrievances = complaintService.getComplaintsForOfficer(assignedOfficer.getOfficerId());
        assertTrue(officerGrievances.stream().anyMatch(c -> c.getId() == registered.getId()));

        // Step 4: Officer resolves the complaint
        boolean resolved = complaintService.updateComplaintStatus(
                registered.getId(),
                ComplaintStatus.RESOLVED,
                assignedOfficer.getOfficerId(),
                "Vehicle towed away. Citation issued to truck driver. Sidewalk cleared."
        );
        assertTrue(resolved);

        // Step 5: Citizen checks complaint history
        List<Complaint> myHistory = complaintService.getComplaintsByCitizen(citizen.getId());
        Optional<Complaint> myGrievance = myHistory.stream().filter(c -> c.getId() == registered.getId()).findFirst();
        assertTrue(myGrievance.isPresent());
        assertEquals(ComplaintStatus.RESOLVED, myGrievance.get().getStatus());
        assertTrue(myGrievance.get().getOfficerActionNotes().contains("Vehicle towed away"));
    }

    /**
     * PIPELINE SCENARIO 3:
     * Admin allocates a scheduled duty shift to an officer.
     * Officer checks in -> duty status transitions to IN_PROGRESS.
     * Officer checks out -> duty status transitions to COMPLETED.
     */
    @Test
    @Order(3)
    public void testPipeline_DutyAllocation_CheckIn_CheckOut() throws Exception {
        // Step 1: Admin registers and allocates a shift to a new Officer
        long uid = System.currentTimeMillis() % 100000;
        Officer officer = officerService.registerOfficer(
                "pipeline_off_" + uid, "pass123",
                "Inspector Ramesh", "ramesh_" + uid + "@traffic.gov.in", "+91 91111 " + String.format("%05d", uid),
                "TP-" + uid, "Inspector", "Central Station", 1
        );
        assertNotNull(officer);

        DutyAssignment duty = new DutyAssignment(
                0, officer.getOfficerId(), 1, ShiftType.EVENING,
                LocalDate.now(), LocalTime.of(14, 0), LocalTime.of(22, 0),
                DutyStatus.SCHEDULED, "Pipeline verification shift"
        );
        DutyAssignment createdDuty = dutyService.scheduleDuty(duty);
        assertNotNull(createdDuty);
        assertEquals(DutyStatus.SCHEDULED, createdDuty.getStatus());

        // Step 2: Officer checks in
        Attendance att = dutyService.markCheckIn(officer.getOfficerId(), createdDuty.getId(), "Officer terminal login checkin");
        assertNotNull(att);
        assertNotNull(att.getCheckInTime());

        // Verify duty status transitioned to IN_PROGRESS
        Optional<DutyAssignment> inProgressDuty = dutyService.getAllDuties().stream()
                .filter(d -> d.getId() == createdDuty.getId())
                .findFirst();
        assertTrue(inProgressDuty.isPresent());
        assertEquals(DutyStatus.IN_PROGRESS, inProgressDuty.get().getStatus());

        // Step 3: Officer checks out
        boolean checkedOut = dutyService.markCheckOut(att.getId(), createdDuty.getId());
        assertTrue(checkedOut);

        // Verify duty status transitioned to COMPLETED
        Optional<DutyAssignment> completedDuty = dutyService.getAllDuties().stream()
                .filter(d -> d.getId() == createdDuty.getId())
                .findFirst();
        assertTrue(completedDuty.isPresent());
        assertEquals(DutyStatus.COMPLETED, completedDuty.get().getStatus());
    }
}
