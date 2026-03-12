package org.example.rbac.command;

import org.example.rbac.RBACSystem;
import org.example.rbac.util.ValidationUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {

    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        ValidationUtils.requireNonEmpty(name, "Command name");
        ValidationUtils.requireNonEmpty(description, "Command description");
        if (command == null) {
            throw new IllegalArgumentException();
        }
        String normName = ValidationUtils.normalizeString(name);
        commands.put(normName, command);
        commandDescriptions.put(normName, ValidationUtils.normalizeString(description));
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        String normName = ValidationUtils.normalizeString(commandName);
        if (normName.isEmpty() || !commands.containsKey(normName)) {
            System.out.println("Unknown command: " + commandName);
            return;
        }
        commands.get(normName).execute(scanner, system);
    }

    public void printHelp() {
        System.out.println("--- Available Commands ---");
        commandDescriptions.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.println(entry.getKey() + " - " + entry.getValue()));
        System.out.println("--------------------------");
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        String normInput = ValidationUtils.normalizeString(input);
        if (normInput.isEmpty()) {
            return;
        }
        String[] parts = normInput.split("\\s+", 2);
        executeCommand(parts[0], scanner, system);
    }
}