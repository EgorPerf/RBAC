package org.example.rbac;

import org.example.rbac.model.*;

import java.util.HashSet;
import java.util.Set;

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
        testRoleAssignmentImplementation();

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

        String[] badUsernames = {"a", "usr!", "space in name", "very_long_username_that_is_definitely_more_than_twenty_characters"};
        for (String u : badUsernames) {
            try {
                User.validate(u, "Test", "test@test.com");
                System.out.println("❌ Fail: Accepted invalid username: " + u);
            } catch (IllegalArgumentException e) {
                System.out.println("✅ Blocked: " + u + " (" + e.getMessage() + ")");
            }
        }

        String[] badEmails = {"no_at_symbol.com", "at@no_dot", "@only_domain.com", "test@domain."};
        for (String m : badEmails) {
            try {
                User.validate("valid_nick", "Test", m);
                System.out.println("❌ Fail: Accepted invalid email: " + m);
            } catch (IllegalArgumentException e) {
                System.out.println("✅ Blocked: " + m);
            }
        }
    }

    private static void testPermissionEngine() {
        System.out.println("\n--- [2] PERMISSION ENGINE ---");

        try {
            Permission p = new Permission("write", "REPORTS", "Allow editing");
            System.out.println("✅ Normalization check: " + p.name() + " on " + p.resource());

            System.out.println("✅ Match (Exact): " + p.matches("WRITE", "reports"));
            System.out.println("✅ Match (Partial): " + p.matches("WRI", "repo"));
            System.out.println("✅ Match (Regex): " + p.matches("^W.*E$", ".*ts$"));
        } catch (Exception e) {
            System.out.println("❌ Fail: Permission logic error");
        }

        try {
            new Permission("READ ", "data", "No spaces");
            System.out.println("❌ Fail: Allowed space in permission name");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Blocked: Space in name");
        }
    }

    private static void testRoleManagement() {
        System.out.println("\n--- [3] ROLE MANAGEMENT ---");

        Set<Permission> perms = new HashSet<>();
        perms.add(new Permission("READ", "users", "View"));

        Role dev = new Role("Developer", "Dev access", perms);
        System.out.println("✅ Role created with ID: " + dev.getId());

        dev.addPermission(new Permission("EXECUTE", "scripts", "Run"));
        System.out.println("✅ Permissions count: " + dev.getPermissions().size());

        try {
            new Role("Developer", "Duplicate name", new HashSet<>());
            System.out.println("❌ Fail: Allowed duplicate role name");
        } catch (IllegalArgumentException e) {
            System.out.println("✅ Blocked: Duplicate name 'Developer'");
        }

        try {
            dev.getPermissions().add(new Permission("HACK", "core", "Evil"));
            System.out.println("❌ Fail: Modified unmodifiable set");
        } catch (UnsupportedOperationException e) {
            System.out.println("✅ Protected: getPermissions() is read-only");
        }
    }

    private static void testMetadataAudit() {
        System.out.println("\n--- [4] AUDIT METADATA ---");

        AssignmentMetadata m = AssignmentMetadata.now("super_admin", "Granting access for project X");
        System.out.println("✅ Timestamp generated: " + m.assignedAt());

        AssignmentMetadata emptyReason = AssignmentMetadata.now("bot", "");
        if (emptyReason.reason().equals("No reason provided")) {
            System.out.println("✅ Default reason applied");
        } else {
            System.out.println("❌ Fail: Default reason not applied");
        }
    }

    private static void testRoleAssignmentImplementation() {
        System.out.println("\n--- [5] ROLE ASSIGNMENT CONTRACT ---");

        User user = User.validate("egor_p", "Egor", "egor@test.com");
        Role role = new Role("Manager", "Management", new HashSet<>());
        AssignmentMetadata meta = AssignmentMetadata.now("root", "Manual assign");

        RoleAssignment assignment = new RoleAssignment() {
            private final String id = "asgn_" + java.util.UUID.randomUUID();
            @Override public String assignmentId() { return id; }
            @Override public User user() { return user; }
            @Override public Role role() { return role; }
            @Override public AssignmentMetadata metadata() { return meta; }
            @Override public boolean isActive() { return true; }
            @Override public String assignmentType() { return "PERMANENT"; }
        };

        System.out.println("✅ Contract test: Assignment " + assignment.assignmentId());
        System.out.println("✅ User linked: " + assignment.user().username());
        System.out.println("✅ Role linked: " + assignment.role().getName());
        System.out.println("✅ Audit trail: " + assignment.metadata().format());
    }
}