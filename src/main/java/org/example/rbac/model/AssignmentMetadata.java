package org.example.rbac.model;

import org.example.rbac.util.DateUtils;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {

    public AssignmentMetadata {
        if (assignedBy == null || assignedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("assignedBy cannot be null or empty");
        }
        if (reason == null || reason.trim().isEmpty()) {
            reason = "No reason provided";
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        return new AssignmentMetadata(assignedBy, DateUtils.getCurrentDateTime(), reason);
    }

    public String format() {
        return String.format("Assigned by: %s at %s. Reason: %s", assignedBy, assignedAt, reason);
    }
}