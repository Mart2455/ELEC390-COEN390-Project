package com.example.coen390.fridgefriends.model;

import java.util.Calendar;
import java.util.Locale;

public class DateParser {

    public static String extractFormattedDate(String text) {
        String parsed = ExpiryDateParser.parseExpiryDate(text);
        if (parsed != null) {
            return parsed;
        }
        return null;
    }

    public static String formatDisplayDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return "No expiration";
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        try {
            // Check yyyy-MM-dd format
            if (dateStr.contains("-")) {
                String[] parts = dateStr.trim().split("-");
                if (parts.length == 3) {
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int d = Integer.parseInt(parts[2]);
                    String mName = (m >= 1 && m <= 12) ? months[m - 1] : "Jun";
                    return String.format(Locale.US, "%s %d, %d", mName, d, y);
                }
            }
            // Check dd/MM/yyyy or MM/dd/yyyy format
            if (dateStr.contains("/")) {
                String[] parts = dateStr.trim().split("/");
                if (parts.length == 3) {
                    int p1 = Integer.parseInt(parts[0]);
                    int p2 = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int d = (p1 > 12) ? p1 : p2;
                    int m = (p1 > 12) ? p2 : p1;
                    String mName = (m >= 1 && m <= 12) ? months[m - 1] : "Jun";
                    return String.format(Locale.US, "%s %d, %d", mName, d, y);
                }
            }
        } catch (Exception ignored) {}
        return dateStr;
    }

    public static int getDaysUntilExpiry(String expiryStr) {
        if (expiryStr == null || expiryStr.trim().isEmpty() || expiryStr.equalsIgnoreCase("no expiration")) {
            return 9999;
        }

        try {
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar expiryCal = Calendar.getInstance();
            expiryCal.set(Calendar.HOUR_OF_DAY, 0);
            expiryCal.set(Calendar.MINUTE, 0);
            expiryCal.set(Calendar.SECOND, 0);
            expiryCal.set(Calendar.MILLISECOND, 0);

            int year = -1, month = -1, day = -1;

            if (expiryStr.contains("-")) {
                String[] parts = expiryStr.trim().split("-");
                if (parts.length == 3) {
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]);
                    day = Integer.parseInt(parts[2]);
                }
            } else if (expiryStr.contains("/")) {
                String[] parts = expiryStr.trim().split("/");
                if (parts.length == 3) {
                    int p1 = Integer.parseInt(parts[0]);
                    int p2 = Integer.parseInt(parts[1]);
                    year = Integer.parseInt(parts[2]);
                    if (year < 100) year += 2000;
                    if (p1 > 12) {
                        day = p1;
                        month = p2;
                    } else if (p2 > 12) {
                        month = p1;
                        day = p2;
                    } else {
                        month = p1;
                        day = p2;
                    }
                }
            }

            if (year != -1 && month != -1 && day != -1) {
                expiryCal.set(year, month - 1, day);
                long diffMs = expiryCal.getTimeInMillis() - today.getTimeInMillis();
                return (int) (diffMs / (24 * 60 * 60 * 1000L));
            }
        } catch (Exception ignored) {}

        return 9999;
    }
}
