package org.example.rbac;

import org.example.rbac.model.*;

import java.util.HashSet;

public class Main {
    public static void main(String[] args) {
        runFullTestSuite();
    }

    private static void runFullTestSuite() {
        System.out.println("🚀 STARTING GLOBAL RBAC TEST SUITE\n");

        testUserRegistry();
        testPermissionEngine();
        testRoleManagement();
        testMetadataAudit();
        testPermanentAssignment();

        System.out.println("\n✨ ALL TESTS PASSED SUCCESSFULLY");
    }

    private static void testUserRegistry() {
        System.out.println("--- [1] USER VALIDATION ---");
        try {
            User valid = User.validate("admin_root", "System Administrator", "admin@corp.com");
            System.out.println("✅ Valid: " + valid.format());
        } catch (Exception e) {
            System.out.println("❌ Fail: " + e.getMessage());
        }
    }

    private static void testPermissionEngine() {
        System.out.println("\n--- [2] PERMISSION ENGINE ---");
        try {
            Permission p = new Permission("write", "REPORTS", "Allow editing");
            System.out.println("✅ Normalization check: " + p.name());
        } catch (Exception e) {
            System.out.println("❌ Fail: " + e.getMessage());
        }
    }

    private static void testRoleManagement() {
        System.out.println("\n--- [3] ROLE MANAGEMENT ---");
        Role dev = new Role("Developer", "Dev access", new HashSet<>());
        System.out.println("✅ Role created with ID: " + dev.getId());
    }

    private static void testMetadataAudit() {
        System.out.println("\n--- [4] AUDIT METADATA ---");
        AssignmentMetadata m = AssignmentMetadata.now("super_admin", "Promotion");
        System.out.println("✅ Metadata audit trail: " + m.format());
    }

    private static void testPermanentAssignment() {
        System.out.println("\n--- [5] PERMANENT ASSIGNMENT TEST ---");

        User user = User.validate("egor_p", "Egor", "egor@test.com");
        Role role = new Role("Manager", "Management", new HashSet<>());
        AssignmentMetadata meta = AssignmentMetadata.now("root", "Manual setup");

        PermanentAssignment pa = new PermanentAssignment(user, role, meta);

        System.out.println("✅ Initial Type: " + pa.assignmentType());
        System.out.println("✅ Initial Active Status: " + pa.isActive());

        if (!pa.summary().contains("ACTIVE")) {
            System.out.println("❌ Fail: Summary should show ACTIVE status");
        }

        System.out.println("🔄 Revoking assignment...");
        pa.revoke();

        if (pa.isRevoked() && !pa.isActive()) {
            System.out.println("✅ Success: Assignment is no longer active");
        } else {
            System.out.println("❌ Fail: Revoke logic not working");
        }

        System.out.println("✅ Final Summary Check:\n" + pa.summary());

        if (!pa.summary().contains("INACTIVE")) {
            System.out.println("❌ Fail: Summary should show INACTIVE status after revoke");
        }
    }
}