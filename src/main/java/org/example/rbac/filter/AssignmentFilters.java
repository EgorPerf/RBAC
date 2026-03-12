package org.example.rbac.filter;

import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.TemporaryAssignment;
import org.example.rbac.model.User;
import org.example.rbac.util.ValidationUtils;

public class AssignmentFilters {

    private AssignmentFilters() {}

    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        ValidationUtils.requireNonEmpty(username, "Username");
        String norm = ValidationUtils.normalizeString(username);
        return assignment -> assignment.user().username().equals(norm);
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        ValidationUtils.requireNonEmpty(roleName, "Role name");
        String norm = ValidationUtils.normalizeString(roleName);
        return assignment -> assignment.role().getName().equals(norm);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        ValidationUtils.requireNonEmpty(type, "Type");
        String norm = ValidationUtils.normalizeString(type).toUpperCase();
        return assignment -> assignment.assignmentType().equals(norm);
    }

    public static AssignmentFilter assignedBy(String username) {
        ValidationUtils.requireNonEmpty(username, "Username");
        String norm = ValidationUtils.normalizeString(username);
        return assignment -> assignment.metadata().assignedBy().equals(norm);
    }

    public static AssignmentFilter assignedAfter(String date) {
        ValidationUtils.requireNonEmpty(date, "Date");
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Invalid date format");
        }
        return assignment -> assignment.metadata().assignedAt().compareTo(date) > 0;
    }

    public static AssignmentFilter expiringBefore(String date) {
        ValidationUtils.requireNonEmpty(date, "Date");
        if (!ValidationUtils.isValidDate(date)) {
            throw new IllegalArgumentException("Invalid date format");
        }
        return assignment -> {
            if (assignment instanceof TemporaryAssignment temp) {
                return temp.getExpiresAt().compareTo(date) < 0;
            }
            return false;
        };
    }
}