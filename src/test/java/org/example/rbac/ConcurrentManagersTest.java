package org.example.rbac;

import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.Role;
import org.example.rbac.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConcurrentManagersTest {

    private UserManager userManager;
    private RoleManager roleManager;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
    }

    @Test
    void testUserManagerConcurrentAdd() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            service.submit(() -> {
                try {
                    User user = User.create("user" + index, "Full Name " + index, "user" + index + "@test.com");
                    userManager.add(user);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        service.shutdown();

        assertEquals(100, userManager.count(), "Количество пользователей должно быть ровно 100");
    }

    @Test
    void testRoleManagerConcurrentAdd() throws InterruptedException {
        int numberOfThreads = 50;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            service.submit(() -> {
                try {
                    Role role = new Role("Role" + index, "Description " + index, new HashSet<>());
                    roleManager.add(role);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        service.shutdown();

        assertEquals(50, roleManager.count(), "Количество ролей должно быть ровно 50");
    }
}