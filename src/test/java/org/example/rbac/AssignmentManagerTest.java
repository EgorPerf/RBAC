package org.example.rbac;

import org.example.rbac.filter.AssignmentFilters;
import org.example.rbac.filter.AssignmentSorters;
import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentManagerTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    private User user;
    private Role role;
    private Role role2;
    private Permission perm;
    private AssignmentMetadata meta;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        user = User.create("testuser", "Test User", "test@test.com");
        userManager.add(user);

        role = new Role("TestRole", "Desc", new HashSet<>());
        perm = new Permission("EXECUTE", "SYSTEM", "Desc");
        role.addPermission(perm);
        roleManager.add(role);

        role2 = new Role("AnotherRole", "Desc", new HashSet<>());
        roleManager.add(role2);

        meta = AssignmentMetadata.now("admin", "Test setup");
    }

    @Test
    @DisplayName("Исключения в конструкторе")
    void testConstructorExceptions() {
        assertThrows(IllegalArgumentException.class, () -> new AssignmentManager(null, roleManager));
        assertThrows(IllegalArgumentException.class, () -> new AssignmentManager(userManager, null));
    }

    @Test
    @DisplayName("Добавление валидного назначения")
    void testValidAdd() {
        PermanentAssignment pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findById(pa.assignmentId()).isPresent());
    }

    @Test
    @DisplayName("Исключения при добавлении")
    void testAddExceptions() {
        PermanentAssignment pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(null));
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(pa));

        User unregisteredUser = User.create("ghost", "Ghost", "g@g.com");
        PermanentAssignment badUserPa = new PermanentAssignment(unregisteredUser, role, meta);
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(badUserPa));

        Role unregisteredRole = new Role("GhostRole", "Desc", new HashSet<>());
        PermanentAssignment badRolePa = new PermanentAssignment(user, unregisteredRole, meta);
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.add(badRolePa));

        TemporaryAssignment duplicateActive = new TemporaryAssignment(user, role, meta, LocalDateTime.now().plusDays(1).toString(), false);
        assertThrows(IllegalStateException.class, () -> assignmentManager.add(duplicateActive));
    }

    @Test
    @DisplayName("Проверка связи RoleManager InUseChecker")
    void testRoleManagerInUseChecker() {
        PermanentAssignment pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);

        assertThrows(IllegalStateException.class, () -> roleManager.remove(role));

        assertDoesNotThrow(() -> roleManager.remove(role2));
    }

    @Test
    @DisplayName("Базовые методы Repository")
    void testRepositoryMethods() {
        PermanentAssignment pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);

        assertEquals(1, assignmentManager.findAll().size());
        assertTrue(assignmentManager.remove(pa));
        assertFalse(assignmentManager.remove(pa));
        assertFalse(assignmentManager.remove(null));
        assertFalse(assignmentManager.findById("wrong_id").isPresent());
        assertFalse(assignmentManager.findById(null).isPresent());

        assignmentManager.add(pa);
        assignmentManager.clear();
        assertEquals(0, assignmentManager.count());
    }

    @Test
    @DisplayName("Поиск по User и Role")
    void testFindByUserAndRole() {
        PermanentAssignment pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);

        assertEquals(1, assignmentManager.findByUser(user).size());
        assertEquals(0, assignmentManager.findByUser(null).size());

        assertEquals(1, assignmentManager.findByRole(role).size());
        assertEquals(0, assignmentManager.findByRole(null).size());
    }

    @Test
    @DisplayName("Фильтрация и сортировка")
    void testFilterAndSort() {
        PermanentAssignment pa1 = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa1);

        List<RoleAssignment> filtered = assignmentManager.findByFilter(AssignmentFilters.activeOnly());
        assertEquals(1, filtered.size());
        assertEquals(1, assignmentManager.findByFilter(null).size());

        List<RoleAssignment> sorted = assignmentManager.findAll(AssignmentFilters.activeOnly(), AssignmentSorters.byRoleName());
        assertEquals(1, sorted.size());

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.findAll(null, AssignmentSorters.byRoleName()));
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.findAll(AssignmentFilters.activeOnly(), null));
    }

    @Test
    @DisplayName("Проверка активных и просроченных назначений")
    void testActiveAndExpired() {
        PermanentAssignment activePerm = new PermanentAssignment(user, role, meta);
        TemporaryAssignment expiredTemp = new TemporaryAssignment(user, role2, meta, LocalDateTime.now().minusDays(1).toString(), false);

        assignmentManager.add(activePerm);
        assignmentManager.add(expiredTemp);

        assertEquals(1, assignmentManager.getActiveAssignments().size());
        assertEquals(1, assignmentManager.getExpiredAssignments().size());
    }

    @Test
    @DisplayName("Проверка прав пользователя (userHasRole, userHasPermission, getUserPermissions)")
    void testUserPermissions() {
        PermanentAssignment pa = new PermanentAssignment(user, role, meta);
        assignmentManager.add(pa);

        assertTrue(assignmentManager.userHasRole(user, role));
        assertFalse(assignmentManager.userHasRole(user, role2));
        assertFalse(assignmentManager.userHasRole(null, role));
        assertFalse(assignmentManager.userHasRole(user, null));

        assertTrue(assignmentManager.userHasPermission(user, "EXECUTE", "system"));
        assertFalse(assignmentManager.userHasPermission(user, "READ", "system"));
        assertFalse(assignmentManager.userHasPermission(null, "EXECUTE", "system"));

        Set<Permission> perms = assignmentManager.getUserPermissions(user);
        assertEquals(1, perms.size());
        assertTrue(perms.contains(perm));
        assertTrue(assignmentManager.getUserPermissions(null).isEmpty());
    }

    @Test
    @DisplayName("Отзыв и продление назначений")
    void testRevokeAndExtend() {
        PermanentAssignment pa = new PermanentAssignment(user, role, meta);
        TemporaryAssignment ta = new TemporaryAssignment(user, role2, meta, LocalDateTime.now().plusDays(1).toString(), false);

        assignmentManager.add(pa);
        assignmentManager.add(ta);

        assignmentManager.revokeAssignment(pa.assignmentId());
        assertFalse(pa.isActive());
        assertThrows(IllegalStateException.class, () -> assignmentManager.revokeAssignment(ta.assignmentId()));

        String newDate = LocalDateTime.now().plusDays(5).toString();
        assignmentManager.extendTemporaryAssignment(ta.assignmentId(), newDate);
        assertEquals(newDate, ta.getExpiresAt());
        assertThrows(IllegalStateException.class, () -> assignmentManager.extendTemporaryAssignment(pa.assignmentId(), newDate));

        assertThrows(IllegalArgumentException.class, () -> assignmentManager.revokeAssignment(null));
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.revokeAssignment("wrong"));
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.extendTemporaryAssignment(null, newDate));
        assertThrows(IllegalArgumentException.class, () -> assignmentManager.extendTemporaryAssignment("wrong", newDate));
    }

    @Test
    @DisplayName("Проверка equals и hashCode")
    void testEqualsAndHashCode() {
        AssignmentManager m1 = new AssignmentManager(userManager, roleManager);
        AssignmentManager m2 = new AssignmentManager(userManager, roleManager);

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }
}