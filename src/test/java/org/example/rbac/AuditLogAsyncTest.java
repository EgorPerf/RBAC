package org.example.rbac;

import org.example.rbac.manager.AuditLog;
import org.example.rbac.model.AuditEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLogAsyncTest {

    @Test
    void testAsyncLogging() throws InterruptedException {
        AuditLog auditLog = new AuditLog();

        auditLog.log("TEST_ACTION", "admin", "user1", "details");

        int retries = 10;
        while (auditLog.getAll().isEmpty() && retries > 0) {
            Thread.sleep(50);
            retries--;
        }

        List<AuditEntry> entries = auditLog.getAll();
        assertEquals(1, entries.size());
        assertEquals("TEST_ACTION", entries.get(0).action());
    }

    @Test
    void testMultipleLogsProcessed() throws InterruptedException {
        AuditLog auditLog = new AuditLog();

        for (int i = 0; i < 50; i++) {
            auditLog.log("ACTION_" + i, "admin", "target", "details");
        }

        int retries = 20;
        while (auditLog.getAll().size() < 50 && retries > 0) {
            Thread.sleep(50);
            retries--;
        }

        assertEquals(50, auditLog.getAll().size());
    }
}