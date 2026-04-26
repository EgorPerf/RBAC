package org.example.rbac;

import org.example.rbac.manager.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogTest {

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    private void waitForLogs(int expectedSize) throws InterruptedException {
        int retries = 20;
        while (auditLog.getAll().size() < expectedSize && retries > 0) {
            Thread.sleep(50);
            retries--;
        }
    }

    @Test
    void testLogAndGetAll() throws InterruptedException {
        auditLog.log("ACTION1", "user1", "target1", "details1");
        auditLog.log("ACTION2", "user2", "target2", "details2");

        waitForLogs(2);

        assertEquals(2, auditLog.getAll().size());
    }

    @Test
    void testGetByPerformer() throws InterruptedException {
        auditLog.log("ACTION1", "admin", "target1", "details1");
        auditLog.log("ACTION2", "admin", "target2", "details2");
        auditLog.log("ACTION3", "user", "target3", "details3");

        waitForLogs(3);

        assertEquals(2, auditLog.getByPerformer("admin").size());
        assertEquals(1, auditLog.getByPerformer("user").size());
    }

    @Test
    void testGetByAction() throws InterruptedException {
        auditLog.log("CREATE", "admin", "target1", "details1");
        auditLog.log("UPDATE", "admin", "target2", "details2");
        auditLog.log("CREATE", "user", "target3", "details3");

        waitForLogs(3);

        assertEquals(2, auditLog.getByAction("CREATE").size());
        assertEquals(1, auditLog.getByAction("UPDATE").size());
    }

    @Test
    void testPrintLog() throws InterruptedException {
        auditLog.log("TEST_ACTION", "test_user", "test_target", "test_details");

        waitForLogs(1);

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        auditLog.printLog();

        String output = outContent.toString();
        assertTrue(output.contains("TEST_ACTION"));
        assertTrue(output.contains("test_user"));

        System.setOut(System.out);
    }

    @Test
    void testSaveToFile() throws InterruptedException {
        auditLog.log("SAVE_ACTION", "admin", "file", "saved");

        waitForLogs(1);

        String filename = "test_audit.txt";
        auditLog.saveToFile(filename);

        File file = new File(filename);
        assertTrue(file.exists());
        file.delete();
    }
}