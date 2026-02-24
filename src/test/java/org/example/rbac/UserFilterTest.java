package org.example.rbac;

import org.example.rbac.filter.UserFilter;
import org.example.rbac.filter.UserFilters;
import org.example.rbac.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserFilterTest {

    @Test
    @DisplayName("Проверка фильтра byUsername")
    void testByUsername() {
        User user = User.create("test_user", "Test Name", "test@test.com");
        assertTrue(UserFilters.byUsername("test_user").test(user));
        assertFalse(UserFilters.byUsername("wrong_user").test(user));
    }

    @Test
    @DisplayName("Проверка фильтра byUsernameContains (с игнором регистра)")
    void testByUsernameContains() {
        User user = User.create("test_user", "Test Name", "test@test.com");
        assertTrue(UserFilters.byUsernameContains("ST_US").test(user));
        assertFalse(UserFilters.byUsernameContains("admin").test(user));
    }

    @Test
    @DisplayName("Проверка фильтра byEmail")
    void testByEmail() {
        User user = User.create("test_user", "Test Name", "test@test.com");
        assertTrue(UserFilters.byEmail("test@test.com").test(user));
        assertFalse(UserFilters.byEmail("wrong@test.com").test(user));
    }

    @Test
    @DisplayName("Проверка фильтра byEmailDomain")
    void testByEmailDomain() {
        User user = User.create("test_user", "Test Name", "test@company.com");
        assertTrue(UserFilters.byEmailDomain("@company.com").test(user));
        assertFalse(UserFilters.byEmailDomain("@test.com").test(user));
    }

    @Test
    @DisplayName("Проверка фильтра byFullNameContains (с игнором регистра)")
    void testByFullNameContains() {
        User user = User.create("test_user", "John Doe", "test@test.com");
        assertTrue(UserFilters.byFullNameContains("hn do").test(user));
        assertFalse(UserFilters.byFullNameContains("ivan").test(user));
    }

    @Test
    @DisplayName("Проверка комбинаций and / or")
    void testComposition() {
        User user = User.create("john_doe", "John Doe", "john@company.com");

        UserFilter f1 = UserFilters.byUsername("john_doe");
        UserFilter f2 = UserFilters.byEmailDomain("@company.com");
        UserFilter f3 = UserFilters.byUsername("wrong");

        assertTrue(f1.and(f2).test(user));
        assertFalse(f1.and(f3).test(user));
        assertTrue(f3.or(f1).test(user));
        assertFalse(f3.or(UserFilters.byEmail("a@b.com")).test(user));
    }
}