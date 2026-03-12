package org.example.rbac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.example.rbac.util.ConsoleUtils;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleUtilsTest {

    private String captureOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(outContent));
            action.run();
            return outContent.toString().replaceAll("\u001B\\[[;\\d]*m", "");
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("ConsoleUtils: приватный конструктор")
    void testPrivateConstructor() throws Exception {
        Constructor<ConsoleUtils> constructor = ConsoleUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    @DisplayName("promptString: необязательное поле (пустой ввод)")
    void testPromptStringOptionalEmpty() {
        Scanner scanner = new Scanner("\n");
        String result = ConsoleUtils.promptString(scanner, "Test", false);
        assertEquals("", result);
    }

    @Test
    @DisplayName("promptString: обязательное поле с ошибкой и последующим успехом")
    void testPromptStringRequired() {
        Scanner scanner = new Scanner("\n   valid input   \n");
        String[] result = new String[1];

        String output = captureOutput(() -> {
            result[0] = ConsoleUtils.promptString(scanner, "Test Required", true);
        });

        assertEquals("valid input", result[0]);
        assertTrue(output.contains("Ошибка: Это поле обязательно для заполнения."));
    }

    @Test
    @DisplayName("promptInt: успешный ввод с первого раза")
    void testPromptIntSuccess() {
        Scanner scanner = new Scanner(" 5 \n");
        int result = ConsoleUtils.promptInt(scanner, "Enter int", 1, 10);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("promptInt: ошибки формата и диапазона, затем успех")
    void testPromptIntWithErrors() {
        Scanner scanner = new Scanner("abc\n0\n11\n 7 \n");
        int[] result = new int[1];

        String output = captureOutput(() -> {
            result[0] = ConsoleUtils.promptInt(scanner, "Enter int", 1, 10);
        });

        assertEquals(7, result[0]);
        assertTrue(output.contains("Ошибка: Введите корректное целое число."));
        assertTrue(output.contains("Ошибка: Число должно быть в диапазоне от 1 до 10."));
    }

    @Test
    @DisplayName("promptYesNo: варианты положительного ответа")
    void testPromptYesNoTrue() {
        assertTrue(ConsoleUtils.promptYesNo(new Scanner("y\n"), "Test"));
        assertTrue(ConsoleUtils.promptYesNo(new Scanner("yes\n"), "Test"));
        assertTrue(ConsoleUtils.promptYesNo(new Scanner("да\n"), "Test"));
        assertTrue(ConsoleUtils.promptYesNo(new Scanner("д\n"), "Test"));
        assertTrue(ConsoleUtils.promptYesNo(new Scanner("1\n"), "Test"));
        assertTrue(ConsoleUtils.promptYesNo(new Scanner("  Y  \n"), "Test"));
    }

    @Test
    @DisplayName("promptYesNo: варианты отрицательного ответа")
    void testPromptYesNoFalse() {
        assertFalse(ConsoleUtils.promptYesNo(new Scanner("n\n"), "Test"));
        assertFalse(ConsoleUtils.promptYesNo(new Scanner("no\n"), "Test"));
        assertFalse(ConsoleUtils.promptYesNo(new Scanner("нет\n"), "Test"));
        assertFalse(ConsoleUtils.promptYesNo(new Scanner("н\n"), "Test"));
        assertFalse(ConsoleUtils.promptYesNo(new Scanner("0\n"), "Test"));
        assertFalse(ConsoleUtils.promptYesNo(new Scanner("  No  \n"), "Test"));
    }

    @Test
    @DisplayName("promptYesNo: неверный ввод, затем правильный")
    void testPromptYesNoWithError() {
        Scanner scanner = new Scanner("maybe\nда\n");
        boolean[] result = new boolean[1];

        String output = captureOutput(() -> {
            result[0] = ConsoleUtils.promptYesNo(scanner, "Test");
        });

        assertTrue(result[0]);
        assertTrue(output.contains("Ошибка: Введите 'y' (да) или 'n' (нет)."));
    }

    @Test
    @DisplayName("promptChoice: пустой список выбрасывает исключение")
    void testPromptChoiceEmptyList() {
        Scanner scanner = new Scanner("1\n");
        assertThrows(IllegalArgumentException.class, () ->
                ConsoleUtils.promptChoice(scanner, "Choose", Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () ->
                ConsoleUtils.promptChoice(scanner, "Choose", null));
    }

    @Test
    @DisplayName("promptChoice: успешный выбор")
    void testPromptChoiceSuccess() {
        List<String> options = List.of("Option A", "Option B", "Option C");
        Scanner scanner = new Scanner("2\n");

        String[] result = new String[1];
        String output = captureOutput(() -> {
            result[0] = ConsoleUtils.promptChoice(scanner, "Make a choice", options);
        });

        assertEquals("Option B", result[0]);
        assertTrue(output.contains("=== Make a choice ==="));
        assertTrue(output.contains("1. Option A"));
        assertTrue(output.contains("2. Option B"));
        assertTrue(output.contains("3. Option C"));
    }
}