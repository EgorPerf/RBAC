package org.example.rbac.filter;

import org.example.rbac.model.RoleAssignment;

import java.util.Comparator;

public class AssignmentSorters {

    private AssignmentSorters() {}

    public static Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(assignment -> assignment.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(assignment -> assignment.role().getName());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(assignment -> assignment.metadata().assignedAt());
    }
}