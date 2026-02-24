package org.example.rbac;

import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.Role;
import org.example.rbac.model.RoleAssignment;
import org.example.rbac.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class RoleAssignmentTest {

    @Test
    @DisplayName("Проверка контракта интерфейса")
    void testInterfaceContract() {
        User expectedUser = User.create("test_user", "Test Name", "test@test.com");
        Role expectedRole = new Role("TestRole", "Desc", new HashSet<>());
        AssignmentMetadata expectedMeta = AssignmentMetadata.now("admin", "Setup");

        RoleAssignment assignment = new RoleAssignment() {
            @Override
            public String assignmentId() { return "asgn_123"; }
            @Override
            public User user() { return expectedUser; }
            @Override
            public Role role() { return expectedRole; }
            @Override
            public AssignmentMetadata metadata() { return expectedMeta; }
            @Override
            public boolean isActive() { return true; }
            @Override
            public String assignmentType() { return "CUSTOM"; }
        };

        assertEquals("asgn_123", assignment.assignmentId());
        assertEquals(expectedUser, assignment.user());
        assertEquals(expectedRole, assignment.role());
        assertEquals(expectedMeta, assignment.metadata());
        assertTrue(assignment.isActive());
        assertEquals("CUSTOM", assignment.assignmentType());
    }
}