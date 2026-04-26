package org.example.rbac;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.AssignmentMetadata;
import org.example.rbac.model.PermanentAssignment;
import org.example.rbac.model.Permission;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemLoadTest {

    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    @BeforeEach
    void setUp() {
        Role.clearUsedNames();
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);
    }

    @Test
    void testSystemUnderLoad() throws InterruptedException {
        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    User user = User.create("loaduser" + index, "Load User " + index, "user" + index + "@load.com");
                    userManager.add(user);

                    Role role = new Role("LoadRole" + index, "Role for " + index, new HashSet<>(List.of(
                            new Permission("EXECUTE", "task" + index, "Run task")
                    )));
                    roleManager.add(role);

                    AssignmentMetadata meta = AssignmentMetadata.now("system", "Load test");
                    assignmentManager.add(new PermanentAssignment(user, role, meta));

                    userManager.update("loaduser" + index, "Updated User " + index, "updated" + index + "@load.com");

                    userManager.findByFilterParallel(u -> u.username().contains("load"));
                    roleManager.findRolesWithPermission("EXECUTE", "task" + index);
                    assignmentManager.userHasPermission(user, "EXECUTE", "task" + index);

                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdownNow();

        assertTrue(completed);
        assertEquals(0, errorCount.get());
        assertEquals(threadCount, userManager.count());
        assertEquals(threadCount, roleManager.count());
        assertEquals(threadCount, assignmentManager.count());
    }
}