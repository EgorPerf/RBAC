package org.example.rbac;

import org.example.rbac.model.Permission;
import org.example.rbac.model.User;

public class Main {
    public static void main(String[] args) {
        testUserValidation();
        testPermissionValidation();
    }

    private static void testUserValidation() {
        System.out.println("=== Тестирование валидации User ===");

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
            System.out.println("✅ Ожидаемая ошибка: " + e.getMessage());
        }
    }

    private static void testPermissionValidation() {
        System.out.println("\n=== Тестирование Permission ===");

        try {
            Permission p1 = new Permission("read", "USERS", "Чтение списка пользователей");
            System.out.println("✅ Успех нормализации: " + p1.format());

            System.out.println("✅ Поиск точного совпадения: " + p1.matches("READ", "users"));
            System.out.println("✅ Поиск по паттерну: " + p1.matches(".*EA.*", ".*ser.*"));
            System.out.println("✅ Поиск с null: " + p1.matches(null, "users"));
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
        }

        try {
            new Permission("READ WRITE", "users", "Ошибка из-за пробела");
            System.out.println("❌ Тест провален: ожидалась ошибка пробела");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Ожидаемая ошибка: " + e.getMessage());
        }

        try {
            new Permission("DELETE", "reports", "");
            System.out.println("❌ Тест провален: ожидалась ошибка пустого описания");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Ожидаемая ошибка: " + e.getMessage());
        }
    }
}