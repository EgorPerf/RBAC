package org.example.rbac;

import org.example.rbac.filter.RoleFilter;
import org.example.rbac.filter.RoleFilters;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleFilterTest {

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
    }

    @Test
    @DisplayName("Проверка фильтра byName")
    void testByName() {
        Role role = new Role("Admin", "Desc", new HashSet<>());
        assertTrue(RoleFilters.byName("Admin").test(role));
        assertFalse(RoleFilters.byName("User").test(role));
    }

    @Test
    @DisplayName("Проверка фильтра byNameContains (с игнором регистра)")
    void testByNameContains() {
        Role role = new Role("SuperAdmin", "Desc", new HashSet<>());
        assertTrue(RoleFilters.byNameContains("peradm").test(role));
        assertFalse(RoleFilters.byNameContains("user").test(role));
    }

    @Test
    @DisplayName("Проверка фильтров hasPermission")
    void testHasPermission() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("Editor", "Desc", perms);
        Permission p1 = new Permission("WRITE", "POSTS", "Desc");
        role.addPermission(p1);

        assertTrue(RoleFilters.hasPermission(p1).test(role));
        assertFalse(RoleFilters.hasPermission(new Permission("READ", "USERS", "Desc")).test(role));

        assertTrue(RoleFilters.hasPermission("WRITE", "posts").test(role));
        assertFalse(RoleFilters.hasPermission("DELETE", "posts").test(role));
    }

    @Test
    @DisplayName("Проверка фильтра hasAtLeastNPermissions")
    void testHasAtLeastNPermissions() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("Manager", "Desc", perms);
        role.addPermission(new Permission("READ", "DATA", "Desc"));
        role.addPermission(new Permission("WRITE", "DATA", "Desc"));

        assertTrue(RoleFilters.hasAtLeastNPermissions(1).test(role));
        assertTrue(RoleFilters.hasAtLeastNPermissions(2).test(role));
        assertFalse(RoleFilters.hasAtLeastNPermissions(3).test(role));
    }

    @Test
    @DisplayName("Проверка комбинаций and / or")
    void testComposition() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("Viewer", "Desc", perms);
        role.addPermission(new Permission("READ", "DATA", "Desc"));

        RoleFilter f1 = RoleFilters.byName("Viewer");
        RoleFilter f2 = RoleFilters.hasAtLeastNPermissions(1);
        RoleFilter f3 = RoleFilters.byName("Admin");

        assertTrue(f1.and(f2).test(role));
        assertFalse(f1.and(f3).test(role));
        assertTrue(f3.or(f1).test(role));
        assertFalse(f3.or(RoleFilters.hasAtLeastNPermissions(5)).test(role));
    }
}