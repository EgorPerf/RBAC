package org.example.rbac;

import org.example.rbac.command.Command;
import org.example.rbac.command.CommandParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private CommandParser parser;
    private RBACSystem system;
    private Scanner scanner;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        scanner = new Scanner("");
    }

    @Test
    @DisplayName("Регистрация и выполнение команды")
    void testRegisterAndExecute() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Command cmd = (s, sys) -> executed.set(true);

        parser.registerCommand("test", "Test command", cmd);
        parser.executeCommand("test", scanner, system);

        assertTrue(executed.get());
    }

    @Test
    @DisplayName("Исключения при регистрации")
    void testRegistrationExceptions() {
        Command cmd = (s, sys) -> {};
        assertThrows(IllegalArgumentException.class, () -> parser.registerCommand(null, "desc", cmd));
        assertThrows(IllegalArgumentException.class, () -> parser.registerCommand("cmd", null, cmd));
        assertThrows(IllegalArgumentException.class, () -> parser.registerCommand("cmd", "desc", null));
    }

    @Test
    @DisplayName("Парсинг и выполнение (parseAndExecute)")
    void testParseAndExecute() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Command cmd = (s, sys) -> executed.set(true);

        parser.registerCommand("run", "Run it", cmd);

        parser.parseAndExecute("run args", scanner, system);
        assertTrue(executed.get());

        executed.set(false);
        parser.parseAndExecute(" run ", scanner, system);
        assertTrue(executed.get());

        assertDoesNotThrow(() -> parser.parseAndExecute("", scanner, system));
        assertDoesNotThrow(() -> parser.parseAndExecute(null, scanner, system));
    }

    @Test
    @DisplayName("Выполнение неизвестной команды")
    void testUnknownCommand() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(outContent));
            parser.executeCommand("unknown", scanner, system);
            assertTrue(outContent.toString().contains("Unknown command: unknown"));
            assertDoesNotThrow(() -> parser.executeCommand(null, scanner, system));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Вывод справки (printHelp)")
    void testPrintHelp() {
        Command cmd = (s, sys) -> {};
        parser.registerCommand("help", "Shows help", cmd);

        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(outContent));
            parser.printHelp();

            String output = outContent.toString();
            assertTrue(output.contains("help - Shows help"));
            assertTrue(output.contains("--- Available Commands ---"));
        } finally {
            System.setOut(originalOut);
        }
    }
}