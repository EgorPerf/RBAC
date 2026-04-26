package org.example.rbac;

import org.example.rbac.util.BackgroundExecutor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundExecutorTest {

    @Test
    void testTaskExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executedInDifferentThread = new AtomicBoolean(false);
        Thread mainThread = Thread.currentThread();

        BackgroundExecutor.execute(() -> {
            if (Thread.currentThread() != mainThread) {
                executedInDifferentThread.set(true);
            }
            latch.countDown();
        });

        boolean completed = latch.await(2, TimeUnit.SECONDS);

        assertTrue(completed);
        assertTrue(executedInDifferentThread.get());
    }
}