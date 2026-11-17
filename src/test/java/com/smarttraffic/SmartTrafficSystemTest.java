package com.smarttraffic;

import com.smarttraffic.dao.DatabaseManager;
import com.smarttraffic.exception.AuthenticationException;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.exception.ValidationException;
import com.smarttraffic.model.*;
import com.smarttraffic.model.enums.ChallanStatus;
import com.smarttraffic.model.enums.ComplaintStatus;
import com.smarttraffic.model.enums.Role;
import com.smarttraffic.model.enums.VehicleType;
import com.smarttraffic.service.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SmartTrafficSystemTest {

    private static DatabaseManager dbManager;
    private static AuthService authService;
    private static OfficerService officerService;
    private static JunctionService junctionService;
    private static ChallanService challanService;
    private static ComplaintService complaintService;
    private static EmergencyService emergencyService;
    private static ReportService reportService;

    @BeforeAll
    public static void setUp() {
        dbManager = DatabaseManager.getInstance();
        authService = AuthService.getInstance();
        officerService = OfficerService.getInstance();
        junctionService = JunctionService.getInstance();
        challanService = ChallanService.getInstance();
        complaintService = ComplaintService.getInstance();
        emergencyService = EmergencyService.getInstance();
        reportService = ReportService.getInstance();
    }

    @Test
    @Order(1)
    public void testDatabaseConnection() throws Exception {
        try (Connection conn = dbManager.getConnection()) {
            assertNotNull(conn, "Database connection should not be null");
            assertFalse(conn.isClosed(), "Database connection should be open");
        }
    }

    @Test
    @Order(2)
    public void testAdminAuthentication() throws Exception {
        User admin = authService.login("admin", "admin123");
        assertNotNull(admin, "Admin user should exist");
        assertEquals(Role.ADMIN, admin.getRole(), "Role should be ADMIN");
        assertTrue(admin.hasAdministrativePrivileges(), "Admin should have administrative privileges");
    }

    @Test
    @Order(3)
    public void testOfficerAuthentication() throws Exception {
        User officer = authService.login("officer1", "officer123");
        assertNotNull(officer, "Officer should exist");
        assertEquals(Role.OFFICER, officer.getRole(), "Role should be OFFICER");
        assertTrue(officer instanceof Officer, "Officer should be an instance of Officer model");
        Officer o = (Officer) officer;
        assertEquals("TP-101", o.getBadgeNumber(), "Badge number should be TP-101");
    }

    @Test
    @Order(4)
    public void testCitizenRegistrationAndLogin() throws Exception {
        String testUser = "testcitizen_" + System.currentTimeMillis();
        Citizen registered = authService.registerCitizen(
                testUser, "pass123", "pass123",
                "Karan Malhotra", "karan@example.com", "+91 99999 88888",
                "DL-998877", "KA-03-TR-9999", "Indiranagar, Bangalore"
        );
        assertNotNull(registered, "Registered citizen should not be null");

        User loggedIn = authService.login(testUser, "pass123");
        assertNotNull(loggedIn, "Newly registered citizen should be able to log in");
        assertEquals(Role.CITIZEN, loggedIn.getRole());
    }

    @Test
    @Order(5)
    public void testJunctionsAndOfficerAllocation() throws Exception {
        List<Junction> junctions = junctionService.getAllJunctions();
        assertFalse(junctions.isEmpty(), "Junctions should be pre-seeded in the database");

        List<Officer> officers = officerService.getAllOfficers();
        assertFalse(officers.isEmpty(), "Officers should be pre-seeded in the database");

        Officer firstOfficer = officers.get(0);
        Junction firstJunction = junctions.get(0);

        boolean assigned = officerService.assignJunction(firstOfficer.getOfficerId(), firstJunction.getId());
        assertTrue(assigned, "Assigning junction to officer should succeed");
    }

    @Test
    @Order(6)
    public void testChallanIssuanceAndFineSettlement() throws Exception {
        List<ViolationType> violationTypes = challanService.getAllViolationTypes();
        assertFalse(violationTypes.isEmpty(), "Violation types catalog should not be empty");

        ViolationType violation = violationTypes.get(0);
        List<Junction> junctions = junctionService.getAllJunctions();
        List<Officer> officers = officerService.getAllOfficers();

        Challan newChallan = new Challan(
                0, null, "KA-01-TEST-1234", VehicleType.FOUR_WHEELER_CAR,
                "Rohan Mehra", "+91 91234 56789",
                officers.get(0).getOfficerId(),
                junctions.get(0).getId(),
                violation.getId(),
                LocalDateTime.now(),
                violation.getStandardFineAmount(),
                ChallanStatus.PENDING,
                null, null,
                "evidence_test.jpg",
                "Automated unit test violation entry"
        );

        Challan issued = challanService.issueChallan(newChallan);
        assertNotNull(issued, "Issued challan should not be null");
        assertNotNull(issued.getChallanNumber(), "Challan number should be auto-generated");
        assertEquals(ChallanStatus.PENDING, issued.getPaymentStatus());

        // Test Receipt Generation
        String receipt = ReceiptService.getInstance().generateChallanReceiptText(issued);
        assertNotNull(receipt, "Receipt text should not be null");
        assertTrue(receipt.contains(issued.getChallanNumber()), "Receipt should contain challan number");

        // Test Fine Settlement
        String paymentRef = "UNIT-TEST-TXN-101";
        boolean paid = challanService.payChallan(issued.getId(), paymentRef);
        assertTrue(paid, "Challan payment should be recorded");

        Optional<Challan> updatedChallan = challanService.getChallanById(issued.getId());
        assertTrue(updatedChallan.isPresent());
        assertEquals(ChallanStatus.PAID, updatedChallan.get().getPaymentStatus());
        assertEquals(paymentRef, updatedChallan.get().getPaymentReference());
    }

    @Test
    @Order(7)
    public void testComplaintFilingAndResolution() throws Exception {
        List<Junction> junctions = junctionService.getAllJunctions();
        List<Officer> officers = officerService.getAllOfficers();

        Complaint complaint = new Complaint(
                0, null, 5, junctions.get(0).getId(),
                "Broken Traffic Signal", "N/A",
                "Unit test: North-bound green signal light not glowing.",
                LocalDateTime.now(), ComplaintStatus.PENDING,
                null, null, null, null, null
        );

        Complaint filed = complaintService.fileComplaint(complaint);
        assertNotNull(filed);
        assertNotNull(filed.getComplaintNumber());
        assertEquals(ComplaintStatus.PENDING, filed.getStatus());

        // Resolve complaint
        boolean resolved = complaintService.updateComplaintStatus(
                filed.getId(),
                ComplaintStatus.RESOLVED,
                officers.get(0).getOfficerId(),
                "Maintenance crew replaced the LED module."
        );
        assertTrue(resolved, "Complaint should be updated to RESOLVED");

        Optional<Complaint> updated = complaintService.getComplaintById(filed.getId());
        assertTrue(updated.isPresent());
        assertEquals(ComplaintStatus.RESOLVED, updated.get().getStatus());
    }

    @Test
    @Order(8)
    public void testPolymorphicReportGeneration() throws Exception {
        ReportService.ReportGenerator violationReport = reportService.getViolationReport();
        assertNotNull(violationReport);
        assertNotNull(violationReport.generateTextSummary());
        assertFalse(violationReport.getStatistics().isEmpty());

        ReportService.ReportGenerator revenueReport = reportService.getRevenueReport();
        assertNotNull(revenueReport);
        assertNotNull(revenueReport.generateTextSummary());

        ReportService.ReportGenerator accidentReport = reportService.getAccidentReport();
        assertNotNull(accidentReport);
        assertNotNull(accidentReport.generateTextSummary());
    }

    @Test
    @Order(9)
    public void testOfficerAttendanceAndDutyProgression() throws Exception {
        DutyService dutyService = DutyService.getInstance();
        long ts = System.currentTimeMillis() % 100000;
        Officer testOfficer = officerService.registerOfficer(
                "duty_officer_" + ts, "pass123",
                "Sub-Inspector Vikram", "vikram_" + ts + "@traffic.gov.in", "+91 98888 " + String.format("%05d", ts),
                "TP-" + ts, "Sub-Inspector", "MG Road Traffic Station", 1
        );
        assertNotNull(testOfficer);

        // Mark check-in
        Attendance att = dutyService.markCheckIn(testOfficer.getOfficerId(), null, "Automated Test Check-in");
        assertNotNull(att, "Attendance record should be created on check-in");
        assertEquals(testOfficer.getOfficerId(), att.getOfficerId());
        assertNotNull(att.getCheckInTime());

        // Check active attendance
        Optional<Attendance> todayAtt = dutyService.getTodayAttendanceForOfficer(testOfficer.getOfficerId());
        assertTrue(todayAtt.isPresent());

        // Mark check-out
        boolean checkedOut = dutyService.markCheckOut(att.getId(), att.getDutyId());
        assertTrue(checkedOut, "Check-out should succeed");
    }

    @Test
    @Order(10)
    public void testOfficerCrudCompleteLifecycle() throws Exception {
        // 1. Create a dedicated test officer
        long ts = System.currentTimeMillis() % 100000;
        Officer created = officerService.registerOfficer(
                "crud_officer_" + ts, "pass123",
                "Sub-Inspector Mahesh", "mahesh_" + ts + "@traffic.gov.in", "+91 95555 " + String.format("%05d", ts),
                "CR-" + ts, "Sub-Inspector", "Indiranagar Station", 1
        );
        assertNotNull(created);
        assertEquals("Sub-Inspector Mahesh", created.getFullName());

        // 2. Read officer
        Optional<Officer> fetched = officerService.getOfficerById(created.getOfficerId());
        assertTrue(fetched.isPresent());
        assertEquals("Indiranagar Station", fetched.get().getPoliceStation());

        // 3. Update officer
        fetched.get().setFullName("Inspector Mahesh Kumar");
        fetched.get().setRank("Inspector");
        fetched.get().setPoliceStation("Koramangala Station");
        boolean updated = officerService.updateOfficer(fetched.get());
        assertTrue(updated);

        Optional<Officer> updatedOfficer = officerService.getOfficerById(created.getOfficerId());
        assertTrue(updatedOfficer.isPresent());
        assertEquals("Inspector Mahesh Kumar", updatedOfficer.get().getFullName());
        assertEquals("Inspector", updatedOfficer.get().getRank());
        assertEquals("Koramangala Station", updatedOfficer.get().getPoliceStation());

        // 4. Delete officer (complete deletion from database)
        boolean deleted = officerService.deleteOfficer(created.getOfficerId());
        assertTrue(deleted, "Delete should succeed and remove officer permanently");

        Optional<Officer> afterDelete = officerService.getOfficerById(created.getOfficerId());
        assertTrue(afterDelete.isEmpty(), "Officer must no longer exist in database after deletion");
    }

    @Test
    @Order(11)
    public void testJunctionCrudCompleteLifecycle() throws Exception {
        long ts = System.currentTimeMillis() % 10000;
        Junction newJunction = new Junction(
                0, "JN-" + ts, "Electronic City Phase " + (ts % 10),
                "Tech Corridors", "Near Toll Plaza", 12.8452, 77.6602, com.smarttraffic.model.enums.RiskLevel.HIGH
        );
        Junction created = junctionService.createJunction(newJunction);
        assertNotNull(created);
        assertTrue(created.getId() > 0);

        // Update
        created.setName("Electronic City Flyover Junction");
        created.setRiskLevel(com.smarttraffic.model.enums.RiskLevel.LOW);
        boolean updated = junctionService.updateJunction(created);
        assertTrue(updated);

        Optional<Junction> fetched = junctionService.getJunctionById(created.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Electronic City Flyover Junction", fetched.get().getName());
        assertEquals(com.smarttraffic.model.enums.RiskLevel.LOW, fetched.get().getRiskLevel());

        // Delete
        boolean deleted = junctionService.deleteJunction(created.getId());
        assertTrue(deleted);
        assertTrue(junctionService.getJunctionById(created.getId()).isEmpty());
    }
}
