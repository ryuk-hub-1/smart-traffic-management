package com.smarttraffic;

import com.smarttraffic.model.*;
import com.smarttraffic.model.enums.*;
import com.smarttraffic.service.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullCrudVerificationTest {

    private static OfficerService officerService;
    private static JunctionService junctionService;
    private static DutyService dutyService;
    private static ChallanService challanService;
    private static ComplaintService complaintService;
    private static EmergencyService emergencyService;

    @BeforeAll
    public static void setUp() {
        officerService = OfficerService.getInstance();
        junctionService = JunctionService.getInstance();
        dutyService = DutyService.getInstance();
        challanService = ChallanService.getInstance();
        complaintService = ComplaintService.getInstance();
        emergencyService = EmergencyService.getInstance();
    }

    @Test
    @Order(1)
    public void testOfficerFullCrud() throws Exception {
        long id = System.currentTimeMillis() % 100000;
        // CREATE
        Officer o = officerService.registerOfficer(
                "off_crud_" + id, "pass123", "Officer Full Name",
                "officer" + id + "@police.gov", "+91 99111 " + String.format("%05d", id),
                "BDG-" + id, "Inspector", "Central Station", 1
        );
        assertNotNull(o);

        // READ
        Optional<Officer> read = officerService.getOfficerById(o.getOfficerId());
        assertTrue(read.isPresent());
        assertEquals("Inspector", read.get().getRank());

        // UPDATE
        read.get().setFullName("Inspector Updated Name");
        read.get().setPoliceStation("North Station");
        read.get().setStatus("ON_LEAVE");
        boolean updated = officerService.updateOfficer(read.get());
        assertTrue(updated);

        Optional<Officer> afterUpdate = officerService.getOfficerById(o.getOfficerId());
        assertTrue(afterUpdate.isPresent());
        assertEquals("Inspector Updated Name", afterUpdate.get().getFullName());
        assertEquals("North Station", afterUpdate.get().getPoliceStation());
        assertEquals("ON_LEAVE", afterUpdate.get().getStatus());

        // DELETE
        boolean deleted = officerService.deleteOfficer(o.getOfficerId());
        assertTrue(deleted);
        assertTrue(officerService.getOfficerById(o.getOfficerId()).isEmpty());
    }

    @Test
    @Order(2)
    public void testJunctionFullCrud() throws Exception {
        long id = System.currentTimeMillis() % 10000;
        // CREATE
        Junction j = new Junction(0, "JNC-" + id, "Test Junction " + id, "North Zone", "Near Landmark", 12.9, 77.5, RiskLevel.HIGH);
        Junction created = junctionService.addJunction(j);
        assertNotNull(created);

        // READ
        Optional<Junction> read = junctionService.getJunctionById(created.getId());
        assertTrue(read.isPresent());
        assertEquals("North Zone", read.get().getArea());

        // UPDATE
        read.get().setName("Updated Junction Name");
        read.get().setRiskLevel(RiskLevel.LOW);
        boolean updated = junctionService.updateJunction(read.get());
        assertTrue(updated);

        Optional<Junction> afterUpdate = junctionService.getJunctionById(created.getId());
        assertTrue(afterUpdate.isPresent());
        assertEquals("Updated Junction Name", afterUpdate.get().getName());
        assertEquals(RiskLevel.LOW, afterUpdate.get().getRiskLevel());

        // DELETE
        boolean deleted = junctionService.deleteJunction(created.getId());
        assertTrue(deleted);
        assertTrue(junctionService.getJunctionById(created.getId()).isEmpty());
    }

    @Test
    @Order(3)
    public void testDutyFullCrud() throws Exception {
        // CREATE
        DutyAssignment duty = new DutyAssignment(
                0, 1, 1, ShiftType.MORNING,
                LocalDate.now().plusDays(2), LocalTime.of(6, 0), LocalTime.of(14, 0),
                DutyStatus.SCHEDULED, "Future beat"
        );
        DutyAssignment created = dutyService.scheduleDuty(duty);
        assertNotNull(created);

        // UPDATE
        boolean updated = dutyService.updateDutyStatus(created.getId(), DutyStatus.CANCELLED);
        assertTrue(updated);

        // DELETE
        boolean deleted = dutyService.deleteDuty(created.getId());
        assertTrue(deleted);
    }

    @Test
    @Order(4)
    public void testChallanFullCrud() throws Exception {
        // CREATE
        Challan challan = new Challan(
                0, null, "KA-05-CRUD-9999", VehicleType.TWO_WHEELER,
                "Test Offender", "+91 98765 00000",
                1, 1, 1,
                LocalDateTime.now(), 500.0,
                ChallanStatus.PENDING, null, null,
                "proof.jpg", "No helmet"
        );
        Challan created = challanService.issueChallan(challan);
        assertNotNull(created);
        assertNotNull(created.getChallanNumber());

        // READ
        Optional<Challan> read = challanService.getChallanById(created.getId());
        assertTrue(read.isPresent());

        // UPDATE
        read.get().setRemarks("Updated remarks: verified with camera footage");
        boolean updated = challanService.updateChallan(read.get());
        assertTrue(updated);

        // DELETE
        boolean deleted = challanService.deleteChallan(created.getId());
        assertTrue(deleted);
        assertTrue(challanService.getChallanById(created.getId()).isEmpty());
    }

    @Test
    @Order(5)
    public void testComplaintFullCrud() throws Exception {
        // CREATE
        Complaint c = new Complaint();
        c.setCitizenId(5);
        c.setComplaintType("Reckless Driving");
        c.setDescription("Overspeeding car in school zone.");
        c.setIncidentDate(LocalDateTime.now());
        Complaint created = complaintService.fileComplaint(c);
        assertNotNull(created);

        // READ
        Optional<Complaint> read = complaintService.getComplaintById(created.getId());
        assertTrue(read.isPresent());

        // UPDATE
        boolean updated = complaintService.updateComplaintStatus(created.getId(), ComplaintStatus.INVESTIGATING, 1, "Action initiated");
        assertTrue(updated);

        // DELETE
        boolean deleted = complaintService.deleteComplaint(created.getId());
        assertTrue(deleted);
        assertTrue(complaintService.getComplaintById(created.getId()).isEmpty());
    }

    @Test
    @Order(6)
    public void testEmergencyReportFullCrud() throws Exception {
        // CREATE
        EmergencyReport rep = new EmergencyReport(
                0, null, 1, 1, "Minor Skid", EmergencySeverity.MINOR,
                1, 0, false, false, false, LocalDateTime.now(),
                EmergencyStatus.REPORTED, "Bike skid"
        );
        EmergencyReport created = emergencyService.reportEmergency(rep);
        assertNotNull(created);

        // UPDATE
        boolean updated = emergencyService.updateStatus(created.getId(), EmergencyStatus.CLEARED, "Bike cleared");
        assertTrue(updated);

        // DELETE
        boolean deleted = emergencyService.deleteEmergency(created.getId());
        assertTrue(deleted);
        assertTrue(emergencyService.getEmergencyById(created.getId()).isEmpty());
    }

    @Test
    @Order(7)
    public void testAttendanceFullCrud() throws Exception {
        // CREATE
        Attendance att = new Attendance(
                0, 1, null, LocalDate.now().minusDays(5),
                LocalTime.of(8, 0), LocalTime.of(16, 0),
                AttendanceStatus.PRESENT, "Manual log"
        );
        dutyService.recordAttendance(att);

        // DELETE
        boolean deleted = dutyService.deleteAttendance(att.getId());
        assertTrue(deleted);
    }
}
