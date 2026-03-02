package org.example.rbac;

import org.example.rbac.command.Command;
import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CommandTest {

    private RBACSystem system;

    @BeforeEach
    void setUp() {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        system = new RBACSystem(um, rm, am);
    }

    @Test
    @DisplayName("Проверка инициализации RBACSystem")
    void testRBACSystem() {
        assertNotNull(system.getUserManager());
        assertNotNull(system.getRoleManager());
        assertNotNull(system.getAssignmentManager());
    }

    @Test
    @DisplayName("Проверка контракта интерфейса Command")
    void testCommandExecution() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Scanner scanner = new Scanner("test");

        Command testCommand = (scan, sys) -> {
            assertNotNull(scan);
            assertNotNull(sys);
            assertEquals("test", scan.nextLine());
            executed.set(true);
        };

        testCommand.execute(scanner, system);
        assertTrue(executed.get());
    }
}