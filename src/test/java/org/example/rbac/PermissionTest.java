package org.example.rbac;

import org.example.rbac.model.Permission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PermissionTest {

    @Test
    @DisplayName("Успешное создание и нормализация")
    void testValidPermission() {
        Permission p = new Permission("read", "USERS", "Description");
        assertEquals("READ", p.name());
        assertEquals("users", p.resource());
        assertEquals("Description", p.description());
    }

    @Test
    @DisplayName("Форматирование вывода")
    void testFormat() {
        Permission p = new Permission("write", "REPORTS", "Can edit reports");
        assertEquals("WRITE on reports: Can edit reports", p.format());
    }

    @Test
    @DisplayName("Исключения при невалидном name")
    void testInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> new Permission(null, "res", "desc"));
        assertThrows(IllegalArgumentException.class, () -> new Permission("", "res", "desc"));
        assertThrows(IllegalArgumentException.class, () -> new Permission("READ WRITE", "res", "desc"));
    }

    @Test
    @DisplayName("Исключения при невалидном resource")
    void testInvalidResource() {
        assertThrows(IllegalArgumentException.class, () -> new Permission("READ", null, "desc"));
        assertThrows(IllegalArgumentException.class, () -> new Permission("READ", "  ", "desc"));
    }

    @Test
    @DisplayName("Исключения при невалидном description")
    void testInvalidDescription() {
        assertThrows(IllegalArgumentException.class, () -> new Permission("READ", "res", null));
        assertThrows(IllegalArgumentException.class, () -> new Permission("READ", "res", ""));
    }

    @Test
    @DisplayName("Проверка метода matches")
    void testMatches() {
        Permission p = new Permission("DELETE", "DOCUMENTS", "Can delete docs");

        assertTrue(p.matches("DELETE", "documents"));
        assertTrue(p.matches("DEL", "doc"));
        assertTrue(p.matches(null, "documents"));
        assertTrue(p.matches("DELETE", null));
        assertFalse(p.matches("READ", "documents"));
        assertFalse(p.matches("DELETE", "users"));
    }
}