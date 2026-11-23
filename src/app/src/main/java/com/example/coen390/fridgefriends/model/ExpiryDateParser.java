package com.example.coen390.fridgefriends.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for normalizing raw dot-matrix OCR text from food packaging
 * and parsing expiry dates into standard yyyy-MM-dd format.
 */
public class ExpiryDateParser {

    private static final int MIN_VALID_YEAR = 2024;
    private static final int MAX_YEARS_IN_FUTURE = 10;

    // Keywords prioritizing lines with high probability of containing expiration dates
    private static final String[] PRIORITY_KEYWORDS = {
            "BEST BEFORE", "MEILLEUR AVANT", "BEST BY", "USE BY", "EXP", "EXPIRATION",
            "BB", "B.B.", "MA", "AL", "PFR", "PFD", "PROD", "MFG"
    };

    /**
     * Parses raw OCR text and extracts a validated date in yyyy-MM-dd format.
     *
     * @param rawText Full text scanned by ML Kit Text Recognition
     * @return Formatted yyyy-MM-dd date string, or null if no valid date found
     */
    public static String parseExpiryDate(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return null;
        }

        String[] rawLines = rawText.split("\\r?\\n");
        List<String> priorityLines = new ArrayList<>();
        List<String> otherLines = new ArrayList<>();

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            boolean isPriority = false;
            String upper = trimmed.toUpperCase(Locale.US);
            for (String kw : PRIORITY_KEYWORDS) {
                if (upper.contains(kw)) {
                    isPriority = true;
                    break;
                }
            }
            if (isPriority) {
                priorityLines.add(trimmed);
            } else {
                otherLines.add(trimmed);
            }
        }

        // Search priority keyword lines first
        for (String line : priorityLines) {
            String result = processAndMatchLine(line);
            if (result != null) return result;
        }

        // Fallback to remaining lines
        for (String line : otherLines) {
            String result = processAndMatchLine(line);
            if (result != null) return result;
        }

        return null;
    }

    private static String processAndMatchLine(String line) {
        if (line == null || line.trim().isEmpty()) return null;

        // 1. Collapse stray spaces inside numbers/dates (e.g., "20 26" -> "2026", "2026 / 05 / 18" -> "2026/05/18")
        String cleaned = line.replaceAll("(?<=\\d)\\s+(?=\\d)", "")
                .replaceAll("(?<=\\d)\\s*[/\\-.]\\s*(?=\\d)", "/")
                .replaceAll("(?<=\\b\\d{1,2})\\s+(?=[A-Za-z]{2,4}\\b)", " ")
                .replaceAll("(?<=\\b[A-Za-z]{2,4})\\s+(?=\\d{1,2}\\b)", " ");

        // 2. Perform dot-matrix OCR normalization on numeric date candidates
        String normalized = normalizeDotMatrixCharacters(cleaned);

        // 3. Attempt matching date formats
        return matchDatePatterns(normalized);
    }

    /**
     * Replaces common dot-matrix OCR character misreads inside numeric date candidates
     * (e.g. O/D -> 0, I/l -> 1, S -> 5, B -> 8).
     */
    private static String normalizeDotMatrixCharacters(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] tokens = input.split(" ");
        for (String token : tokens) {
            String upperToken = token.toUpperCase(Locale.US);

            // Check if token is a month code or keyword - keep intact
            if (isMonthOrKeyword(upperToken)) {
                sb.append(token).append(" ");
                continue;
            }

            // If token looks like a date string with digits/slashes/dots or dot-matrix characters
            if (containsNumericCandidates(token)) {
                String sub = token
                        .replaceAll("(?i)[ODQ]", "0")
                        .replaceAll("(?i)[IlL|!]", "1")
                        .replaceAll("(?i)[S$Z]", "5")
                        .replaceAll("(?<=\\d)B(?=\\d)|(?<=/)B(?=\\d)|(?<=\\d)B(?=/)|\\bB(?=\\d{2})", "8");
                sb.append(sub).append(" ");
            } else {
                sb.append(token).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private static boolean isMonthOrKeyword(String token) {
        String t = token.toUpperCase(Locale.US).replaceAll("[^A-Z]", "");
        if (t.length() < 2) return false;
        String[] monthsAndKeywords = {
                "JAN", "FEB", "FEV", "MAR", "APR", "AVR", "MAY", "MAI", "JUN", "JUIN",
                "JUL", "JUIL", "AUG", "AOUT", "SEP", "OCT", "NOV", "DEC",
                "JA", "FE", "MR", "AL", "AP", "AV", "MA", "MY", "JN", "JL", "AU", "OU", "SE", "OC", "NO", "DE",
                "EXP", "BB", "BEST", "BEFORE", "USE", "PFR", "PFD", "PROD", "MFG"
        };
        for (String mk : monthsAndKeywords) {
            if (t.equals(mk)) return true;
        }
        return false;
    }

    private static boolean containsNumericCandidates(String token) {
        return Pattern.compile("[0-9/\\-.]").matcher(token).find() ||
                Pattern.compile("(?i)[ODISB]").matcher(token).find();
    }

    private static String matchDatePatterns(String text) {
        String upper = text.toUpperCase(Locale.US);

        // Pattern 1: YYYY-MM-DD, YYYY/MM/DD, YYYY.MM.DD (e.g. 2026-05-18, 2026/05/18)
        Pattern pYYYYMMDD = Pattern.compile("\\b(\\d{4})[/\\-.](0?[1-9]|1[0-2])[/\\-.](0?[1-9]|[12]\\d|3[01])\\b");
        Matcher mYYYYMMDD = pYYYYMMDD.matcher(upper);
        if (mYYYYMMDD.find()) {
            String g1 = mYYYYMMDD.group(1);
            String g2 = mYYYYMMDD.group(2);
            String g3 = mYYYYMMDD.group(3);
            if (g1 != null && g2 != null && g3 != null) {
                int y = Integer.parseInt(g1);
                int m = Integer.parseInt(g2);
                int d = Integer.parseInt(g3);
                if (isValidDate(y, m, d)) {
                    return formatDate(y, m, d);
                }
            }
        }

        // Pattern 2: DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY (e.g. 18/05/2026)
        Pattern pDDMMYYYY = Pattern.compile("\\b(0?[1-9]|[12]\\d|3[01])[/\\-.](0?[1-9]|1[0-2])[/\\-.](20\\d{2})\\b");
        Matcher mDDMMYYYY = pDDMMYYYY.matcher(upper);
        if (mDDMMYYYY.find()) {
            String g1 = mDDMMYYYY.group(1);
            String g2 = mDDMMYYYY.group(2);
            String g3 = mDDMMYYYY.group(3);
            if (g1 != null && g2 != null && g3 != null) {
                int d = Integer.parseInt(g1);
                int m = Integer.parseInt(g2);
                int y = Integer.parseInt(g3);
                if (isValidDate(y, m, d)) {
                    return formatDate(y, m, d);
                }
            }
        }

        // Pattern 3: MM/DD/YYYY or MM-DD-YYYY (e.g. 05/18/2026)
        Pattern pMMDDYYYY = Pattern.compile("\\b(0?[1-9]|1[0-2])[/\\-.](0?[1-9]|[12]\\d|3[01])[/\\-.](20\\d{2})\\b");
        Matcher mMMDDYYYY = pMMDDYYYY.matcher(upper);
        if (mMMDDYYYY.find()) {
            String g1 = mMMDDYYYY.group(1);
            String g2 = mMMDDYYYY.group(2);
            String g3 = mMMDDYYYY.group(3);
            if (g1 != null && g2 != null && g3 != null) {
                int m = Integer.parseInt(g1);
                int d = Integer.parseInt(g2);
                int y = Integer.parseInt(g3);
                if (isValidDate(y, m, d)) {
                    return formatDate(y, m, d);
                }
            }
        }

        // Pattern 4: YYYY MMM DD or DD MMM YYYY or YY MMM DD or DD MMM YY (e.g., 2026 MAY 18, 18 MAY 2026, 26 MA 18, 27 JN 06)
        Pattern pMonthText = Pattern.compile("\\b(\\d{4}|\\d{2})?[/\\-.\\s]*" +
                "(JAN|FEB|FEV|MAR|APR|AVR|MAY|MAI|JUN|JUIN|JUL|JUIL|AUG|AOUT|SEP|OCT|NOV|DEC|" +
                "JA|FE|MR|AL|AP|AV|MA|MY|JN|JL|AU|OU|SE|OC|NO|DE)" +
                "[/\\-.\\s]*(\\d{1,2})?[/\\-.\\s]*(\\d{4}|\\d{2})?\\b");
        Matcher mMonthText = pMonthText.matcher(upper);
        if (mMonthText.find()) {
            String g1 = mMonthText.group(1);
            String mStr = mMonthText.group(2);
            String g3 = mMonthText.group(3);
            String g4 = mMonthText.group(4);

            int month = parseMonthCode(mStr);
            int year = -1;
            int day = 15;

            if (g1 != null && g4 != null) {
                int val1 = Integer.parseInt(g1);
                int val4 = Integer.parseInt(g4);
                if (val1 > 31) {
                    year = parseYear(g1);
                    day = val4;
                } else {
                    day = val1;
                    year = parseYear(g4);
                }
            } else if (g1 != null) {
                if (g3 != null) {
                    year = parseYear(g1);
                    day = Integer.parseInt(g3);
                } else {
                    year = parseYear(g1);
                }
            } else if (g4 != null) {
                year = parseYear(g4);
            }

            if (year != -1 && isValidDate(year, month, day)) {
                return formatDate(year, month, day);
            }
        }

        // Pattern 5: MM/YYYY or MM-YYYY (e.g. 05/2026)
        Pattern pMMYYYY = Pattern.compile("\\b(0?[1-9]|1[0-2])[/\\-.](20\\d{2})\\b");
        Matcher mMMYYYY = pMMYYYY.matcher(upper);
        if (mMMYYYY.find()) {
            String g1 = mMMYYYY.group(1);
            String g2 = mMMYYYY.group(2);
            if (g1 != null && g2 != null) {
                int m = Integer.parseInt(g1);
                int y = Integer.parseInt(g2);
                int d = getMaxDaysInMonth(m, y);
                if (isValidDate(y, m, d)) {
                    return formatDate(y, m, d);
                }
            }
        }

        // Pattern 6: DD MMM YY or MM/YY (e.g. 18 MAY 26, 05/26)
        Pattern pMMYY = Pattern.compile("\\b(0?[1-9]|1[0-2])[/\\-](\\d{2})\\b");
        Matcher mMMYY = pMMYY.matcher(upper);
        if (mMMYY.find()) {
            String g1 = mMMYY.group(1);
            String g2 = mMMYY.group(2);
            if (g1 != null && g2 != null) {
                int m = Integer.parseInt(g1);
                int y = parseYear(g2);
                int d = getMaxDaysInMonth(m, y);
                if (isValidDate(y, m, d)) {
                    return formatDate(y, m, d);
                }
            }
        }

        return null;
    }

    private static int parseYear(String yStr) {
        if (yStr == null) return Calendar.getInstance().get(Calendar.YEAR);
        int y = Integer.parseInt(yStr);
        if (yStr.length() == 2) {
            return 2000 + y;
        }
        return y;
    }

    private static int parseMonthCode(String mStr) {
        if (mStr == null) return 1;
        String m = mStr.toUpperCase(Locale.US).trim();
        if (m.startsWith("JA")) return 1;
        if (m.startsWith("FE")) return 2;
        if (m.startsWith("MR") || m.startsWith("MAR")) return 3;
        if (m.startsWith("AL") || m.startsWith("AP") || m.startsWith("AV")) return 4;
        if (m.startsWith("MA") || m.startsWith("MY")) return 5;
        if (m.startsWith("JN") || m.startsWith("JUN") || m.startsWith("JUIN")) return 6;
        if (m.startsWith("JL") || m.startsWith("JUL") || m.startsWith("JUIL")) return 7;
        if (m.startsWith("AU") || m.startsWith("AUG") || m.startsWith("OU") || m.startsWith("AOUT")) return 8;
        if (m.startsWith("SE") || m.startsWith("SEP")) return 9;
        if (m.startsWith("OC") || m.startsWith("OCT")) return 10;
        if (m.startsWith("NO") || m.startsWith("NOV")) return 11;
        if (m.startsWith("DE") || m.startsWith("DEC")) return 12;
        return 1;
    }

    private static boolean isValidDate(int year, int month, int day) {
        int maxYear = Calendar.getInstance().get(Calendar.YEAR) + MAX_YEARS_IN_FUTURE;
        if (year < MIN_VALID_YEAR || year > maxYear) {
            return false;
        }
        if (month < 1 || month > 12) {
            return false;
        }
        int maxDays = getMaxDaysInMonth(month, year);
        return day >= 1 && day <= maxDays;
    }

    private static int getMaxDaysInMonth(int month, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private static String formatDate(int year, int month, int day) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day);
    }
}
