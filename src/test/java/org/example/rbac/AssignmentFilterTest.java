package org.example.rbac;

import org.example.rbac.filter.AssignmentFilter;
import org.example.rbac.filter.AssignmentFilters;
import org.example.rbac.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentFilterTest {

    private User user1;
    private User user2;
    private Role role1;
    private Role role2;
    private PermanentAssignment permAsgn;
    private TemporaryAssignment tempAsgn;
    private String pastDate;
    private String futureDate;
    private String farFutureDate;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();

        user1 = User.create("alice", "Alice", "alice@test.com");
        user2 = User.create("bob", "Bob", "bob@test.com");

        role1 = new Role("Admin", "Admin Role", new HashSet<>());
        role2 = new Role("Guest", "Guest Role", new HashSet<>());

        pastDate = LocalDateTime.now().minusDays(5).toString();
        futureDate = LocalDateTime.now().plusDays(5).toString();
        farFutureDate = LocalDateTime.now().plusDays(10).toString();

        AssignmentMetadata meta1 = new AssignmentMetadata("sysadmin", LocalDateTime.now().minusDays(1).toString(), "Setup");
        AssignmentMetadata meta2 = new AssignmentMetadata("manager", LocalDateTime.now().minusDays(2).toString(), "Temp access");

        permAsgn = new PermanentAssignment(user1, role1, meta1);
        tempAsgn = new TemporaryAssignment(user2, role2, meta2, futureDate, false);
    }

    @Test
    @DisplayName("Фильтры по User и Username")
    void testUserFilters() {
        assertTrue(AssignmentFilters.byUser(user1).test(permAsgn));
        assertFalse(AssignmentFilters.byUser(user2).test(permAsgn));

        assertTrue(AssignmentFilters.byUsername("bob").test(tempAsgn));
        assertFalse(AssignmentFilters.byUsername("alice").test(tempAsgn));
    }

    @Test
    @DisplayName("Фильтры по Role и RoleName")
    void testRoleFilters() {
        assertTrue(AssignmentFilters.byRole(role1).test(permAsgn));
        assertFalse(AssignmentFilters.byRole(role2).test(permAsgn));

        assertTrue(AssignmentFilters.byRoleName("Guest").test(tempAsgn));
        assertFalse(AssignmentFilters.byRoleName("Admin").test(tempAsgn));
    }

    @Test
    @DisplayName("Фильтры активности (active / inactive)")
    void testActivityFilters() {
        assertTrue(AssignmentFilters.activeOnly().test(permAsgn));
        assertFalse(AssignmentFilters.inactiveOnly().test(permAsgn));

        permAsgn.revoke();

        assertFalse(AssignmentFilters.activeOnly().test(permAsgn));
        assertTrue(AssignmentFilters.inactiveOnly().test(permAsgn));
    }

    @Test
    @DisplayName("Фильтры по типу назначения")
    void testByTypeFilter() {
        assertTrue(AssignmentFilters.byType("PERMANENT").test(permAsgn));
        assertFalse(AssignmentFilters.byType("TEMPORARY").test(permAsgn));

        assertTrue(AssignmentFilters.byType("TEMPORARY").test(tempAsgn));
    }

    @Test
    @DisplayName("Фильтры по метаданным (assignedBy, assignedAfter)")
    void testMetadataFilters() {
        assertTrue(AssignmentFilters.assignedBy("sysadmin").test(permAsgn));
        assertFalse(AssignmentFilters.assignedBy("manager").test(permAsgn));

        assertTrue(AssignmentFilters.assignedAfter(pastDate).test(permAsgn));
        assertFalse(AssignmentFilters.assignedAfter(futureDate).test(permAsgn));
    }

    @Test
    @DisplayName("Фильтр expiringBefore")
    void testExpiringBeforeFilter() {
        assertTrue(AssignmentFilters.expiringBefore(farFutureDate).test(tempAsgn));
        assertFalse(AssignmentFilters.expiringBefore(pastDate).test(tempAsgn));

        assertFalse(AssignmentFilters.expiringBefore(farFutureDate).test(permAsgn));
    }

    @Test
    @DisplayName("Комбинация фильтров (and / or)")
    void testComposition() {
        AssignmentFilter f1 = AssignmentFilters.byUsername("alice");
        AssignmentFilter f2 = AssignmentFilters.activeOnly();
        AssignmentFilter f3 = AssignmentFilters.byType("TEMPORARY");

        assertTrue(f1.and(f2).test(permAsgn));
        assertFalse(f1.and(f3).test(permAsgn));

        assertTrue(f3.or(f1).test(permAsgn));
        assertFalse(f3.or(AssignmentFilters.inactiveOnly()).test(permAsgn));
    }
}