package org.example.rbac;

import org.example.rbac.model.User;

public class Main {
    public static void main(String[] args) {
        testUserValidation();
    }

    private static void testUserValidation() {
        System.out.println("=== Тестирование валидации User (V2) ===\n");

        try {
            User validUser = User.validate("egor_dev", "Егор Перфильев", "egor@example.com");
            System.out.println("✅ Успех: " + validUser.format());
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
        }

        try {
            User.validate("ab", "Иван Иванов", "ivan@example.com");
            System.out.println("❌ Тест провален: ожидалась ошибка валидации");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Ожидаемая ошибка (длина username): " + e.getMessage());
        }

        try {
            User.validate("egor-dev", "Егор", "test@example.com");
            System.out.println("❌ Тест провален: ожидалась ошибка символов");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Ожидаемая ошибка (символы username): " + e.getMessage());
        }

        try {
            User.validate("admin_123", "Админ", "admin_at_localhost.com");
            System.out.println("❌ Тест провален: ожидалась ошибка email");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Ожидаемая ошибка (email): " + e.getMessage());
        }
    }
}