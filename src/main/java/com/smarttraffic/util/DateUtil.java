package com.smarttraffic.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Date and Time formatting utility.
 */
public class DateUtil {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "N/A";
        return dt.format(DATE_TIME_FORMATTER);
    }

    public static String formatDate(LocalDate d) {
        if (d == null) return "N/A";
        return d.format(DATE_FORMATTER);
    }

    public static String formatTime(LocalTime t) {
        if (t == null) return "N/A";
        return t.format(TIME_FORMATTER);
    }

    public static LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(text);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
