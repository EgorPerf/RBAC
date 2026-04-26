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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelExecutionTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();

        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        for (int i = 0; i < 1000; i++) {
            userManager.add(User.create("user" + i, "Name " + i, "user" + i + "@test.com"));
        }

        Role admin = new Role("Admin", "Admin Role", new HashSet<>(List.of(new Permission("ALL", "system", "All access"))));
        Role user = new Role("User", "User Role", new HashSet<>(List.of(new Permission("READ", "data", "Read access"))));

        roleManager.add(admin);
        roleManager.add(user);

        User firstUser = userManager.findByUsername("user0").get();
        AssignmentMetadata meta = AssignmentMetadata.now("system", "Test setup");
        assignmentManager.add(new PermanentAssignment(firstUser, admin, meta));
    }

    @Test
    void testFindByFilterParallelReturnsSameAsSequential() {
        List<User> sequentialResult = userManager.findByFilter(u -> u.username().endsWith("0"));
        List<User> parallelResult = userManager.findByFilterParallel(u -> u.username().endsWith("0"));

        assertEquals(100, sequentialResult.size());
        assertEquals(sequentialResult.size(), parallelResult.size());
        assertTrue(parallelResult.containsAll(sequentialResult));
    }

    @Test
    void testParallelReportGenerationDoesNotLoseData() {
        ReportGenerator generator = new ReportGenerator();

        String userReport = generator.generateUserReport(userManager, assignmentManager);

        long actualDataRows = userReport.lines().filter(line -> line.contains("user") && line.contains("@test.com")).count();

        assertEquals(1000, actualDataRows);
    }

    @Test
    void testParallelMatrixGenerationDoesNotCrash() {
        ReportGenerator generator = new ReportGenerator();

        String matrixReport = generator.generatePermissionMatrix(userManager, assignmentManager);

        assertTrue(matrixReport.length() > 0);
        assertTrue(matrixReport.contains("User \\ Resource"));
    }
}