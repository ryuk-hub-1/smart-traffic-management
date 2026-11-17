package com.smarttraffic.service;

import com.smarttraffic.dao.*;
import com.smarttraffic.exception.DatabaseException;
import com.smarttraffic.model.*;
import com.smarttraffic.model.enums.ChallanStatus;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService demonstrates Polymorphism, Interfaces, Collections Framework,
 * and Java Streams for analytics and report generation.
 */
public class ReportService {
    private static ReportService instance;
    private final ChallanDao challanDao = new ChallanDao();
    private final OfficerDao officerDao = new OfficerDao();
    private final JunctionDao junctionDao = new JunctionDao();
    private final AttendanceDao attendanceDao = new AttendanceDao();
    private final EmergencyDao emergencyDao = new EmergencyDao();

    private ReportService() {
    }

    public static synchronized ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }

    /**
     * Interface for polymorphic report generation.
     */
    public interface ReportGenerator {
        String getTitle();
        String generateTextSummary();
        Map<String, Object> getStatistics();
    }

    /**
     * Polymorphic implementation: Violation Summary Report.
     */
    public class ViolationSummaryReport implements ReportGenerator {
        private final List<Challan> challans;

        public ViolationSummaryReport(List<Challan> challans) {
            this.challans = challans;
        }

        @Override
        public String getTitle() {
            return "Traffic Violation Breakdown & Offense Analysis";
        }

        @Override
        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("Total Violations Recorded", challans.size());

            // Java Streams - Grouping by category
            Map<String, Long> categoryCount = challans.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getViolationCategory() != null ? c.getViolationCategory() : "Other",
                            Collectors.counting()
                    ));
            stats.put("Category Breakdown", categoryCount);

            double totalFine = challans.stream()
                    .mapToDouble(Challan::getFineAmount)
                    .sum();
            stats.put("Total Penalties Issued", String.format("₹ %,.2f", totalFine));

            return stats;
        }

        @Override
        public String generateTextSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("========================================================\n");
            sb.append("         TRAFFIC VIOLATION AUDIT & ANALYSIS REPORT      \n");
            sb.append("========================================================\n\n");
            sb.append("Total Citations Processed: ").append(challans.size()).append("\n\n");

            sb.append("OFFENSE BREAKDOWN BY CATEGORY:\n");
            Map<String, Long> byCat = challans.stream()
                    .collect(Collectors.groupingBy(c -> c.getViolationCategory() != null ? c.getViolationCategory() : "Other", Collectors.counting()));
            byCat.forEach((cat, cnt) -> sb.append(String.format(" - %-30s : %d offenses\n", cat, cnt)));

            sb.append("\nTOTAL LEVIED PENALTY VALUE: ");
            double totalFine = challans.stream().mapToDouble(Challan::getFineAmount).sum();
            sb.append(String.format("₹ %,.2f\n", totalFine));
            sb.append("========================================================\n");
            return sb.toString();
        }
    }

    /**
     * Polymorphic implementation: Revenue & Fine Collections Report.
     */
    public class RevenueCollectionReport implements ReportGenerator {
        private final List<Challan> challans;

        public RevenueCollectionReport(List<Challan> challans) {
            this.challans = challans;
        }

        @Override
        public String getTitle() {
            return "Fine Revenue Collection & Recovery Report";
        }

        @Override
        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new LinkedHashMap<>();
            double collected = challans.stream()
                    .filter(c -> c.getPaymentStatus() == ChallanStatus.PAID)
                    .mapToDouble(Challan::getFineAmount)
                    .sum();

            double pending = challans.stream()
                    .filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING || c.getPaymentStatus() == ChallanStatus.OVERDUE)
                    .mapToDouble(Challan::getFineAmount)
                    .sum();

            long paidCount = challans.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PAID).count();
            long pendingCount = challans.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.PENDING).count();
            long overdueCount = challans.stream().filter(c -> c.getPaymentStatus() == ChallanStatus.OVERDUE).count();

            stats.put("Total Collected Revenue", String.format("₹ %,.2f", collected));
            stats.put("Total Outstanding Dues", String.format("₹ %,.2f", pending));
            stats.put("Paid Challans Count", paidCount);
            stats.put("Pending Challans Count", pendingCount);
            stats.put("Overdue Challans Count", overdueCount);
            return stats;
        }

        @Override
        public String generateTextSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("========================================================\n");
            sb.append("       REVENUE COLLECTION & FINE RECOVERY REPORT        \n");
            sb.append("========================================================\n\n");

            Map<String, Object> stats = getStatistics();
            stats.forEach((k, v) -> sb.append(String.format(" %-28s : %s\n", k, v)));

            sb.append("========================================================\n");
            return sb.toString();
        }
    }

    /**
     * Polymorphic implementation: Emergency & Accident Summary Report.
     */
    public class AccidentSummaryReport implements ReportGenerator {
        private final List<EmergencyReport> reports;

        public AccidentSummaryReport(List<EmergencyReport> reports) {
            this.reports = reports;
        }

        @Override
        public String getTitle() {
            return "Accident Severity & Junction Hazard Report";
        }

        @Override
        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("Total Reported Incidents", reports.size());
            int totalCasualties = reports.stream().mapToInt(EmergencyReport::getCasualtiesCount).sum();
            int totalVehicles = reports.stream().mapToInt(EmergencyReport::getVehiclesInvolved).sum();
            long ambulances = reports.stream().filter(EmergencyReport::isAmbulanceRequired).count();

            stats.put("Total Casualties", totalCasualties);
            stats.put("Vehicles Impacted", totalVehicles);
            stats.put("Ambulance Dispatches", ambulances);
            return stats;
        }

        @Override
        public String generateTextSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("========================================================\n");
            sb.append("       ACCIDENT SEVERITY & EMERGENCY AUDIT REPORT       \n");
            sb.append("========================================================\n\n");
            sb.append("Total Incidents Recorded: ").append(reports.size()).append("\n\n");

            sb.append("INCIDENTS BY SEVERITY:\n");
            Map<String, Long> bySev = reports.stream()
                    .collect(Collectors.groupingBy(r -> r.getSeverity().name(), Collectors.counting()));
            bySev.forEach((sev, cnt) -> sb.append(String.format(" - %-20s : %d incidents\n", sev, cnt)));

            sb.append("========================================================\n");
            return sb.toString();
        }
    }

    public ReportGenerator getViolationReport() throws DatabaseException {
        return new ViolationSummaryReport(challanDao.findAll());
    }

    public ReportGenerator getRevenueReport() throws DatabaseException {
        return new RevenueCollectionReport(challanDao.findAll());
    }

    public ReportGenerator getAccidentReport() throws DatabaseException {
        return new AccidentSummaryReport(emergencyDao.findAll());
    }
}
