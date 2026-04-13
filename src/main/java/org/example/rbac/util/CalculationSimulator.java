package org.example.rbac.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;

public class CalculationSimulator {

    private CalculationSimulator() {}

    public static void runSimulation(int numThreads, int totalSteps, int delayMs) {
        AtomicIntegerArray progress = new AtomicIntegerArray(numThreads);
        AtomicLongArray threadIds = new AtomicLongArray(numThreads);
        AtomicLongArray executionTimes = new AtomicLongArray(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        System.out.println(ConsoleUtils.ANSI_YELLOW + "=== Запуск многопоточного расчёта ===" + ConsoleUtils.ANSI_RESET);
        System.out.print("\n".repeat(numThreads));

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            Thread worker = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                threadIds.set(threadIndex, Thread.currentThread().threadId());

                for (int step = 1; step <= totalSteps; step++) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    progress.set(threadIndex, step);
                }

                executionTimes.set(threadIndex, System.currentTimeMillis() - startTime);
                latch.countDown();
            });
            worker.start();
        }

        boolean allDone = false;
        while (!allDone) {
            System.out.print("\033[" + numThreads + "A");
            allDone = true;

            for (int i = 0; i < numThreads; i++) {
                int currentStep = progress.get(i);
                long tId = threadIds.get(i);
                long eTime = executionTimes.get(i);

                int percent = (int) ((currentStep * 100.0) / totalSteps);
                int filledBars = 20 * currentStep / totalSteps;
                String bar = "█".repeat(filledBars) + "-".repeat(20 - filledBars);

                if (currentStep < totalSteps) {
                    System.out.printf("\033[2K\rПоток %2d (ID: %4d) [%s] %3d%%\n", (i + 1), tId, bar, percent);
                    allDone = false;
                } else {
                    System.out.printf("\033[2K\rПоток %2d (ID: %4d) [%s] 100%% | %sЗавершено за %d мс%s\n",
                            (i + 1), tId, bar, ConsoleUtils.ANSI_GREEN, eTime, ConsoleUtils.ANSI_RESET);
                }
            }

            if (!allDone) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
            }
        }
        System.out.println(ConsoleUtils.ANSI_YELLOW + "=== Все расчёты успешно завершены ===" + ConsoleUtils.ANSI_RESET);
    }
}