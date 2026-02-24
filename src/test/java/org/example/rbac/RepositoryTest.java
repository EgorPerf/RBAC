package org.example.rbac;

import org.example.rbac.manager.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    @Test
    @DisplayName("Проверка контракта интерфейса Repository")
    void testRepositoryContract() {
        Repository<String> mockRepo = new Repository<>() {
            @Override
            public void add(String item) {}

            @Override
            public boolean remove(String item) {
                return true;
            }

            @Override
            public Optional<String> findById(String id) {
                return "1".equals(id) ? Optional.of("Item1") : Optional.empty();
            }

            @Override
            public List<String> findAll() {
                return Collections.emptyList();
            }

            @Override
            public int count() {
                return 0;
            }

            @Override
            public void clear() {}
        };

        assertDoesNotThrow(() -> mockRepo.add("Test"));
        assertTrue(mockRepo.remove("Test"));
        assertTrue(mockRepo.findById("1").isPresent());
        assertEquals("Item1", mockRepo.findById("1").get());
        assertFalse(mockRepo.findById("2").isPresent());
        assertTrue(mockRepo.findAll().isEmpty());
        assertEquals(0, mockRepo.count());
        assertDoesNotThrow(mockRepo::clear);
    }
}