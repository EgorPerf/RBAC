package org.example.rbac;

import org.example.rbac.model.AbstractRoleAssignment;
import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class AbstractRoleAssignmentTest {

    private User user;
    private Role role;
    private AssignmentMetadata meta;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        user = User.create("test_user", "Test Name", "test@test.com");
        role = new Role("TestRole", "Desc", new HashSet<>());
        meta = AssignmentMetadata.now("admin", "Setup");
    }

    private AbstractRoleAssignment createAssignment(boolean active, String type) {
        return new AbstractRoleAssignment(user, role, meta) {
            @Override
            public boolean isActive() {
                return active;
            }

            @Override
            public String assignmentType() {
                return type;
            }
        };
    }

    @Test
    @DisplayName("Успешное создание и проверка геттеров")
    void testValidCreation() {
        AbstractRoleAssignment assignment = createAssignment(true, "MOCK");

        assertTrue(assignment.assignmentId().startsWith("asgn_"));
        assertEquals(user, assignment.user());
        assertEquals(role, assignment.role());
        assertEquals(meta, assignment.metadata());
    }

    @Test
    @DisplayName("Исключения при null аргументах конструктора")
    void testConstructorNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> new AbstractRoleAssignment(null, role, meta) {
            public boolean isActive() { return true; }
            public String assignmentType() { return "MOCK"; }
        });

        assertThrows(IllegalArgumentException.class, () -> new AbstractRoleAssignment(user, null, meta) {
            public boolean isActive() { return true; }
            public String assignmentType() { return "MOCK"; }
        });

        assertThrows(IllegalArgumentException.class, () -> new AbstractRoleAssignment(user, role, null) {
            public boolean isActive() { return true; }
            public String assignmentType() { return "MOCK"; }
        });
    }

    @Test
    @DisplayName("Проверка вывода метода summary()")
    void testSummary() {
        AbstractRoleAssignment activeAssignment = createAssignment(true, "TEST_TYPE");
        String summary = activeAssignment.summary();

        assertTrue(summary.contains("[TEST_TYPE]"));
        assertTrue(summary.contains("TestRole assigned to test_user"));
        assertTrue(summary.contains("Status: ACTIVE"));
        assertTrue(summary.contains("Reason: Setup"));

        AbstractRoleAssignment inactiveAssignment = createAssignment(false, "TEST_TYPE");
        assertTrue(inactiveAssignment.summary().contains("Status: INACTIVE"));
    }

    @Test
    @DisplayName("Проверка equals и hashCode")
    void testEqualsAndHashCode() {
        AbstractRoleAssignment asgn1 = createAssignment(true, "MOCK");
        AbstractRoleAssignment asgn2 = createAssignment(true, "MOCK");

        assertEquals(asgn1, asgn1);
        assertEquals(asgn1.hashCode(), asgn1.hashCode());
        assertNotEquals(asgn1, asgn2);
        assertNotEquals(null, asgn1);
        assertNotEquals(new Object(), asgn1);
    }
}