package org.example.rbac;

import org.example.rbac.manager.AssignmentManager;
import org.example.rbac.manager.AuditLog;
import org.example.rbac.manager.RoleManager;
import org.example.rbac.manager.UserManager;
import org.example.rbac.util.TaskScheduler;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerTest {

    @Test
    void testSchedulerRunsPeriodically() throws InterruptedException {
        UserManager um = new UserManager();
        RoleManager rm = new RoleManager();
        AssignmentManager am = new AssignmentManager(um, rm);
        AuditLog log = new AuditLog();
        TaskScheduler scheduler = new TaskScheduler();

        scheduler.start(um, rm, am, log, 1);

        TimeUnit.SECONDS.sleep(3);
        scheduler.stop();

        assertTrue(log.getAll().size() >= 2);
        assertTrue(log.getAll().get(0).action().equals("SYSTEM_STATS"));
    }
}