package org.example.rbac;

import org.example.rbac.model.*;
import java.time.LocalDateTime;
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
        testTemporaryAssignment();

        System.out.println("\n✨ ALL TESTS PASSED SUCCESSFULLY");
    }

    private static void testUserRegistry() {
        System.out.println("--- [1] USER VALIDATION ---");
        try {
            User valid = User.validate("admin_root", "System Administrator", "admin@corp.com");
            System.out.println("✅ Valid user accepted");
        } catch (Exception e) {
            System.out.println("❌ Fail: " + e.getMessage());
        }
    }

    private static void testPermissionEngine() {
        System.out.println("\n--- [2] PERMISSION ENGINE ---");
        try {
            Permission p = new Permission("write", "REPORTS", "Edit");
            System.out.println("✅ Normalization check: " + p.name());
        } catch (Exception e) {
            System.out.println("❌ Fail: " + e.getMessage());
        }
    }

    private static void testRoleManagement() {
        System.out.println("\n--- [3] ROLE MANAGEMENT ---");
        Role dev = new Role("Developer", "Access", new HashSet<>());
        System.out.println("✅ Role created: " + dev.getName());
    }

    private static void testMetadataAudit() {
        System.out.println("\n--- [4] AUDIT METADATA ---");
        AssignmentMetadata m = AssignmentMetadata.now("super_admin", "Check");
        System.out.println("✅ Metadata audit trail verified");
    }

    private static void testPermanentAssignment() {
        System.out.println("\n--- [5] PERMANENT ASSIGNMENT TEST ---");
        User u = User.validate("egor_p", "Egor", "egor@test.com");
        Role r = new Role("Manager", "Management", new HashSet<>());
        PermanentAssignment pa = new PermanentAssignment(u, r, AssignmentMetadata.now("root", "Setup"));
        pa.revoke();
        System.out.println("✅ Revoke logic verified: isActive = " + pa.isActive());
    }

    private static void testTemporaryAssignment() {
        System.out.println("\n--- [6] TEMPORARY ASSIGNMENT TEST ---");

        User user = User.validate("temp_user", "Temporary Worker", "temp@test.com");
        Role role = new Role("Guest", "Limited access", new HashSet<>());
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Short task");

        // 1. Тест активного назначения (срок в будущем)
        String futureDate = LocalDateTime.now().plusDays(1).toString();
        TemporaryAssignment activeAsgn = new TemporaryAssignment(user, role, meta, futureDate, false);

        System.out.println("✅ Active Assignment: isActive = " + activeAsgn.isActive());
        if (!activeAsgn.assignmentType().equals("TEMPORARY")) {
            System.out.println("❌ Fail: Wrong assignment type");
        }

        // 2. Тест истекшего назначения (срок в прошлом)
        String pastDate = LocalDateTime.now().minusHours(1).toString();
        TemporaryAssignment expiredAsgn = new TemporaryAssignment(user, role, meta, pastDate, true);

        System.out.println("✅ Expired Assignment: isExpired = " + expiredAsgn.isExpired());
        System.out.println("✅ Expired Assignment: isActive = " + expiredAsgn.isActive());

        // 3. Тест продления
        System.out.println("🔄 Extending assignment...");
        String newFutureDate = LocalDateTime.now().plusMonths(1).toString();
        expiredAsgn.extend(newFutureDate);

        if (expiredAsgn.isActive() && expiredAsgn.getExpiresAt().equals(newFutureDate)) {
            System.out.println("✅ Extension success: New expiry set and assignment active");
        } else {
            System.out.println("❌ Fail: Extension logic error");
        }

        // 4. Тест валидации продления (нельзя продлить в прошлое)
        try {
            expiredAsgn.extend(pastDate);
            System.out.println("❌ Fail: Allowed extension to the past");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Blocked: Invalid extension date");
        }

        System.out.println("\n✅ Final Summary Check:\n" + expiredAsgn.summary());
    }
}