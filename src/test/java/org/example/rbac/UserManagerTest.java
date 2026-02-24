package org.example.rbac;

import org.example.rbac.filter.UserFilters;
import org.example.rbac.filter.UserSorters;
import org.example.rbac.manager.UserManager;
import org.example.rbac.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserManagerTest {

    private UserManager manager;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        manager = new UserManager();
        user1 = User.create("alice", "Alice Smith", "alice@example.com");
        user2 = User.create("bob", "Bob Jones", "bob@example.com");
    }

    @Test
    @DisplayName("Добавление и подсчет пользователей")
    void testAddAndCount() {
        manager.add(user1);
        assertEquals(1, manager.count());
        assertThrows(IllegalArgumentException.class, () -> manager.add(user1));
        assertThrows(IllegalArgumentException.class, () -> manager.add(null));
    }

    @Test
    @DisplayName("Удаление пользователя")
    void testRemove() {
        manager.add(user1);
        assertTrue(manager.remove(user1));
        assertFalse(manager.remove(user1));
        assertFalse(manager.remove(null));
        assertEquals(0, manager.count());
    }

    @Test
    @DisplayName("Поиск по ID и Username")
    void testFindByIdAndUsername() {
        manager.add(user1);
        assertTrue(manager.findById("alice").isPresent());
        assertEquals(user1, manager.findById("alice").get());
        assertFalse(manager.findById("bob").isPresent());

        assertTrue(manager.findByUsername("alice").isPresent());
        assertFalse(manager.findByUsername(null).isPresent());
    }

    @Test
    @DisplayName("Поиск по Email")
    void testFindByEmail() {
        manager.add(user1);
        assertTrue(manager.findByEmail("alice@example.com").isPresent());
        assertFalse(manager.findByEmail("wrong@example.com").isPresent());
        assertFalse(manager.findByEmail(null).isPresent());
    }

    @Test
    @DisplayName("Получение всех пользователей и очистка")
    void testFindAllAndClear() {
        manager.add(user1);
        manager.add(user2);
        assertEquals(2, manager.findAll().size());
        manager.clear();
        assertEquals(0, manager.count());
    }

    @Test
    @DisplayName("Поиск по фильтру")
    void testFindByFilter() {
        manager.add(user1);
        manager.add(user2);
        List<User> result = manager.findByFilter(UserFilters.byUsername("alice"));
        assertEquals(1, result.size());
        assertEquals(user1, result.get(0));

        assertEquals(2, manager.findByFilter(null).size());
    }

    @Test
    @DisplayName("Поиск с фильтром и сортировкой")
    void testFindAllWithSorter() {
        manager.add(user2);
        manager.add(user1);
        List<User> result = manager.findAll(UserFilters.byEmailDomain("@example.com"), UserSorters.byUsername());
        assertEquals(2, result.size());
        assertEquals(user1, result.get(0));
        assertEquals(user2, result.get(1));

        assertThrows(IllegalArgumentException.class, () -> manager.findAll(null, UserSorters.byUsername()));
        assertThrows(IllegalArgumentException.class, () -> manager.findAll(UserFilters.byUsername("alice"), null));
    }

    @Test
    @DisplayName("Проверка существования")
    void testExists() {
        manager.add(user1);
        assertTrue(manager.exists("alice"));
        assertFalse(manager.exists("bob"));
        assertFalse(manager.exists(null));
    }

    @Test
    @DisplayName("Обновление пользователя")
    void testUpdate() {
        manager.add(user1);
        manager.update("alice", "Alice New", "new@example.com");
        User updated = manager.findByUsername("alice").get();
        assertEquals("Alice New", updated.fullName());
        assertEquals("new@example.com", updated.email());

        assertThrows(IllegalArgumentException.class, () -> manager.update("bob", "Name", "a@a.com"));
        assertThrows(IllegalArgumentException.class, () -> manager.update(null, "Name", "a@a.com"));
    }

    @Test
    @DisplayName("Проверка equals и hashCode")
    void testEqualsAndHashCode() {
        UserManager m1 = new UserManager();
        UserManager m2 = new UserManager();

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());

        m1.add(user1);
        assertNotEquals(m1, m2);

        m2.add(user1);
        assertEquals(m1, m2);
        assertNotEquals(m1, null);
        assertNotEquals(m1, new Object());
    }
}