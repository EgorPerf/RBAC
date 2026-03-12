package org.example.rbac.model;

import org.example.rbac.util.ValidationUtils;

import java.time.LocalDateTime;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    public static AssignmentMetadata now(String assignedBy, String reason) {
        ValidationUtils.requireNonEmpty(assignedBy, "Assigned by");
        String normBy = ValidationUtils.normalizeString(assignedBy);
        String normReason = ValidationUtils.normalizeString(reason);

        if (normReason.isEmpty()) {
            normReason = "No reason provided";
        }

        return new AssignmentMetadata(
                normBy,
                LocalDateTime.now().toString(),
                normReason
        );
    }

    public String format() {
        return String.format("Assigned by: %s at %s. Reason: %s", assignedBy, assignedAt, reason);
    }
}