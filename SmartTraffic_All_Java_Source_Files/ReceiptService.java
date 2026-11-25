package com.smarttraffic.service;

import com.smarttraffic.model.Challan;
import com.smarttraffic.util.DateUtil;

/**
 * Service to generate formatted printable official traffic challan receipts.
 */
public class ReceiptService {
    private static ReceiptService instance;

    private ReceiptService() {
    }

    public static synchronized ReceiptService getInstance() {
        if (instance == null) {
            instance = new ReceiptService();
        }
        return instance;
    }

    public String generateChallanReceiptText(Challan c) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(62);
        String subline = "-".repeat(62);

        sb.append(line).append("\n");
        sb.append(center("GOVERNMENT TRAFFIC POLICE DEPARTMENT", 62)).append("\n");
        sb.append(center("ELECTRONIC TRAFFIC VIOLATION NOTICE / CHALLAN", 62)).append("\n");
        sb.append(center("Issued under The Motor Vehicles Enforcement Framework", 62)).append("\n");
        sb.append(line).append("\n\n");

        sb.append(String.format(" CHALLAN NO : %-25s  STATUS: [%s]\n", c.getChallanNumber(), c.getPaymentStatus().getLabel()));
        sb.append(String.format(" ISSUE DATE : %-25s\n", DateUtil.formatDateTime(c.getViolationDate())));
        sb.append(subline).append("\n");

        sb.append(" VEHICLE & OFFENDER DETAILS:\n");
        sb.append(String.format("   Vehicle Reg. No : %-20s\n", c.getVehicleNumber()));
        sb.append(String.format("   Vehicle Category: %-20s\n", c.getVehicleType() != null ? c.getVehicleType().getLabel() : "N/A"));
        sb.append(String.format("   Violator / Owner: %-20s\n", c.getViolatorName() != null && !c.getViolatorName().isEmpty() ? c.getViolatorName() : "Registered Owner"));
        sb.append(String.format("   Contact Phone   : %-20s\n", c.getViolatorPhone() != null && !c.getViolatorPhone().isEmpty() ? c.getViolatorPhone() : "Not Disclosed"));
        sb.append(subline).append("\n");

        sb.append(" LOCATION & ISSUING ENFORCEMENT OFFICER:\n");
        sb.append(String.format("   Junction Point  : %-20s\n", c.getJunctionName() != null ? c.getJunctionName() : "Point of Enforcement"));
        sb.append(String.format("   Officer In-Charge: %-20s\n", c.getOfficerName() != null ? c.getOfficerName() : "Patrol Squad"));
        sb.append(String.format("   Badge ID Number : %-20s\n", c.getOfficerBadge() != null ? c.getOfficerBadge() : "N/A"));
        sb.append(subline).append("\n");

        sb.append(" OFFENSE & PENALTY SUMMARY:\n");
        sb.append(String.format("   Violation Code  : %-20s\n", c.getViolationCategory() != null ? c.getViolationCategory() : "General"));
        sb.append(String.format("   Specific Offense: %-20s\n", c.getViolationTitle() != null ? c.getViolationTitle() : "Traffic Infraction"));
        if (c.getRemarks() != null && !c.getRemarks().isEmpty()) {
            sb.append(String.format("   Officer Remarks : %-20s\n", c.getRemarks()));
        }
        sb.append(subline).append("\n");

        sb.append(String.format(" TOTAL FINE AMOUNT PAYABLE:  %s\n", c.getFormattedFine()));
        sb.append(subline).append("\n");

        if (c.getPaymentStatus() == com.smarttraffic.model.enums.ChallanStatus.PAID) {
            sb.append(" PAYMENT VERIFICATION:\n");
            sb.append(String.format("   Transaction Ref : %-20s\n", c.getPaymentReference() != null ? c.getPaymentReference() : "VERIFIED_ONLINE"));
            sb.append(String.format("   Paid Timestamp  : %-20s\n", DateUtil.formatDateTime(c.getPaymentDate())));
            sb.append("   Result          : FULLY SETTLED - NO DUES OUTSTANDING\n");
        } else {
            sb.append(" PAYMENT INSTRUCTIONS:\n");
            sb.append("   Please settle this fine within 30 days via online Citizen\n");
            sb.append("   Portal or visit your nearest Traffic Enforcement Booth.\n");
            sb.append("   Failure to settle may lead to registration hold/court summons.\n");
        }

        sb.append(subline).append("\n");
        sb.append(center("[ DIGITAL AUDIT HASH: " + Integer.toHexString(c.hashCode()).toUpperCase() + " | VERIFIED ELECTRONIC DOCUMENT ]", 62)).append("\n");
        sb.append(line).append("\n");

        return sb.toString();
    }

    private String center(String text, int width) {
        if (text.length() >= width) return text;
        int leftPadding = (width - text.length()) / 2;
        return " ".repeat(leftPadding) + text;
    }
}
