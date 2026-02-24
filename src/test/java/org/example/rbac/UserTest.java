package org.example.rbac;

import org.example.rbac.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Успешное создание валидного пользователя")
    void testValidUserCreation() {
        User user = User.create("john_doe123", "John Doe", "john.doe@example.com");
        assertEquals("john_doe123", user.username());
        assertEquals("John Doe", user.fullName());
        assertEquals("john.doe@example.com", user.email());
    }

    @Test
    @DisplayName("Форматирование вывода пользователя")
    void testUserFormat() {
        User user = User.create("admin", "Super Admin", "admin@corp.com");
        assertEquals("admin (Super Admin) <admin@corp.com>", user.format());
    }

    @Test
    @DisplayName("Исключения при невалидном username")
    void testInvalidUsername() {
        assertThrows(IllegalArgumentException.class, () -> User.create(null, "Name", "a@a.com"));
        assertThrows(IllegalArgumentException.class, () -> User.create("ab", "Name", "a@a.com"), "Слишком короткий");
        assertThrows(IllegalArgumentException.class, () -> User.create("this_username_is_way_too_long", "Name", "a@a.com"), "Слишком длинный");
        assertThrows(IllegalArgumentException.class, () -> User.create("bad-name!", "Name", "a@a.com"), "Недопустимые символы");
    }

    @Test
    @DisplayName("Исключения при невалидном email")
    void testInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> User.create("valid_user", "Name", "bademail.com"), "Нет @");
        assertThrows(IllegalArgumentException.class, () -> User.create("valid_user", "Name", "bad@emailcom"), "Нет точки");
    }

    @Test
    @DisplayName("Исключения при пустом fullName")
    void testInvalidFullName() {
        assertThrows(IllegalArgumentException.class, () -> User.create("valid_user", "", "a@a.com"));
        assertThrows(IllegalArgumentException.class, () -> User.create("valid_user", "   ", "a@a.com"));
    }
}