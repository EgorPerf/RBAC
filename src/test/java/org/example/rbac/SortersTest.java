package org.example.rbac;

import org.example.rbac.filter.AssignmentSorters;
import org.example.rbac.filter.RoleSorters;
import org.example.rbac.filter.UserSorters;
import org.example.rbac.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortersTest {

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
    }

    @Test
    @DisplayName("Проверка UserSorters")
    void testUserSorters() {
        User u1 = User.create("zebra", "Zack", "z@z.com");
        User u2 = User.create("alpha", "Alan", "a@a.com");
        User u3 = User.create("bravo", "Bob", "b@b.com");

        List<User> users = new ArrayList<>(List.of(u1, u2, u3));

        users.sort(UserSorters.byUsername());
        assertEquals("alpha", users.get(0).username());

        users.sort(UserSorters.byFullName());
        assertEquals("Alan", users.get(0).fullName());

        users.sort(UserSorters.byEmail());
        assertEquals("a@a.com", users.get(0).email());
    }

    @Test
    @DisplayName("Проверка RoleSorters")
    void testRoleSorters() {
        Role r1 = new Role("Zeta", "Desc", new HashSet<>());
        Role r2 = new Role("Alpha", "Desc", new HashSet<>());

        r1.addPermission(new Permission("READ", "DATA", "Desc"));
        r1.addPermission(new Permission("WRITE", "DATA", "Desc"));
        r2.addPermission(new Permission("EXECUTE", "DATA", "Desc"));

        List<Role> roles = new ArrayList<>(List.of(r1, r2));

        roles.sort(RoleSorters.byName());
        assertEquals("Alpha", roles.get(0).getName());

        roles.sort(RoleSorters.byPermissionCount());
        assertEquals("Alpha", roles.get(0).getName());
        assertEquals("Zeta", roles.get(1).getName());
    }

    @Test
    @DisplayName("Проверка AssignmentSorters")
    void testAssignmentSorters() {
        User u1 = User.create("z_user", "Z", "z@z.com");
        User u2 = User.create("a_user", "A", "a@a.com");

        Role r1 = new Role("Z_Role", "Desc", new HashSet<>());
        Role r2 = new Role("A_Role", "Desc", new HashSet<>());

        AssignmentMetadata m1 = new AssignmentMetadata("admin", "2026-02-20T10:00", "Reason");
        AssignmentMetadata m2 = new AssignmentMetadata("admin", "2026-01-10T10:00", "Reason");

        RoleAssignment a1 = new PermanentAssignment(u1, r1, m1);
        RoleAssignment a2 = new PermanentAssignment(u2, r2, m2);

        List<RoleAssignment> assignments = new ArrayList<>(List.of(a1, a2));

        assignments.sort(AssignmentSorters.byUsername());
        assertEquals("a_user", assignments.get(0).user().username());

        assignments.sort(AssignmentSorters.byRoleName());
        assertEquals("A_Role", assignments.get(0).role().getName());

        assignments.sort(AssignmentSorters.byAssignmentDate());
        assertEquals("2026-01-10T10:00", assignments.get(0).metadata().assignedAt());
    }
}