package com.example.coen390.fridgefriends.model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateParser {

    public static String extractFormattedDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        // Clean common OCR character confusions
        String upper = text.toUpperCase()
                .replaceAll("(?<=\\d)[O|o]", "0")
                .replaceAll("[O|o](?=\\d)", "0");

        // 1. YY [MonthCode] DD or YYYY [MonthCode] DD (e.g. "27 JN 06", "27JN06", "2027 MA 10", "27-MA-10")
        Pattern pYYMonthDD = Pattern.compile("(\\d{4}|\\d{2})[\\/\\-\\.\\s]*(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|JA|FE|MR|AL|AP|MA|MY|JN|JL|AU|SE|OC|NO|DE|MAI|AVR|AOU)[\\/\\-\\.\\s]*(\\d{1,2})", Pattern.CASE_INSENSITIVE);
        Matcher mYYMonthDD = pYYMonthDD.matcher(upper);
        if (mYYMonthDD.find()) {
            String yStr = mYYMonthDD.group(1);
            String mStr = mYYMonthDD.group(2);
            String dStr = mYYMonthDD.group(3);
            int year = parseYear(yStr);
            int month = parseMonthCode(mStr);
            int day = (dStr != null) ? Integer.parseInt(dStr) : 10;
            return formatDate(day, month, year);
        }

        // 2. [MonthCode] DD YYYY or DD [MonthCode] YYYY (e.g. "MA 10 2027" or "10 MA 2027" or "NOV 23 2026")
        Pattern pMonthName = Pattern.compile("(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC|JA|FE|MR|AL|AP|MA|MY|JN|JL|AU|SE|OC|NO|DE)[a-z]*[\\/\\-\\.\\s]*(\\d{1,2})?,?[\\/\\-\\.\\s]*(\\d{4}|\\d{2})", Pattern.CASE_INSENSITIVE);
        Matcher mMonthName = pMonthName.matcher(upper);
        if (mMonthName.find()) {
            String mStr = mMonthName.group(1);
            String dStr = mMonthName.group(2);
            String yStr = mMonthName.group(3);
            int year = parseYear(yStr);
            int day = (dStr != null) ? Integer.parseInt(dStr) : 28;
            int month = parseMonthCode(mStr);
            return formatDate(day, month, year);
        }

        // 3. MM/DD/YYYY or MM-DD-YYYY or MM.DD.YYYY (e.g. "BB 11/23/2026" or "11/23/26")
        Pattern pMMDDYYYY = Pattern.compile("(\\b0?[1-9]|1[0-2])[\\/\\-\\.](\\b0?[1-9]|[12][0-9]|3[01])[\\/\\-\\.](\\d{4}|\\d{2})");
        Matcher mMMDDYYYY = pMMDDYYYY.matcher(upper);
        if (mMMDDYYYY.find()) {
            int month = Integer.parseInt(mMMDDYYYY.group(1));
            int day = Integer.parseInt(mMMDDYYYY.group(2));
            int year = parseYear(mMMDDYYYY.group(3));
            if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                return formatDate(day, month, year);
            }
        }

        // 4. YYYY/MM/DD or YYYY-MM-DD (e.g. "2026/11/23" or "2026-11-23")
        Pattern pYYYYMMDD = Pattern.compile("(\\d{4})[\\/\\-\\.](\\b0?[1-9]|1[0-2])[\\/\\-\\.](\\d{1,2})");
        Matcher mYYYYMMDD = pYYYYMMDD.matcher(upper);
        if (mYYYYMMDD.find()) {
            int year = Integer.parseInt(mYYYYMMDD.group(1));
            int month = Integer.parseInt(mYYYYMMDD.group(2));
            int day = Integer.parseInt(mYYYYMMDD.group(3));
            return formatDate(day, month, year);
        }

        // 5. MM/YYYY or MM-YYYY (e.g. "04/2027" or "04-2027")
        Pattern pMMYYYY = Pattern.compile("(\\b0?[1-9]|1[0-2])[\\/\\-\\.](\\d{4})");
        Matcher mMMYYYY = pMMYYYY.matcher(upper);
        if (mMMYYYY.find()) {
            int month = Integer.parseInt(mMMYYYY.group(1));
            int year = Integer.parseInt(mMMYYYY.group(2));
            return formatDate(30, month, year);
        }

        // 6. MM/YY or MM-YY (e.g. "04/27" or "11/26")
        Pattern pMMYY = Pattern.compile("(\\b0?[1-9]|1[0-2])[\\/\\-](\\d{2})");
        Matcher mMMYY = pMMYY.matcher(upper);
        if (mMMYY.find()) {
            int month = Integer.parseInt(mMMYY.group(1));
            int year = parseYear(mMMYY.group(2));
            return formatDate(28, month, year);
        }

        return null;
    }

    private static int parseYear(String yStr) {
        if (yStr == null) return 2026;
        if (yStr.length() == 2) {
            return 2000 + Integer.parseInt(yStr);
        }
        return Integer.parseInt(yStr);
    }

    private static int parseMonthCode(String mStr) {
        if (mStr == null) return 5;
        String m = mStr.toUpperCase().trim();
        if (m.startsWith("JA")) return 1;
        if (m.startsWith("FE")) return 2;
        if (m.startsWith("MR") || m.startsWith("MAR")) return 3;
        if (m.startsWith("AL") || m.startsWith("AP") || m.startsWith("AV")) return 4;
        if (m.startsWith("MA") || m.startsWith("MY")) return 5;
        if (m.startsWith("JN") || m.startsWith("JUN")) return 6;
        if (m.startsWith("JL") || m.startsWith("JUL")) return 7;
        if (m.startsWith("AU") || m.startsWith("AUG") || m.startsWith("OU")) return 8;
        if (m.startsWith("SE") || m.startsWith("SEP")) return 9;
        if (m.startsWith("OC") || m.startsWith("OCT")) return 10;
        if (m.startsWith("NO") || m.startsWith("NOV")) return 11;
        if (m.startsWith("DE") || m.startsWith("DEC")) return 12;
        return 5;
    }

    private static String formatDate(int day, int month, int year) {
        return String.format(Locale.US, "%02d/%02d/%d", day, month, year);
    }

    public static String formatDisplayDate(String dateStr) {
        if (dateStr == null) return "Jun 6, 2027";
        try {
            String[] parts = dateStr.split("/");
            if (parts.length == 3) {
                int d = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                String mName = (m >= 1 && m <= 12) ? months[m - 1] : "Jun";
                return String.format(Locale.US, "%s %d, %d", mName, d, y);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    public static int getDaysUntilExpiry(String expiryStr) {
        if (expiryStr == null || expiryStr.trim().isEmpty() || expiryStr.equalsIgnoreCase("no expiration")) {
            return 9999;
        }

        try {
            java.util.Calendar today = java.util.Calendar.getInstance();
            today.set(java.util.Calendar.HOUR_OF_DAY, 0);
            today.set(java.util.Calendar.MINUTE, 0);
            today.set(java.util.Calendar.SECOND, 0);
            today.set(java.util.Calendar.MILLISECOND, 0);

            java.util.Calendar expiryCal = java.util.Calendar.getInstance();
            expiryCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            expiryCal.set(java.util.Calendar.MINUTE, 0);
            expiryCal.set(java.util.Calendar.SECOND, 0);
            expiryCal.set(java.util.Calendar.MILLISECOND, 0);

            String[] parts = expiryStr.split("/");
            if (parts.length == 3) {
                int p1 = Integer.parseInt(parts[0].trim());
                int p2 = Integer.parseInt(parts[1].trim());
                int year = Integer.parseInt(parts[2].trim());
                if (year < 100) year += 2000;

                int day, month;
                if (p1 > 12) {
                    day = p1;
                    month = p2;
                } else if (p2 > 12) {
                    month = p1;
                    day = p2;
                } else {
                    day = p1;
                    month = p2;
                }

                expiryCal.set(year, month - 1, day);
                long diffMs = expiryCal.getTimeInMillis() - today.getTimeInMillis();
                return (int) (diffMs / (24 * 60 * 60 * 1000L));
            }
        } catch (Exception ignored) {}

        return 9999;
    }
}
