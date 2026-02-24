package org.example.rbac;

import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class PermanentAssignmentTest {

    private User user;
    private Role role;
    private AssignmentMetadata meta;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        user = User.create("perm_user", "Permanent User", "perm@test.com");
        role = new Role("AdminRole", "System administrator", new HashSet<>());
        meta = AssignmentMetadata.now("system", "Initial setup");
    }

    @Test
    @DisplayName("Успешное создание и проверка начального состояния")
    void testInitialState() {
        PermanentAssignment assignment = new PermanentAssignment(user, role, meta);

        assertEquals("PERMANENT", assignment.assignmentType());
        assertTrue(assignment.isActive());
        assertFalse(assignment.isRevoked());
    }

    @Test
    @DisplayName("Проверка логики отзыва (revoke)")
    void testRevoke() {
        PermanentAssignment assignment = new PermanentAssignment(user, role, meta);

        assignment.revoke();

        assertFalse(assignment.isActive());
        assertTrue(assignment.isRevoked());
    }

    @Test
    @DisplayName("Повторный вызов revoke не ломает состояние")
    void testMultipleRevokeCalls() {
        PermanentAssignment assignment = new PermanentAssignment(user, role, meta);

        assignment.revoke();
        assignment.revoke();

        assertFalse(assignment.isActive());
        assertTrue(assignment.isRevoked());
    }
}