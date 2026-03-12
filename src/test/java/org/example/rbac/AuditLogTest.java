package org.example.rbac;

import org.example.rbac.manager.AuditLog;
import org.example.rbac.model.AuditEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    private AuditLog auditLog;
    private final String testFileName = "test_audit.txt";

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(Path.of(testFileName));
    }

    @Test
    @DisplayName("Логирование и получение всех записей")
    void testLogAndGetAll() {
        auditLog.log("CREATE", "admin", "user1", "Created user1");
        auditLog.log("DELETE", "admin", "user2", "Deleted user2");

        List<AuditEntry> entries = auditLog.getAll();
        assertEquals(2, entries.size());

        AuditEntry entry = entries.get(0);
        assertNotNull(entry.timestamp());
        assertEquals("CREATE", entry.action());
        assertEquals("admin", entry.performer());
        assertEquals("user1", entry.target());
        assertEquals("Created user1", entry.details());
    }

    @Test
    @DisplayName("Фильтрация по исполнителю")
    void testGetByPerformer() {
        auditLog.log("CREATE", "admin", "user1", "Created user1");
        auditLog.log("UPDATE", "manager", "user2", "Updated user2");
        auditLog.log("DELETE", " admin ", "user3", "Deleted user3");

        List<AuditEntry> adminEntries = auditLog.getByPerformer("admin");
        assertEquals(2, adminEntries.size());

        List<AuditEntry> managerEntries = auditLog.getByPerformer("manager");
        assertEquals(1, managerEntries.size());

        List<AuditEntry> unknownEntries = auditLog.getByPerformer("unknown");
        assertTrue(unknownEntries.isEmpty());
    }

    @Test
    @DisplayName("Фильтрация по действию")
    void testGetByAction() {
        auditLog.log("CREATE", "admin", "user1", "Created user1");
        auditLog.log("create", "manager", "user2", "Created user2");
        auditLog.log("DELETE", "admin", "user3", "Deleted user3");

        List<AuditEntry> createEntries = auditLog.getByAction("CREATE");
        assertEquals(2, createEntries.size());

        List<AuditEntry> deleteEntries = auditLog.getByAction("delete");
        assertEquals(1, deleteEntries.size());
    }

    @Test
    @DisplayName("Вывод лога в консоль")
    void testPrintLog() {
        auditLog.log("TEST_ACTION", "test_user", "test_target", "test_details");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            auditLog.printLog();
            String output = outContent.toString();
            assertTrue(output.contains("TEST_ACTION"));
            assertTrue(output.contains("test_user"));
            assertTrue(output.contains("test_target"));
            assertTrue(output.contains("test_details"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Вывод пустого лога в консоль")
    void testPrintEmptyLog() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            auditLog.printLog();
            assertTrue(outContent.toString().contains("Log is empty."));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Сохранение в файл")
    void testSaveToFile() throws Exception {
        auditLog.log("SAVE_ACTION", "admin", "file", "saving");
        auditLog.saveToFile(testFileName);

        File file = new File(testFileName);
        assertTrue(file.exists());

        String content = Files.readString(file.toPath());
        assertTrue(content.contains("SAVE_ACTION"));
        assertTrue(content.contains("admin"));
        assertTrue(content.contains("saving"));
    }
}