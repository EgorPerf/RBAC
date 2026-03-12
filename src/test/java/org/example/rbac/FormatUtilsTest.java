package org.example.rbac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.example.rbac.util.FormatUtils;

import static org.junit.jupiter.api.Assertions.*;

class FormatUtilsTest {

    @Test
    @DisplayName("FormatUtils: приватный конструктор")
    void testPrivateConstructor() throws Exception {
        Constructor<FormatUtils> constructor = FormatUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    @DisplayName("padRight: дополнение пробелами")
    void testPadRight() {
        assertEquals("test  ", FormatUtils.padRight("test", 6));
        assertEquals("test", FormatUtils.padRight("test", 4));
        assertEquals("test", FormatUtils.padRight("test", 2));
        assertEquals("    ", FormatUtils.padRight(null, 4));
    }

    @Test
    @DisplayName("padLeft: дополнение пробелами")
    void testPadLeft() {
        assertEquals("  test", FormatUtils.padLeft("test", 6));
        assertEquals("test", FormatUtils.padLeft("test", 4));
        assertEquals("test", FormatUtils.padLeft("test", 2));
        assertEquals("    ", FormatUtils.padLeft(null, 4));
    }

    @Test
    @DisplayName("truncate: обрезка строки")
    void testTruncate() {
        assertEquals("test", FormatUtils.truncate("test", 10));
        assertEquals("te...", FormatUtils.truncate("testing", 5));
        assertEquals("te", FormatUtils.truncate("testing", 2));
        assertEquals("", FormatUtils.truncate(null, 5));
    }

    @Test
    @DisplayName("formatHeader: форматирование заголовка")
    void testFormatHeader() {
        assertEquals("=== Title ===", FormatUtils.formatHeader("Title"));
        assertEquals("===  ===", FormatUtils.formatHeader(null));
    }

    @Test
    @DisplayName("formatBox: обрамление текста рамкой")
    void testFormatBox() {
        String text = "Hello\nWorld!";
        String box = FormatUtils.formatBox(text);
        assertTrue(box.contains("+--------+"));
        assertTrue(box.contains("| Hello  |"));
        assertTrue(box.contains("| World! |"));

        String emptyBox = FormatUtils.formatBox(null);
        assertTrue(emptyBox.contains("+--+"));
    }

    @Test
    @DisplayName("formatTable: форматирование таблицы")
    void testFormatTable() {
        String[] headers = {"H1", "Header2"};
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Val1", "V2"});
        rows.add(new String[]{"1", null});

        String table = FormatUtils.formatTable(headers, rows);
        assertTrue(table.contains("+------+---------+"));
        assertTrue(table.contains("| H1   | Header2 |"));
        assertTrue(table.contains("| Val1 | V2      |"));
        assertTrue(table.contains("| 1    |         |"));

        assertEquals("", FormatUtils.formatTable(null, rows));

        String emptyTable = FormatUtils.formatTable(headers, new ArrayList<>());
        assertTrue(emptyTable.contains("+----+---------+"));
        assertTrue(emptyTable.contains("| H1 | Header2 |"));
    }
}