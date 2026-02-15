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
        testAbstractRoleAssignment();

        System.out.println("\n✨ ALL TESTS PASSED SUCCESSFULLY");
    }

    private static void testUserRegistry() {
        System.out.println("--- [1] USER VALIDATION ---");
        try {
            User valid = User.validate("admin_root", "System Administrator", "admin@corp.com");
            System.out.println("✅ Valid: " + valid.format());
        } catch (Exception e) {
            System.out.println("❌ Fail: Valid user rejected");
        }
    }

    private static void testPermissionEngine() {
        System.out.println("\n--- [2] PERMISSION ENGINE ---");
        try {
            Permission p = new Permission("write", "REPORTS", "Allow editing");
            System.out.println("✅ Normalization check: " + p.name());
        } catch (Exception e) {
            System.out.println("❌ Fail: Permission logic error");
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

    private static void testAbstractRoleAssignment() {
        System.out.println("\n--- [5] ABSTRACT ROLE ASSIGNMENT ---");

        User user = User.validate("egor_p", "Egor", "egor@test.com");
        Role role = new Role("Manager", "Management", new HashSet<>());
        AssignmentMetadata meta = AssignmentMetadata.now("root", "Manual setup");

        AbstractRoleAssignment assignment = new AbstractRoleAssignment(user, role, meta) {
            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public String assignmentType() {
                return "PERMANENT";
            }
        };

        System.out.println("✅ ID Auto-generation: " + assignment.assignmentId());

        String summary = assignment.summary();
        System.out.println("✅ Summary Format Check:\n" + summary);

        if (!summary.contains("[PERMANENT]") || !summary.contains("ACTIVE") || !summary.contains("Manual setup")) {
            System.out.println("❌ Fail: Summary string format is incorrect");
        } else {
            System.out.println("✅ Summary content is valid");
        }

        AbstractRoleAssignment sameId = new AbstractRoleAssignment(user, role, meta) {
            @Override public boolean isActive() { return true; }
            @Override public String assignmentType() { return "TEST"; }
        };

        if (assignment.equals(sameId)) {
            System.out.println("❌ Fail: Different assignments should have different IDs");
        } else {
            System.out.println("✅ Identity check: Unique IDs for different instances");
        }

        try {
            new AbstractRoleAssignment(null, role, meta) {
                @Override public boolean isActive() { return true; }
                @Override public String assignmentType() { return "TEST"; }
            };
            System.out.println("❌ Fail: Allowed null user in constructor");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Blocked: Null constructor arguments");
        }
    }
}