package org.example.rbac;

import org.example.rbac.util.CalculationSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculationSimulatorTest {

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<CalculationSimulator> constructor = CalculationSimulator.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void testRunSimulationNormalExecution() {
        CalculationSimulator.runSimulation(2, 5, 1);

        String output = outputStreamCaptor.toString();

        assertTrue(output.contains("=== Запуск многопоточного расчёта ==="));
        assertTrue(output.contains("=== Все расчёты успешно завершены ==="));
        assertTrue(output.contains("Поток  1"));
        assertTrue(output.contains("Поток  2"));
        assertTrue(output.contains("100%"));
        assertTrue(output.contains("Завершено за"));
    }

    @Test
    void testRunSimulationInterruption() throws InterruptedException {
        ThreadGroup group = new ThreadGroup("TestGroup");
        Thread testThread = new Thread(group, () -> CalculationSimulator.runSimulation(3, 50, 100));

        testThread.start();
        Thread.sleep(50);
        group.interrupt();
        testThread.join();

        String output = outputStreamCaptor.toString();
        assertTrue(output.contains("=== Запуск многопоточного расчёта ==="));
    }
}