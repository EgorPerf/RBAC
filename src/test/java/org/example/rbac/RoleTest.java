package org.example.rbac;

import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
    }

    @Test
    @DisplayName("Успешное создание роли")
    void testValidRoleCreation() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("Admin", "Administrator role", perms);

        assertTrue(role.getId().startsWith("role_"));
        assertEquals("Admin", role.getName());
        assertEquals("Administrator role", role.getDescription());
        assertTrue(role.getPermissions().isEmpty());
    }

    @Test
    @DisplayName("Исключения при невалидных аргументах конструктора")
    void testConstructorExceptions() {
        Set<Permission> perms = new HashSet<>();

        assertThrows(IllegalArgumentException.class, () -> new Role(null, "desc", perms));
        assertThrows(IllegalArgumentException.class, () -> new Role("  ", "desc", perms));
        assertThrows(IllegalArgumentException.class, () -> new Role("Name", null, perms));
        assertThrows(IllegalArgumentException.class, () -> new Role("Name", "  ", perms));
        assertThrows(IllegalArgumentException.class, () -> new Role("Name", "desc", null));
    }

    @Test
    @DisplayName("Исключение при дублировании имени роли")
    void testDuplicateRoleName() {
        Set<Permission> perms = new HashSet<>();
        new Role("Manager", "Desc", perms);

        assertThrows(IllegalArgumentException.class, () -> new Role("Manager", "Another desc", perms));
        assertThrows(IllegalArgumentException.class, () -> new Role("  Manager  ", "Another desc", perms));
    }

    @Test
    @DisplayName("Добавление и удаление прав")
    void testPermissionManagement() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("User", "User role", perms);
        Permission p1 = new Permission("READ", "DATA", "Read data");
        Permission p2 = new Permission("WRITE", "DATA", "Write data");

        role.addPermission(p1);
        assertTrue(role.hasPermission(p1));
        assertFalse(role.hasPermission(p2));

        role.addPermission(null);
        assertEquals(1, role.getPermissions().size());

        role.removePermission(p1);
        assertFalse(role.hasPermission(p1));

        role.removePermission(null);
        assertEquals(0, role.getPermissions().size());
    }

    @Test
    @DisplayName("Проверка прав по имени и ресурсу")
    void testHasPermissionByNameAndResource() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("Editor", "Editor role", perms);
        role.addPermission(new Permission("DELETE", "POSTS", "Delete posts"));

        assertTrue(role.hasPermission("DELETE", "posts"));
        assertTrue(role.hasPermission("DEL", "po"));
        assertFalse(role.hasPermission("WRITE", "posts"));
        assertFalse(role.hasPermission(null, "posts"));
        assertFalse(role.hasPermission("DELETE", null));
    }

    @Test
    @DisplayName("Защита коллекции прав от изменений извне")
    void testUnmodifiablePermissions() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("Viewer", "Viewer role", perms);
        Set<Permission> returnedPerms = role.getPermissions();

        assertThrows(UnsupportedOperationException.class, () -> returnedPerms.add(new Permission("READ", "ALL", "Desc")));
    }

    @Test
    @DisplayName("Проверка equals и hashCode")
    void testEqualsAndHashCode() {
        Set<Permission> perms = new HashSet<>();
        Role role1 = new Role("Role1", "Desc1", perms);
        Role role2 = new Role("Role2", "Desc2", perms);

        assertEquals(role1, role1);
        assertNotEquals(role1, role2);
        assertNotEquals(null, role1);
        assertNotEquals(new Object(), role1);
        assertEquals(role1.hashCode(), role1.hashCode());
    }

    @Test
    @DisplayName("Проверка toString и format")
    void testToStringAndFormat() {
        Set<Permission> perms = new HashSet<>();
        Role role = new Role("TestRole", "Test Desc", perms);
        Permission p = new Permission("EXECUTE", "SYSTEM", "Exec sys");
        role.addPermission(p);

        assertTrue(role.toString().contains("TestRole"));
        assertTrue(role.toString().contains("permissionsCount=1"));

        String formatted = role.format();
        assertTrue(formatted.contains("Role: TestRole [ID: role_"));
        assertTrue(formatted.contains("Description: Test Desc"));
        assertTrue(formatted.contains("Permissions (1):"));
        assertTrue(formatted.contains("- EXECUTE on system: Exec sys"));
    }
}