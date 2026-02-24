package org.example.rbac;

import org.example.rbac.filter.RoleFilters;
import org.example.rbac.filter.RoleSorters;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoleManagerTest {

    private RoleManager manager;
    private Role role1;
    private Role role2;
    private Permission p1;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        manager = new RoleManager();
        role1 = new Role("Admin", "Admin role", new HashSet<>());
        role2 = new Role("User", "User role", new HashSet<>());
        p1 = new Permission("READ", "DATA", "Read access");
    }

    @Test
    @DisplayName("Добавление и получение роли")
    void testAddAndFind() {
        manager.add(role1);
        assertEquals(1, manager.count());

        assertTrue(manager.findById(role1.getId()).isPresent());
        assertTrue(manager.findByName("Admin").isPresent());
        assertFalse(manager.findByName(null).isPresent());
        assertFalse(manager.findById(null).isPresent());

        assertThrows(IllegalArgumentException.class, () -> manager.add(role1));
        assertThrows(IllegalArgumentException.class, () -> manager.add(null));
    }

    @Test
    @DisplayName("Удаление роли и проверка занятости")
    void testRemove() {
        manager.add(role1);
        assertTrue(manager.remove(role1));
        assertFalse(manager.remove(role1));
        assertFalse(manager.remove(null));
        assertEquals(0, manager.count());

        manager.add(role2);
        manager.setInUseChecker(r -> r.getName().equals("User"));
        assertThrows(IllegalStateException.class, () -> manager.remove(role2));
    }

    @Test
    @DisplayName("Методы фильтрации и сортировки")
    void testFiltersAndSorters() {
        manager.add(role1);
        manager.add(role2);

        assertEquals(2, manager.findAll().size());

        List<Role> filtered = manager.findByFilter(RoleFilters.byName("Admin"));
        assertEquals(1, filtered.size());
        assertEquals(role1, filtered.get(0));

        assertEquals(2, manager.findByFilter(null).size());

        List<Role> sorted = manager.findAll(RoleFilters.byNameContains(""), RoleSorters.byName());
        assertEquals("Admin", sorted.get(0).getName());

        assertThrows(IllegalArgumentException.class, () -> manager.findAll(null, RoleSorters.byName()));
    }

    @Test
    @DisplayName("Управление правами")
    void testPermissionManagement() {
        manager.add(role1);

        manager.addPermissionToRole("Admin", p1);
        assertTrue(role1.hasPermission(p1));

        List<Role> rolesWithPerm = manager.findRolesWithPermission("READ", "data");
        assertEquals(1, rolesWithPerm.size());

        manager.removePermissionFromRole("Admin", p1);
        assertFalse(role1.hasPermission(p1));

        assertThrows(IllegalArgumentException.class, () -> manager.addPermissionToRole(null, p1));
        assertThrows(IllegalArgumentException.class, () -> manager.addPermissionToRole("Unknown", p1));
        assertThrows(IllegalArgumentException.class, () -> manager.removePermissionFromRole("Unknown", p1));
        assertThrows(IllegalArgumentException.class, () -> manager.findRolesWithPermission(null, "data"));
    }

    @Test
    @DisplayName("Служебные методы")
    void testUtilityMethods() {
        manager.add(role1);
        assertTrue(manager.exists("Admin"));
        assertFalse(manager.exists("Unknown"));
        assertFalse(manager.exists(null));

        manager.clear();
        assertEquals(0, manager.count());

        manager.setInUseChecker(null);
    }

    @Test
    @DisplayName("Проверка equals и hashCode")
    void testEqualsAndHashCode() {
        RoleManager m1 = new RoleManager();
        RoleManager m2 = new RoleManager();

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());

        m1.add(role1);
        assertNotEquals(m1, m2);
        m2.add(role1);
        assertEquals(m1, m2);
    }
}