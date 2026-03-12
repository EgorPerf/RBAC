package org.example.rbac;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;
import org.example.rbac.report.ReportGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReportGeneratorTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private ReportGenerator reportGenerator;
    private final String testFileName = "test_report.txt";

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);
        reportGenerator = new ReportGenerator();

        User user1 = User.create("admin_user", "Admin User", "admin@test.com");
        User user2 = User.create("guest_user", "Guest User", "guest@test.com");
        User user3 = User.create("no_role_user", "No Roles", "noroles@test.com");

        userManager.add(user1);
        userManager.add(user2);
        userManager.add(user3);

        Set<Permission> adminPerms = new HashSet<>(Set.of(
                new Permission("READ", "DATA", "desc"),
                new Permission("WRITE", "DATA", "desc"),
                new Permission("EXECUTE", "SYSTEM", "desc")
        ));
        Role adminRole = new Role("Admin", "Admin desc", adminPerms);

        Set<Permission> guestPerms = new HashSet<>(Set.of(
                new Permission("READ", "DATA", "desc")
        ));
        Role guestRole = new Role("Guest", "Guest desc", guestPerms);

        roleManager.add(adminRole);
        roleManager.add(guestRole);

        AssignmentMetadata meta = AssignmentMetadata.now("sys", "init");
        assignmentManager.add(new PermanentAssignment(user1, adminRole, meta));
        assignmentManager.add(new PermanentAssignment(user2, guestRole, meta));
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Path.of(testFileName));
    }

    @Test
    @DisplayName("Генерация отчета по пользователям")
    void testGenerateUserReport() {
        String report = reportGenerator.generateUserReport(userManager, assignmentManager);

        assertTrue(report.contains("Username"));
        assertTrue(report.contains("Active Roles"));
        assertTrue(report.contains("admin_user"));
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("guest_user"));
        assertTrue(report.contains("Guest"));
        assertTrue(report.contains("no_role_user"));
        assertTrue(report.contains("None"));
    }

    @Test
    @DisplayName("Генерация отчета по ролям")
    void testGenerateRoleReport() {
        String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);

        assertTrue(report.contains("Role Name"));
        assertTrue(report.contains("Active Users Count"));
        assertTrue(report.contains("Admin"));
        assertTrue(report.contains("Guest"));
        assertTrue(report.contains("1"));
    }

    @Test
    @DisplayName("Генерация матрицы прав")
    void testGeneratePermissionMatrix() {
        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);

        assertTrue(report.contains("User \\ Resource"));
        assertTrue(report.contains("data"));
        assertTrue(report.contains("system"));

        assertTrue(report.contains("admin_user"));
        assertTrue(report.contains("READ,WRITE"));
        assertTrue(report.contains("EXECUTE"));

        assertTrue(report.contains("guest_user"));
        assertTrue(report.contains("-"));

        assertTrue(report.contains("no_role_user"));
    }

    @Test
    @DisplayName("Генерация матрицы прав без назначений")
    void testGeneratePermissionMatrixEmpty() {
        assignmentManager.clear();
        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);
        assertTrue(report.contains("No permissions assigned to any user."));
    }

    @Test
    @DisplayName("Сохранение отчета в файл")
    void testExportToFile() throws Exception {
        String testContent = "Test Report Content";
        reportGenerator.exportToFile(testContent, testFileName);

        File file = new File(testFileName);
        assertTrue(file.exists());

        String readContent = Files.readString(file.toPath());
        assertEquals(testContent, readContent);
    }
}