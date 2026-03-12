package org.example.rbac.util;

import java.util.List;

public class FormatUtils {

    private FormatUtils() {}

    public static String padRight(String text, int length) {
        if (text == null) text = "";
        int spaces = length - text.length();
        return spaces > 0 ? text + " ".repeat(spaces) : text;
    }

    public static String padLeft(String text, int length) {
        if (text == null) text = "";
        int spaces = length - text.length();
        return spaces > 0 ? " ".repeat(spaces) + text : text;
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (maxLength < 3) return text.substring(0, Math.min(text.length(), Math.max(0, maxLength)));
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String formatHeader(String text) {
        if (text == null) text = "";
        return "=== " + text + " ===";
    }

    public static String formatBox(String text) {
        if (text == null) text = "";
        String[] lines = text.split("\n");
        int maxLen = 0;
        for (String line : lines) {
            if (line.length() > maxLen) maxLen = line.length();
        }
        String border = "+" + "-".repeat(maxLen + 2) + "+";
        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n");
        for (String line : lines) {
            sb.append("| ").append(padRight(line, maxLen)).append(" |\n");
        }
        sb.append(border);
        return sb.toString();
    }

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) return "";
        int[] colWidths = new int[headers.length];

        for (int i = 0; i < headers.length; i++) {
            colWidths[i] = headers[i] != null ? headers[i].length() : 0;
        }

        if (rows != null) {
            for (String[] row : rows) {
                for (int i = 0; i < Math.min(headers.length, row.length); i++) {
                    int len = row[i] != null ? row[i].length() : 0;
                    if (len > colWidths[i]) colWidths[i] = len;
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        String separator = buildSeparator(colWidths);

        sb.append(separator).append("\n");
        sb.append(buildRow(headers, colWidths)).append("\n");
        sb.append(separator).append("\n");

        if (rows != null) {
            for (String[] row : rows) {
                sb.append(buildRow(row, colWidths)).append("\n");
            }
        }
        sb.append(separator);

        return sb.toString();
    }

    private static String buildSeparator(int[] colWidths) {
        StringBuilder sb = new StringBuilder("+");
        for (int width : colWidths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        return sb.toString();
    }

    private static String buildRow(String[] rowData, int[] colWidths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < colWidths.length; i++) {
            String cell = (i < rowData.length && rowData[i] != null) ? rowData[i] : "";
            sb.append(" ").append(padRight(cell, colWidths[i])).append(" |");
        }
        return sb.toString();
    }
}