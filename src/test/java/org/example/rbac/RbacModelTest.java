package org.example.rbac;

import org.example.rbac.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class RbacModelTest {

    // Этот метод запускается автоматически ПЕРЕД каждым тестом
    @BeforeEach
    void setUp() {
        // Очищаем кэш имен ролей, чтобы тесты были полностью независимыми
        Role.clearUsedNames();
    }

    @Test
    @DisplayName("User: успешное создание и валидация")
    void testUserCreation() {
        User user = User.create("admin_root", "System Administrator", "admin@corp.com");

        assertEquals("admin_root", user.username());
        assertEquals("System Administrator", user.fullName());
        assertEquals("admin@corp.com", user.email());
        assertEquals("admin_root (System Administrator) <admin@corp.com>", user.format());
    }

    @Test
    @DisplayName("User: выброс исключения при неверном email")
    void testInvalidUserEmailThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            User.create("admin_root", "System Administrator", "bad-email");
        });
    }

    @Test
    @DisplayName("Permission: нормализация и проверка по шаблону")
    void testPermissionLogic() {
        Permission p = new Permission("write", "REPORTS", "Edit access");

        assertEquals("WRITE", p.name());
        assertEquals("reports", p.resource());
        assertTrue(p.matches("WRITE", "reports"));
        assertTrue(p.matches("WR", "rep")); // Проверка частичного совпадения
    }

    @Test
    @DisplayName("Role: создание, уникальность имени и управление правами")
    void testRoleLogic() {
        // Передаем изменяемый HashSet, как и просил староста
        Role admin = new Role("Admin", "Full access", new HashSet<>());

        assertTrue(admin.getId().startsWith("role_"));

        // Проверка на уникальность имени
        assertThrows(IllegalArgumentException.class, () -> {
            new Role("  Admin  ", "Duplicate", new HashSet<>());
        }, "Должна быть ошибка, так как имя Admin уже занято");

        // Проверка добавления прав
        Permission p = new Permission("READ", "USERS", "Read users");
        admin.addPermission(p);

        assertTrue(admin.hasPermission(p));
        assertTrue(admin.hasPermission("READ", "users"));
    }

    @Test
    @DisplayName("AssignmentMetadata: автоматическое создание")
    void testMetadataAudit() {
        AssignmentMetadata m = AssignmentMetadata.now("super_admin", "Check");

        assertEquals("super_admin", m.assignedBy());
        assertEquals("Check", m.reason());
        assertNotNull(m.assignedAt());
    }

    @Test
    @DisplayName("PermanentAssignment: логика отзыва (revoke)")
    void testPermanentAssignmentRevoke() {
        User u = User.create("egor_p", "Egor", "egor@test.com");
        Role r = new Role("Manager", "Management", new HashSet<>());
        PermanentAssignment pa = new PermanentAssignment(u, r, AssignmentMetadata.now("root", "Setup"));

        assertTrue(pa.isActive());
        assertEquals("PERMANENT", pa.assignmentType());

        pa.revoke();

        assertFalse(pa.isActive());
        assertTrue(pa.isRevoked());
    }

    @Test
    @DisplayName("TemporaryAssignment: проверка сроков и продления")
    void testTemporaryAssignmentDatesAndExtension() {
        User user = User.create("temp_user", "Temp", "temp@test.com");
        Role role = new Role("Guest", "Limited", new HashSet<>());
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Short task");

        // 1. Активное назначение (на 2 дня вперед)
        String futureDate = LocalDateTime.now().plusDays(2).toString();
        TemporaryAssignment activeAsgn = new TemporaryAssignment(user, role, meta, futureDate, false);

        assertTrue(activeAsgn.isActive());
        assertEquals("TEMPORARY", activeAsgn.assignmentType());
        assertTrue(activeAsgn.getTimeRemaining().contains("days"));

        // 2. Истекшее назначение
        String pastDate = LocalDateTime.now().minusHours(1).toString();
        TemporaryAssignment expiredAsgn = new TemporaryAssignment(user, role, meta, pastDate, true);

        assertFalse(expiredAsgn.isActive());
        assertTrue(expiredAsgn.isExpired());
        assertEquals("Time is up", expiredAsgn.getTimeRemaining());

        // 3. Продление
        String newFutureDate = LocalDateTime.now().plusMonths(1).toString();
        expiredAsgn.extend(newFutureDate);

        assertTrue(expiredAsgn.isActive());
    }
}