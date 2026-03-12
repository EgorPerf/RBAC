package org.example.rbac.util;

import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_YELLOW = "\u001B[33m";

    private ConsoleUtils() {}

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(ANSI_CYAN + message + (required ? " *: " : " (optional): ") + ANSI_RESET);
            String input = ValidationUtils.normalizeString(scanner.nextLine());
            if (required && input.isEmpty()) {
                System.out.println(ANSI_RED + "Ошибка: Это поле обязательно для заполнения." + ANSI_RESET);
            } else {
                return input;
            }
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(ANSI_CYAN + message + " [" + min + "-" + max + "]: " + ANSI_RESET);
            String input = ValidationUtils.normalizeString(scanner.nextLine());
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println(ANSI_RED + "Ошибка: Число должно быть в диапазоне от " + min + " до " + max + "." + ANSI_RESET);
                }
            } catch (NumberFormatException e) {
                System.out.println(ANSI_RED + "Ошибка: Введите корректное целое число." + ANSI_RESET);
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(ANSI_CYAN + message + " (y/n): " + ANSI_RESET);
            String input = ValidationUtils.normalizeString(scanner.nextLine()).toLowerCase();
            if (input.equals("y") || input.equals("yes") || input.equals("да") || input.equals("д") || input.equals("1")) {
                return true;
            } else if (input.equals("n") || input.equals("no") || input.equals("нет") || input.equals("н") || input.equals("0")) {
                return false;
            } else {
                System.out.println(ANSI_RED + "Ошибка: Введите 'y' (да) или 'n' (нет)." + ANSI_RESET);
            }
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("Список вариантов пуст.");
        }
        System.out.println(ANSI_YELLOW + "=== " + message + " ===" + ANSI_RESET);
        for (int i = 0; i < options.size(); i++) {
            System.out.println(ANSI_CYAN + (i + 1) + ". " + ANSI_RESET + options.get(i).toString());
        }
        int choice = promptInt(scanner, "Ваш выбор", 1, options.size());
        return options.get(choice - 1);
    }
}