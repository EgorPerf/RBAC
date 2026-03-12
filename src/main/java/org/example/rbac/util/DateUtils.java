package org.example.rbac.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private DateUtils() {}

    public static String getCurrentDate() {
        return LocalDate.now().toString();
    }

    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static boolean isBefore(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return date1.compareTo(date2) > 0;
    }

    public static String addDays(String date, int days) {
        if (date == null || date.length() < 10) return date;
        String justDate = date.substring(0, 10);
        try {
            return LocalDate.parse(justDate).plusDays(days).toString();
        } catch (Exception e) {
            return date;
        }
    }

    public static String formatRelativeTime(String date) {
        if (date == null || date.length() < 10) return "";
        String justDate = date.substring(0, 10);
        LocalDate target;
        try {
            target = LocalDate.parse(justDate);
        } catch (Exception e) {
            return "";
        }
        LocalDate now = LocalDate.now();
        long days = ChronoUnit.DAYS.between(now, target);

        if (days == 0) return "today";
        if (days == 1) return "in 1 day";
        if (days > 1) return "in " + days + " days";
        if (days == -1) return "1 day ago";
        return Math.abs(days) + " days ago";
    }
}