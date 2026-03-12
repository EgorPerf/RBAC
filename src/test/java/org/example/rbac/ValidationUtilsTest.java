package org.example.rbac;

import org.example.rbac.util.ValidationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    @DisplayName("isValidUsername: корректные и некорректные значения")
    void testIsValidUsername() {
        assertTrue(ValidationUtils.isValidUsername("user123"));
        assertTrue(ValidationUtils.isValidUsername("User_Name"));
        assertTrue(ValidationUtils.isValidUsername("abc"));
        assertTrue(ValidationUtils.isValidUsername("a".repeat(20)));

        assertFalse(ValidationUtils.isValidUsername(null));
        assertFalse(ValidationUtils.isValidUsername(""));
        assertFalse(ValidationUtils.isValidUsername("ab"));
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21)));
        assertFalse(ValidationUtils.isValidUsername("user name"));
        assertFalse(ValidationUtils.isValidUsername("user@name"));
    }

    @Test
    @DisplayName("isValidEmail: корректные и некорректные значения")
    void testIsValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("first.last@domain.co.uk"));
        assertTrue(ValidationUtils.isValidEmail("user-123@sub.domain.com"));

        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
        assertFalse(ValidationUtils.isValidEmail("plainaddress"));
        assertFalse(ValidationUtils.isValidEmail("@missinguser.com"));
        assertFalse(ValidationUtils.isValidEmail("user@.com"));
        assertFalse(ValidationUtils.isValidEmail("user@domain..com"));
    }

    @Test
    @DisplayName("isValidDate: корректные и некорректные значения")
    void testIsValidDate() {
        assertTrue(ValidationUtils.isValidDate("2023-10-25"));
        assertTrue(ValidationUtils.isValidDate("2000-01-01"));
        assertTrue(ValidationUtils.isValidDate("2024-02-29"));

        assertFalse(ValidationUtils.isValidDate(null));
        assertFalse(ValidationUtils.isValidDate(""));
        assertFalse(ValidationUtils.isValidDate("25-10-2023"));
        assertFalse(ValidationUtils.isValidDate("2023/10/25"));
        assertFalse(ValidationUtils.isValidDate("2023-13-01"));
        assertFalse(ValidationUtils.isValidDate("2023-02-29"));
        assertFalse(ValidationUtils.isValidDate("invalid_date"));
    }

    @Test
    @DisplayName("normalizeString: удаление лишних пробелов и обработка null")
    void testNormalizeString() {
        assertEquals("hello world", ValidationUtils.normalizeString("  hello   world  "));
        assertEquals("test", ValidationUtils.normalizeString("test"));
        assertEquals("", ValidationUtils.normalizeString("   "));
        assertEquals("", ValidationUtils.normalizeString(""));
        assertEquals("", ValidationUtils.normalizeString(null));
    }

    @Test
    @DisplayName("requireNonEmpty: успешное выполнение и выброс исключения")
    void testRequireNonEmpty() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("valid", "Field"));
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty(" a ", "Field"));

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty(null, "TestField"));
        assertTrue(ex1.getMessage().contains("TestField не может быть пустым"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty("", "TestField"));
        assertTrue(ex2.getMessage().contains("TestField не может быть пустым"));

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtils.requireNonEmpty("   ", "TestField"));
        assertTrue(ex3.getMessage().contains("TestField не может быть пустым"));
    }
}