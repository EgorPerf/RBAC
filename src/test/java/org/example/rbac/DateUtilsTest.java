package org.example.rbac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDate;

import org.example.rbac.util.DateUtils;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @Test
    @DisplayName("DateUtils: приватный конструктор")
    void testPrivateConstructor() throws Exception {
        Constructor<DateUtils> constructor = DateUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    @DisplayName("getCurrentDate: формат YYYY-MM-DD")
    void testGetCurrentDate() {
        String date = DateUtils.getCurrentDate();
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    @DisplayName("getCurrentDateTime: формат YYYY-MM-DD HH:MM:SS")
    void testGetCurrentDateTime() {
        String dt = DateUtils.getCurrentDateTime();
        assertTrue(dt.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    @DisplayName("isBefore / isAfter: строковое сравнение")
    void testComparisons() {
        String d1 = "2023-01-01 10:00:00";
        String d2 = "2023-01-02 10:00:00";
        assertTrue(DateUtils.isBefore(d1, d2));
        assertFalse(DateUtils.isAfter(d1, d2));
        assertTrue(DateUtils.isAfter(d2, d1));
        assertFalse(DateUtils.isBefore(d2, d1));
        assertFalse(DateUtils.isBefore(null, d2));
        assertFalse(DateUtils.isAfter(null, d2));
    }

    @Test
    @DisplayName("addDays: добавление дней к дате")
    void testAddDays() {
        assertEquals("2023-01-05", DateUtils.addDays("2023-01-01", 4));
        assertEquals("2023-01-05", DateUtils.addDays("2023-01-01 15:30:00", 4));
        assertNull(DateUtils.addDays(null, 5));
        assertEquals("invalid", DateUtils.addDays("invalid", 5));
    }

    @Test
    @DisplayName("formatRelativeTime: относительное форматирование")
    void testFormatRelativeTime() {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        String future = LocalDate.now().plusDays(5).toString();
        String yesterday = LocalDate.now().minusDays(1).toString();
        String past = LocalDate.now().minusDays(3).toString();

        assertEquals("today", DateUtils.formatRelativeTime(today));
        assertEquals("in 1 day", DateUtils.formatRelativeTime(tomorrow));
        assertEquals("in 5 days", DateUtils.formatRelativeTime(future));
        assertEquals("1 day ago", DateUtils.formatRelativeTime(yesterday));
        assertEquals("3 days ago", DateUtils.formatRelativeTime(past));
        assertEquals("", DateUtils.formatRelativeTime(null));
        assertEquals("", DateUtils.formatRelativeTime("invalid"));
    }
}