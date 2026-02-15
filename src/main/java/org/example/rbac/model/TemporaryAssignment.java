package org.example.rbac.model;

import java.time.LocalDateTime;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private final boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        if (expiresAt == null || expiresAt.isBlank()) {
            throw new IllegalArgumentException("Дата истечения не может быть пустой.");
        }
        this.expiresAt = expiresAt;
        this.autoRenew = autoRenew;
    }

    @Override
    public boolean isActive() {
        return !isExpired();
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    public boolean isExpired() {
        return LocalDateTime.now().toString().compareTo(expiresAt) > 0;
    }

    public void extend(String newExpirationDate) {
        if (newExpirationDate == null || newExpirationDate.compareTo(expiresAt) <= 0) {
            throw new IllegalArgumentException("Новая дата должна быть позже текущей даты истечения.");
        }
        this.expiresAt = newExpirationDate;
    }

    @Override
    public String summary() {
        return String.format("%s\nExpires at: %s (Auto-renew: %s)",
                super.summary(),
                expiresAt,
                autoRenew ? "YES" : "NO");
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }
}