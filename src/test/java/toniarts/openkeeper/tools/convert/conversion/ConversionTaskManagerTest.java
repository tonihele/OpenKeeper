/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.conversion;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.AssetsConverter.ConvertProcess;
import toniarts.openkeeper.tools.convert.conversion.task.IConversionTask;
import toniarts.openkeeper.tools.convert.conversion.task.IConversionTaskUpdate;
import static org.junit.jupiter.api.Assertions.*;

class ConversionTaskManagerTest {
    private final ConversionTaskManager manager = new ConversionTaskManager();

    private ExecutorService executor() throws Exception {
        var field = ConversionTaskManager.class.getDeclaredField("executorService");
        field.setAccessible(true);
        return (ExecutorService) field.get(manager);
    }

    @AfterEach
    void stopWorkersEvenWhenRegressionFails() throws Exception {
        executor().shutdownNow();
        assertTrue(executor().awaitTermination(5, TimeUnit.SECONDS));
    }

    private void add(ConvertProcess process, CheckedTask action, boolean needed) {
        manager.addTask(process, () -> new IConversionTask() {
            @Override
            public void executeTask() throws Exception {
                action.run();
            }

            @Override
            public void addListener(IConversionTaskUpdate listener) {
            }
        }, needed);
    }

    @Test
    void assertionInPrerequisitePropagatesWithoutRunningDependent() throws Exception {
        var error = new AssertionError("Synthetic conversion failure");
        var dependentRan = new AtomicBoolean();
        add(ConvertProcess.TEXTURES, () -> { throw error; }, true);
        add(ConvertProcess.MODELS, () -> dependentRan.set(true), true);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                assertSame(error, assertThrows(AssertionError.class, manager::executeTasks)));
        assertFalse(dependentRan.get());
        assertTrue(executor().isTerminated());
    }

    @Test
    void virtualMachineErrorPropagatesEvenWhenAllTasksWereSubmitted() throws Exception {
        // Construct the error; do not exhaust the test VM's memory.
        var error = new OutOfMemoryError("Synthetic conversion failure");
        add(ConvertProcess.TEXTURES, () -> { throw error; }, true);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                assertSame(error, assertThrows(OutOfMemoryError.class, manager::executeTasks)));
        assertTrue(executor().isTerminated());
    }

    @Test
    void emptyConversionClosesExecutor() throws Exception {
        assertTrue(manager.executeTasks());
        assertTrue(executor().isShutdown());
    }

    @Test
    void exceptionInPrerequisiteReturnsFailure() throws Exception {
        var dependentRan = new AtomicBoolean();
        add(ConvertProcess.TEXTURES, () -> { throw new IllegalStateException("Synthetic failure"); }, true);
        add(ConvertProcess.MODELS, () -> dependentRan.set(true), true);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertFalse(manager.executeTasks()));
        assertFalse(dependentRan.get());
        assertTrue(executor().isTerminated());
    }

    @Test
    void successfulDependencyChainRunsInOrder() throws Exception {
        var prerequisiteRan = new AtomicBoolean();
        var dependentRan = new AtomicBoolean();
        add(ConvertProcess.TEXTURES, () -> prerequisiteRan.set(true), true);
        add(ConvertProcess.MODELS, () -> {
            assertTrue(prerequisiteRan.get());
            assertFalse(dependentRan.getAndSet(true));
        }, true);

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertTrue(manager.executeTasks()));
        assertTrue(dependentRan.get());
        assertTrue(executor().isTerminated());
    }

    @Test
    void skippedTasksCloseExecutorWithoutRunning() throws Exception {
        add(ConvertProcess.TEXTURES, () -> fail("Skipped conversion ran"), false);
        assertTrue(manager.executeTasks());
        assertTrue(executor().isShutdown());
    }

    @Test
    void interruptionCancelsWorkersAndPreservesCallerInterrupt() throws Exception {
        var started = new CountDownLatch(1);
        var workerStopped = new CountDownLatch(1);
        var callerFailed = new AtomicBoolean();
        var interruptRestored = new AtomicBoolean();
        add(ConvertProcess.TEXTURES, () -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } finally {
                workerStopped.countDown();
            }
        }, true);
        add(ConvertProcess.MODELS, () -> fail("Dependent conversion ran"), true);
        Thread caller = new Thread(() -> {
            callerFailed.set(!manager.executeTasks());
            interruptRestored.set(Thread.currentThread().isInterrupted());
        });
        caller.start();
        try {
            assertTrue(started.await(3, TimeUnit.SECONDS));
            caller.interrupt();
            caller.join(3000);
            assertFalse(caller.isAlive());
            assertTrue(callerFailed.get());
            assertTrue(interruptRestored.get());
            assertTrue(workerStopped.await(3, TimeUnit.SECONDS));
            assertTrue(executor().isShutdown());
        } finally {
            caller.interrupt();
        }
    }

    @FunctionalInterface
    private interface CheckedTask {
        void run() throws Exception;
    }
}
