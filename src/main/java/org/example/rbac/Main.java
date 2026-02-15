package org.example.rbac;

import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;

import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        testUser();
        testPermission();
        testRole();
        testMetadata();
    }

    private static void testUser() {
        System.out.println("=== TEST: User Validation ===");

        try {
            User u = User.validate("egor_123", "Egor Perf", "egor@test.com");
            System.out.println("✅ Valid user: " + u.format());
        } catch (Exception e) {
            System.out.println("❌ Fail: " + e.getMessage());
        }

        String[] invalidUsernames = {"ab", "too_long_username_over_20_chars", "user-name!", ""};
        for (String uname : invalidUsernames) {
            try {
                User.validate(uname, "Test", "test@test.com");
                System.out.println("❌ Fail: Accepted invalid username: " + uname);
            } catch (IllegalArgumentException e) {
                System.out.println("✅ Catch: " + uname + " -> " + e.getMessage());
            }
        }

        try {
            User.validate("valid_user", "Test", "invalid-email@com");
            System.out.println("❌ Fail: Accepted invalid email");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Catch: Invalid email -> " + e.getMessage());
        }
    }

    private static void testPermission() {
        System.out.println("\n=== TEST: Permission Validation ===");

        try {
            Permission p = new Permission("read", "USERS", "Description");
            System.out.println("✅ Normalization: " + p.format());

            System.out.println("✅ Matches exact: " + p.matches("READ", "users"));
            System.out.println("✅ Matches regex: " + p.matches("R.*D", "u.*s"));
        } catch (Exception e) {
            System.out.println("❌ Fail: " + e.getMessage());
        }

        try {
            new Permission("READ WRITE", "users", "Desc");
            System.out.println("❌ Fail: Accepted space in name");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Catch: Space in name -> " + e.getMessage());
        }
    }

    private static void testRole() {
        System.out.println("\n=== TEST: Role Functionality ===");

        Permission pRead = new Permission("READ", "docs", "View docs");
        Permission pWrite = new Permission("WRITE", "docs", "Edit docs");

        Role admin = new Role("Admin", "Superuser", new HashSet<>());
        admin.addPermission(pRead);
        admin.addPermission(pWrite);

        System.out.println("✅ Role format:\n" + admin.format());
        System.out.println("✅ ID starts with role_: " + admin.getId().startsWith("role_"));
        System.out.println("✅ hasPermission (Stream API): " + admin.hasPermission("READ", "docs"));

        try {
            new Role("Admin", "Duplicate", new HashSet<>());
            System.out.println("❌ Fail: Duplicate name allowed");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Catch: Duplicate name -> " + e.getMessage());
        }

        try {
            admin.getPermissions().clear();
            System.out.println("❌ Fail: Unmodifiable collection modified");
        } catch (UnsupportedOperationException e) {
            System.out.println("✅ Catch: Collection is unmodifiable");
        }
    }

    private static void testMetadata() {
        System.out.println("\n=== TEST: AssignmentMetadata ===");

        AssignmentMetadata meta = AssignmentMetadata.now("admin_user", "Adding rights");
        System.out.println("✅ Meta format: " + meta.format());

        AssignmentMetadata metaNoReason = AssignmentMetadata.now("system", null);
        System.out.println("✅ Default reason: " + metaNoReason.reason());

        try {
            AssignmentMetadata.now("", "Desc");
            System.out.println("❌ Fail: Empty assignedBy allowed");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Catch: Empty assignedBy -> " + e.getMessage());
        }
    }
}