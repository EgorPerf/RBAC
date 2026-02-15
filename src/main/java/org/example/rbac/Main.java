package org.example.rbac;

import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;

import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        testUserValidation();
        testPermissionValidation();
        testRole();
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

    private static void testRole() {
        System.out.println("\n=== Тестирование Role ===");

        Role adminRole = new Role("Administrator", "Full system access", new HashSet<>());

        Permission readUsers = new Permission("READ", "users", "Can view user list");
        Permission writeUsers = new Permission("WRITE", "users", "Can create and edit users");
        Permission deleteUsers = new Permission("DELETE", "users", "Can delete users");

        adminRole.addPermission(readUsers);
        adminRole.addPermission(writeUsers);
        adminRole.addPermission(deleteUsers);

        System.out.println(adminRole.format());

        System.out.println("\n✅ hasPermission (объект): " + adminRole.hasPermission(readUsers));
        System.out.println("✅ hasPermission (строки): " + adminRole.hasPermission("DELETE", "users"));
        System.out.println("✅ Отсутствующее право: " + !adminRole.hasPermission("WRITE", "reports"));

        try {
            new Role("Administrator", "Duplicate test", new HashSet<>());
            System.out.println("❌ Тест провален: создана роль с дублирующимся именем!");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Ожидаемая ошибка (дубликат имени): " + e.getMessage());
        }

        try {
            adminRole.getPermissions().add(new Permission("EXECUTE", "system", "Hack"));
            System.out.println("❌ Тест провален: коллекция не защищена!");
        } catch (UnsupportedOperationException e) {
            System.out.println("✅ Ожидаемая ошибка (неизменяемая коллекция): " + e.getClass().getSimpleName());
        }
    }
}