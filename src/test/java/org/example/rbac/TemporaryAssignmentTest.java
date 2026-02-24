package org.example.rbac;

import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.Role;
import org.example.rbac.model.TemporaryAssignment;
import org.example.rbac.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class TemporaryAssignmentTest {

    private User user;
    private Role role;
    private AssignmentMetadata meta;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        user = User.create("temp_user", "Temporary User", "temp@test.com");
        role = new Role("GuestRole", "Guest access", new HashSet<>());
        meta = AssignmentMetadata.now("admin", "Temporary access");
    }

    @Test
    @DisplayName("Успешное создание активного назначения")
    void testActiveAssignmentCreation() {
        String futureDate = LocalDateTime.now().plusDays(3).toString();
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, meta, futureDate, true);

        assertEquals("TEMPORARY", assignment.assignmentType());
        assertTrue(assignment.isActive());
        assertFalse(assignment.isExpired());
        assertEquals(futureDate, assignment.getExpiresAt());
        assertTrue(assignment.isAutoRenew());
    }

    @Test
    @DisplayName("Исключения при невалидной дате истечения")
    void testConstructorInvalidDate() {
        assertThrows(IllegalArgumentException.class, () -> new TemporaryAssignment(user, role, meta, null, false));
        assertThrows(IllegalArgumentException.class, () -> new TemporaryAssignment(user, role, meta, "", false));
        assertThrows(IllegalArgumentException.class, () -> new TemporaryAssignment(user, role, meta, "   ", false));
    }

    @Test
    @DisplayName("Проверка истекшего назначения")
    void testExpiredAssignment() {
        String pastDate = LocalDateTime.now().minusDays(1).toString();
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, meta, pastDate, false);

        assertFalse(assignment.isActive());
        assertTrue(assignment.isExpired());
    }

    @Test
    @DisplayName("Успешное продление (extend)")
    void testValidExtension() {
        String initialDate = LocalDateTime.now().plusDays(1).toString();
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, meta, initialDate, false);

        String newDate = LocalDateTime.now().plusDays(10).toString();
        assignment.extend(newDate);

        assertEquals(newDate, assignment.getExpiresAt());
        assertTrue(assignment.isActive());
    }

    @Test
    @DisplayName("Исключения при невалидном продлении")
    void testInvalidExtension() {
        String initialDate = LocalDateTime.now().plusDays(5).toString();
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, meta, initialDate, false);

        String pastDate = LocalDateTime.now().minusDays(1).toString();
        String sameDate = initialDate;

        assertThrows(IllegalArgumentException.class, () -> assignment.extend(null));
        assertThrows(IllegalArgumentException.class, () -> assignment.extend(pastDate));
        assertThrows(IllegalArgumentException.class, () -> assignment.extend(sameDate));
    }

    @Test
    @DisplayName("Проверка вывода summary()")
    void testSummary() {
        String futureDate = LocalDateTime.now().plusDays(2).toString();
        TemporaryAssignment assignment = new TemporaryAssignment(user, role, meta, futureDate, true);
        String summary = assignment.summary();

        assertTrue(summary.contains("[TEMPORARY]"));
        assertTrue(summary.contains("Expires at: " + futureDate));
        assertTrue(summary.contains("Auto-renew: YES"));
    }

    @Test
    @DisplayName("Проверка расчета оставшегося времени getTimeRemaining()")
    void testGetTimeRemaining() {
        String pastDate = LocalDateTime.now().minusHours(2).toString();
        TemporaryAssignment expiredAssignment = new TemporaryAssignment(user, role, meta, pastDate, false);
        assertEquals("Time is up", expiredAssignment.getTimeRemaining());

        String futureDate = LocalDateTime.now().plusDays(2).plusHours(5).toString();
        TemporaryAssignment activeAssignment = new TemporaryAssignment(user, role, meta, futureDate, false);
        String timeRemaining = activeAssignment.getTimeRemaining();

        assertTrue(timeRemaining.contains("days"));
        assertTrue(timeRemaining.contains("hours remaining"));
    }
}