package org.example.rbac.manager;

import org.example.rbac.model.AuditEntry;
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
        System.out.printf("%-20s | %-15s | %-15s | %-15s | %s%n", "Timestamp", "Action", "Performer", "Target", "Details");
        System.out.println("-".repeat(95));
        if (logEntries.isEmpty()) {
            System.out.println("Log is empty.");
            return;
        }
        for (AuditEntry e : logEntries) {
            String ts = e.timestamp();
            if (ts.length() > 19) {
                ts = ts.substring(0, 19).replace("T", " ");
            }
            System.out.printf("%-20s | %-15s | %-15s | %-15s | %s%n",
                    ts, e.action(), e.performer(), e.target(), e.details());
        }
    }

    public void saveToFile(String filename) {
        ValidationUtils.requireNonEmpty(filename, "Filename");
        try (PrintWriter writer = new PrintWriter(ValidationUtils.normalizeString(filename))) {
            writer.printf("%-25s | %-15s | %-15s | %-15s | %s%n", "Timestamp", "Action", "Performer", "Target", "Details");
            writer.println("-".repeat(105));
            for (AuditEntry e : entries) {
                writer.printf("%-25s | %-15s | %-15s | %-15s | %s%n", e.timestamp(), e.action(), e.performer(), e.target(), e.details());
            }
            System.out.println("Audit log saved successfully to " + filename);
        } catch (Exception e) {
            System.out.println("Error saving audit log: " + e.getMessage());
        }
    }
}