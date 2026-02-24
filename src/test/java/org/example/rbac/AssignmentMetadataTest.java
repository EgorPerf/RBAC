package org.example.rbac;

import org.example.rbac.model.AssignmentMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentMetadataTest {

    @Test
    @DisplayName("Успешное создание через now()")
    void testValidNowCreation() {
        AssignmentMetadata metadata = AssignmentMetadata.now("admin_user", "Project setup");

        assertEquals("admin_user", metadata.assignedBy());
        assertEquals("Project setup", metadata.reason());
        assertNotNull(metadata.assignedAt());
        assertFalse(metadata.assignedAt().isBlank());
    }

    @Test
    @DisplayName("Использование дефолтной причины при null или пустоте")
    void testDefaultReason() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", null);
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "");
        AssignmentMetadata meta3 = AssignmentMetadata.now("admin", "   ");

        assertEquals("No reason provided", meta1.reason());
        assertEquals("No reason provided", meta2.reason());
        assertEquals("No reason provided", meta3.reason());
    }

    @Test
    @DisplayName("Исключения при невалидном assignedBy")
    void testInvalidAssignedBy() {
        assertThrows(IllegalArgumentException.class, () -> AssignmentMetadata.now(null, "Reason"));
        assertThrows(IllegalArgumentException.class, () -> AssignmentMetadata.now("", "Reason"));
        assertThrows(IllegalArgumentException.class, () -> AssignmentMetadata.now("   ", "Reason"));
    }

    @Test
    @DisplayName("Проверка метода format()")
    void testFormat() {
        AssignmentMetadata metadata = new AssignmentMetadata("system_root", "2026-02-24T12:00:00", "Initial config");
        String expected = "Assigned by: system_root at 2026-02-24T12:00:00. Reason: Initial config";

        assertEquals(expected, metadata.format());
    }
}