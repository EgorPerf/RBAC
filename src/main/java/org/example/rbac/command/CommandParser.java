package org.example.rbac.command;

import org.example.rbac.RBACSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandParser {

    private final Map<String, Command> commands = new HashMap<>();
    private final Map<String, String> commandDescriptions = new HashMap<>();

    public void registerCommand(String name, String description, Command command) {
        if (name == null || description == null || command == null) {
            throw new IllegalArgumentException();
        }
        commands.put(name, command);
        commandDescriptions.put(name, description);
    }

    public void executeCommand(String commandName, Scanner scanner, RBACSystem system) {
        if (commandName == null || !commands.containsKey(commandName)) {
            System.out.println("Unknown command: " + commandName);
            return;
        }
        commands.get(commandName).execute(scanner, system);
    }

    public void printHelp() {
        System.out.println("--- Available Commands ---");
        commandDescriptions.forEach((name, desc) ->
                System.out.println(name + " - " + desc));
        System.out.println("--------------------------");
    }

    public void parseAndExecute(String input, Scanner scanner, RBACSystem system) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        String[] parts = input.trim().split("\\s+", 2);
        executeCommand(parts[0], scanner, system);
    }
}