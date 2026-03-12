package org.example.rbac.model;

import org.example.rbac.util.DateUtils;
import org.example.rbac.util.ValidationUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TemporaryAssignment extends AbstractRoleAssignment {

    private String expiresAt;
    private final boolean autoRenew;

    public TemporaryAssignment(User user, Role role, AssignmentMetadata metadata, String expiresAt, boolean autoRenew) {
        super(user, role, metadata);
        ValidationUtils.requireNonEmpty(expiresAt, "Дата истечения");
        this.expiresAt = ValidationUtils.normalizeString(expiresAt);
        this.autoRenew = autoRenew;
    }

    @Override
    public String assignmentType() {
        return "TEMPORARY";
    }

    @Override
    public boolean isActive() {
        String current = DateUtils.getCurrentDateTime();
        String normalizedExpires = this.expiresAt.replace("T", " ");

        // Используем DateUtils для строкового сравнения дат
        if (DateUtils.isBefore(normalizedExpires, current)) {
            if (autoRenew) {
                // Используем DateUtils для добавления дней при автопродлении
                this.expiresAt = DateUtils.addDays(this.expiresAt, 30) + " 23:59:59";
                return true;
            }
            return false;
        }
        return true;
    }

    public boolean isExpired() {
        return !isActive();
    }

    public void extend(String newExpirationDate) {
        ValidationUtils.requireNonEmpty(newExpirationDate, "Новая дата истечения");
        String normDate = ValidationUtils.normalizeString(newExpirationDate);

        String current = DateUtils.getCurrentDateTime();
        String normalizedNew = normDate.replace("T", " ");
        String normalizedCurrent = this.expiresAt.replace("T", " ");

        // Используем DateUtils для проверки валидности новой даты
        if (normalizedNew.equals(normalizedCurrent) || DateUtils.isBefore(normalizedNew, current)) {
            throw new IllegalArgumentException("Новая дата должна быть в будущем и отличаться от текущей.");
        }
        this.expiresAt = normDate;
    }

    @Override
    public String summary() {
        return String.format("%s\n[%s] Expires at: %s, Auto-renew: %s",
                super.summary(),
                assignmentType(),
                expiresAt,
                autoRenew ? "YES" : "NO");
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public String getTimeRemaining() {
        if (isExpired()) {
            return "Time is up";
        }
        try {
            LocalDateTime current = LocalDateTime.now();
            LocalDateTime exp = LocalDateTime.parse(this.expiresAt.replace(" ", "T"));
            long days = ChronoUnit.DAYS.between(current, exp);
            LocalDateTime currentPlusDays = current.plusDays(days);
            long hours = ChronoUnit.HOURS.between(currentPlusDays, exp);
            return days + " days, " + hours + " hours remaining";
        } catch (Exception e) {
            return "Time is up";
        }
    }
}