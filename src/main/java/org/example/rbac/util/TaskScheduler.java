package org.example.rbac.util;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.AuditLog;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RBAC-Scheduler");
        t.setDaemon(true);
        return t;
    });

    public void start(UserManager um, RoleManager rm, AssignmentManager am, AuditLog log, int intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                am.processExpirations(log);

                int users = um.count();
                int roles = rm.count();
                int assigns = am.count();

                String stats = String.format("Users: %d, Roles: %d, Assignments: %d", users, roles, assigns);
                log.log("SYSTEM_STATS", "SYSTEM", "GLOBAL", stats);

            } catch (Exception e) {
                log.log("SCHEDULER_ERROR", "SYSTEM", "INTERNAL", e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}