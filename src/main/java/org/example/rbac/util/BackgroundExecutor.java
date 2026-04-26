package org.example.rbac.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BackgroundExecutor {

    private BackgroundExecutor() {}

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static void execute(Runnable task) {
        executor.submit(task);
    }

    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}