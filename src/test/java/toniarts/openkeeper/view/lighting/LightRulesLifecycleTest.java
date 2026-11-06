/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.lighting;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Concurrency / lifecycle stress for {@link LightRules} publication.
 * Reproduces the classic loader-vs-render race pattern and asserts the
 * READY gate keeps consumer APIs safe without external mapLoaded checks.
 */
class LightRulesLifecycleTest {

    @Test
    void concurrentLoadAndUpdateDoesNotThrow() throws Exception {
        LightRules rules = new LightRules();
        CyclicBarrier start = new CyclicBarrier(2);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread loader = new Thread(() -> {
            try {
                start.await(5, TimeUnit.SECONDS);
                for (int i = 0; i < 2_000; i++) {
                    Point tile = new Point(i % 64, (i / 64) % 64);
                    rules.addTorchLight(tile, new Vector3f(tile.x, 1f, tile.y),
                            200, 120, 40, 1.2f, 1.5f);
                    if ((i & 3) == 0) {
                        rules.clearTileLights(tile);
                    }
                    rules.setCheapLighting((i & 1) == 0);
                }
                rules.publishReady();
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                done.countDown();
            }
        }, "LightRules-loader");

        Thread consumer = new Thread(() -> {
            try {
                start.await(5, TimeUnit.SECONDS);
                for (int i = 0; i < 4_000; i++) {
                    rules.update(1f / 60f, null);
                    rules.setCheapLighting((i & 1) == 0);
                    ColorRGBA flat = rules.computeFlatLight(Vector3f.ZERO);
                    if (flat == null) {
                        fail("computeFlatLight returned null");
                    }
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                done.countDown();
            }
        }, "LightRules-consumer");

        loader.start();
        consumer.start();
        assertTrue(done.await(30, TimeUnit.SECONDS), "stress threads timed out");
        if (failure.get() != null) {
            fail(failure.get());
        }
        assertTrue(rules.isReady());
        rules.update(1f / 60f, null);
        assertTrue(rules.getSourceCount() >= 0);
    }

    @Test
    void publishReadyIsIdempotentAndDisposeWins() {
        LightRules rules = new LightRules();
        assertEquals(LightRules.Phase.LOADING, rules.getPhase());
        rules.publishReady();
        rules.publishReady();
        assertTrue(rules.isReady());
        rules.dispose();
        rules.publishReady();
        assertEquals(LightRules.Phase.DISPOSED, rules.getPhase());
        assertFalse(rules.isReady());
    }

    @Test
    void computeFlatLightSafeBeforeReady() {
        LightRules rules = new LightRules();
        ColorRGBA before = rules.computeFlatLight(new Vector3f(1f, 0f, 1f));
        assertEquals(rules.getGlobalAmbient().r, before.r, 1e-5f);
        rules.publishReady();
        ColorRGBA after = rules.computeFlatLight(new Vector3f(1f, 0f, 1f));
        assertTrue(after != null);
    }

    @Test
    void manyConsumerThreadsBeforePublishStaySafe() throws Exception {
        LightRules rules = new LightRules();
        int consumers = 4;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(consumers);
        AtomicInteger errors = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < consumers; t++) {
            Thread thread = new Thread(() -> {
                try {
                    start.await(5, TimeUnit.SECONDS);
                    for (int i = 0; i < 1_000; i++) {
                        rules.update(0.016f, null);
                        rules.computeFlatLight(Vector3f.ZERO);
                    }
                } catch (Throwable ex) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            }, "pre-ready-consumer-" + t);
            threads.add(thread);
            thread.start();
        }
        // Loader mutations while consumers hammer no-op APIs
        Thread loader = new Thread(() -> {
            try {
                start.countDown();
                for (int i = 0; i < 1_000; i++) {
                    Point tile = new Point(i % 32, i % 32);
                    rules.addTerrainLight(tile, new Vector3f(tile.x, 0.5f, tile.y), 10, 10, 10);
                }
                rules.publishReady();
            } catch (Throwable t) {
                errors.incrementAndGet();
            }
        }, "pre-ready-loader");
        loader.start();

        assertTrue(done.await(30, TimeUnit.SECONDS));
        loader.join(5_000);
        assertEquals(0, errors.get());
        assertTrue(rules.isReady());
    }

    @Test
    void repeatedPublishAndReloadPattern() {
        LightRules first = new LightRules();
        first.addTorchLight(new Point(0, 0), Vector3f.ZERO, 100, 100, 100, 1f, 1f);
        first.publishReady();
        first.update(0.016f, null);
        first.dispose();

        LightRules second = new LightRules();
        assertFalse(second.isReady());
        second.addTorchLight(new Point(1, 1), Vector3f.ZERO, 100, 100, 100, 1f, 1f);
        second.publishReady();
        second.update(0.016f, null);
        assertTrue(second.getUploadedLightCount() >= 1);
        second.dispose();
        assertEquals(LightRules.Phase.DISPOSED, second.getPhase());
    }
}
