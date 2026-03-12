package org.example.rbac;

import org.example.rbac.command.CommandParser;
import org.example.rbac.command.CommandRegistry;
import org.example.rbac.util.ConsoleUtils;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        RBACSystem system = new RBACSystem();
        system.initialize(); // Создаст роль Admin и пользователя admin

        CommandParser parser = new CommandParser();
        CommandRegistry.registerUserCommands(parser);
        CommandRegistry.registerRoleCommands(parser);
        CommandRegistry.registerAssignmentCommands(parser);
        CommandRegistry.registerPermissionCommands(parser);
        CommandRegistry.registerUtilityCommands(parser);

        Scanner scanner = new Scanner(System.in);

        System.out.println(ConsoleUtils.ANSI_GREEN + "==========================================" + ConsoleUtils.ANSI_RESET);
        System.out.println(ConsoleUtils.ANSI_GREEN + "      Добро пожаловать в систему RBAC!    " + ConsoleUtils.ANSI_RESET);
        System.out.println(ConsoleUtils.ANSI_GREEN + "==========================================" + ConsoleUtils.ANSI_RESET);
        System.out.println("Введите " + ConsoleUtils.ANSI_CYAN + "help" + ConsoleUtils.ANSI_RESET + " для просмотра списка команд.");

        while (true) {
            System.out.print(ConsoleUtils.ANSI_YELLOW + "\nRBAC [" + system.getCurrentUser() + "] > " + ConsoleUtils.ANSI_RESET);

            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+");
            String commandName = parts[0].toLowerCase();

            try {
                parser.executeCommand(commandName, scanner, system);
            } catch (RuntimeException e) {
                if ("EXIT_SIGNAL".equals(e.getMessage())) break;
                System.out.println(ConsoleUtils.ANSI_RED + e.getMessage() + ConsoleUtils.ANSI_RESET);
            } catch (Exception e) {
                System.out.println(ConsoleUtils.ANSI_RED + "Непредвиденная ошибка: " + e.getMessage() + ConsoleUtils.ANSI_RESET);
            }
        }
        scanner.close();
        System.out.println(ConsoleUtils.ANSI_GREEN + "Работа системы завершена. До свидания!" + ConsoleUtils.ANSI_RESET);
    }
}