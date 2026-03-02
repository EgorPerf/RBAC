package org.example.rbac;

import org.example.rbac.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RBACSystemTest {

    private RBACSystem system;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        system = new RBACSystem();
    }

    @Test
    @DisplayName("Проверка инициализации менеджеров и дефолтного юзера")
    void testManagersInitialization() {
        assertNotNull(system.getUserManager());
        assertNotNull(system.getRoleManager());
        assertNotNull(system.getAssignmentManager());
        assertEquals("system", system.getCurrentUser());
    }

    @Test
    @DisplayName("Проверка сеттера и геттера currentUser")
    void testCurrentUser() {
        system.setCurrentUser("admin_user");
        assertEquals("admin_user", system.getCurrentUser());
    }

    @Test
    @DisplayName("Проверка метода initialize()")
    void testInitialize() {
        system.initialize();

        assertTrue(system.getUserManager().exists("admin"));

        assertTrue(system.getRoleManager().exists("Admin"));
        assertTrue(system.getRoleManager().exists("Manager"));
        assertTrue(system.getRoleManager().exists("Viewer"));

        var adminUser = system.getUserManager().findByUsername("admin").get();
        var adminRole = system.getRoleManager().findByName("Admin").get();

        assertTrue(system.getAssignmentManager().userHasRole(adminUser, adminRole));

        assertDoesNotThrow(() -> system.initialize());
    }

    @Test
    @DisplayName("Проверка метода generateStatistics()")
    void testGenerateStatistics() {
        system.initialize();
        String stats = system.generateStatistics();

        assertTrue(stats.contains("Users: 1"));
        assertTrue(stats.contains("Roles: 3"));
        assertTrue(stats.contains("Assignments: 1"));
    }
}