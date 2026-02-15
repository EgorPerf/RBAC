package org.example.rbac.model;

import java.time.LocalDateTime;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    public static AssignmentMetadata now(String assignedBy, String reason) {
        if (assignedBy == null || assignedBy.isBlank()) {
            throw new IllegalArgumentException("Имя назначившего не может быть пустым.");
        }
        return new AssignmentMetadata(
                assignedBy,
                LocalDateTime.now().toString(),
                (reason == null || reason.isBlank()) ? "No reason provided" : reason
        );
    }

    public String format() {
        return String.format("Assigned by: %s at %s. Reason: %s", assignedBy, assignedAt, reason);
    }
}