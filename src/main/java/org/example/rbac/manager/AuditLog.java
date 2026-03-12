package org.example.rbac.manager;

import org.example.rbac.model.AuditEntry;
import org.example.rbac.util.FormatUtils;
import org.example.rbac.util.ValidationUtils;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLog {

    private final List<AuditEntry> entries = new ArrayList<>();

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().toString();
        entries.add(new AuditEntry(
                timestamp,
                ValidationUtils.normalizeString(action),
                ValidationUtils.normalizeString(performer),
                ValidationUtils.normalizeString(target),
                ValidationUtils.normalizeString(details)
        ));
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        String norm = ValidationUtils.normalizeString(performer);
        return entries.stream()
                .filter(e -> e.performer().equals(norm))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> getByAction(String action) {
        String norm = ValidationUtils.normalizeString(action);
        return entries.stream()
                .filter(e -> e.action().equalsIgnoreCase(norm))
                .collect(Collectors.toList());
    }

    public void printLog() {
        printLog(this.entries);
    }

    public void printLog(List<AuditEntry> logEntries) {
        if (logEntries.isEmpty()) {
            System.out.println("Log is empty.");
            return;
        }
        String[] headers = {"Timestamp", "Action", "Performer", "Target", "Details"};
        List<String[]> rows = logEntries.stream().map(e -> {
            String ts = e.timestamp().length() > 19 ? e.timestamp().substring(0, 19).replace("T", " ") : e.timestamp();
            return new String[]{ts, e.action(), e.performer(), e.target(), FormatUtils.truncate(e.details(), 45)};
        }).toList();
        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "Filename");
        try (PrintWriter writer = new PrintWriter(ValidationUtils.normalizeString(filename))) {
            String[] headers = {"Timestamp", "Action", "Performer", "Target", "Details"};
            List<String[]> rows = entries.stream().map(e ->
                    new String[]{e.timestamp(), e.action(), e.performer(), e.target(), e.details()}
            ).toList();
            writer.println(FormatUtils.formatTable(headers, rows));
            System.out.println("Audit log saved successfully to " + filename);
        } catch (Exception e) {
            System.out.println("Error saving audit log: " + e.getMessage());
        }
    }
}