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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.event;

import com.simsilica.es.EntityId;
import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.event.ServerEvents.CreatureEvent;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventDispatcherTest {

    private final CreatureEvent creature = new CreatureEvent(new EntityId(1));

    @Test
    void startupAndSessionEventsWaitForSimulationAndKeepMixedInsertionOrder() throws InterruptedException {
        try (var events = new ServerEventDispatcher()) {
            var delivered = new ArrayList<String>();
            var threads = new ArrayList<Thread>();
            events.enqueue(ServerEvents.CREATURE_SLAPPED, creature);
            events.subscribe(ServerEvents.CREATURE_SLAPPED, (type, event) -> {
                delivered.add("slap");
                threads.add(Thread.currentThread());
            });
            events.subscribe(ServerEvents.CREATURE_AFRAID, (type, event) -> {
                delivered.add("fear");
                threads.add(Thread.currentThread());
            });
            events.start();
            runOnSession(() -> {
                events.enqueue(ServerEvents.CREATURE_AFRAID, creature);
                events.enqueue(ServerEvents.CREATURE_SLAPPED, creature);
                events.enqueue(ServerEvents.CREATURE_SLAPPED, creature);
            });
            assertTrue(delivered.isEmpty());
            events.processTick(.25f);
            assertEquals(List.of("slap", "fear", "slap", "slap"), delivered);
            assertTrue(threads.stream().allMatch(thread -> thread == Thread.currentThread()));
            events.processTick(.25f);
            assertEquals(4, delivered.size());
        }
    }

    @Test
    void immediateAndNestedEventsFinishBeforeProducerCleanupWithoutDrainingQueuedInput() {
        try (var events = new ServerEventDispatcher()) {
            var order = new ArrayList<String>();
            var live = new AtomicReference<>("live state");
            var observed = new AtomicReference<String>();
            events.subscribe(ServerEvents.CREATURE_AFRAID, (type, event) -> {
                observed.set(live.get());
                order.add("fear");
                events.publishNow(ServerEvents.CREATURE_ANGRY, event);
                order.add("fear complete");
            });
            events.subscribe(ServerEvents.CREATURE_ANGRY, (type, event) -> order.add("anger"));
            events.subscribe(ServerEvents.CREATURE_SLAPPED, (type, event) -> order.add("slap"));
            events.start();
            events.enqueue(ServerEvents.CREATURE_SLAPPED, creature);
            events.publishNow(ServerEvents.CREATURE_AFRAID, creature);
            live.set(null);
            order.add("cleanup");
            assertEquals("live state", observed.get());
            assertEquals(List.of("fear", "anger", "fear complete", "cleanup"), order);
            events.processTick(.25f);
            assertEquals("slap", order.getLast());
        }
    }

    @Test
    void publicationRequiresExplicitStartAndRejectsOtherThreads() throws InterruptedException {
        try (var events = new ServerEventDispatcher()) {
            assertThrows(IllegalStateException.class,
                    () -> events.publishNow(ServerEvents.CREATURE_AFRAID, creature));
            assertThrows(IllegalStateException.class, () -> events.processTick(.25f));
            events.start();
            runOnSession(() -> {
                assertThrows(IllegalStateException.class,
                        () -> events.publishNow(ServerEvents.CREATURE_AFRAID, creature));
                assertThrows(IllegalStateException.class, () -> events.processTick(.25f));
                assertThrows(IllegalStateException.class, events::start);
            });
        }
    }

    @Test
    void gamesAndStaticClientBusAreIsolatedAndSubscriptionsCanBeRemoved() {
        var staticEvents = new ArrayList<CreatureEvent>();
        EventListener<CreatureEvent> staticListener = (type, event) -> staticEvents.add(event);
        EventBus.addListener(ServerEvents.CREATURE_SLAPPED, staticListener);
        try (var first = new ServerEventDispatcher(); var second = new ServerEventDispatcher()) {
            var receivedFirst = new ArrayList<CreatureEvent>();
            var receivedSecond = new ArrayList<CreatureEvent>();
            var subscription = first.subscribe(ServerEvents.CREATURE_SLAPPED,
                    (type, event) -> receivedFirst.add(event));
            second.subscribe(ServerEvents.CREATURE_SLAPPED, (type, event) -> receivedSecond.add(event));
            first.start();
            second.start();
            first.publishNow(ServerEvents.CREATURE_SLAPPED, creature);
            assertEquals(List.of(creature), receivedFirst);
            assertTrue(receivedSecond.isEmpty());
            assertTrue(staticEvents.isEmpty());
            subscription.close();
            subscription.close();
            first.publishNow(ServerEvents.CREATURE_SLAPPED, creature);
            assertEquals(1, receivedFirst.size());
            second.publishNow(ServerEvents.CREATURE_SLAPPED, creature);
            assertEquals(List.of(creature), receivedSecond);
        } finally {
            EventBus.removeListener(ServerEvents.CREATURE_SLAPPED, staticListener);
        }
    }

    @Test
    void shutdownDiscardsInputRejectsLateProducersAndIsIdempotent() throws InterruptedException {
        var delivered = new ArrayList<CreatureEvent>();
        try (var events = new ServerEventDispatcher()) {
            var subscription = events.subscribe(ServerEvents.CREATURE_SLAPPED,
                    (type, event) -> delivered.add(event));
            events.enqueue(ServerEvents.CREATURE_SLAPPED, creature);
            events.start();
            events.close();
            events.close();
            subscription.close();
            runOnSession(() -> assertFalse(events.enqueue(ServerEvents.CREATURE_SLAPPED, creature)));
            events.processTick(.25f);
            assertTrue(delivered.isEmpty());
            assertThrows(IllegalStateException.class,
                    () -> events.publishNow(ServerEvents.CREATURE_SLAPPED, creature));
            assertThrows(IllegalStateException.class,
                    () -> events.subscribe(ServerEvents.CREATURE_SLAPPED, (type, event) -> {}));
        }
    }

    @Test
    void failingListenerUsesSimsilicaPolicyAndDoesNotPreventOtherConsumers() {
        try (var events = new ServerEventDispatcher()) {
            var received = new ArrayList<CreatureEvent>();
            events.subscribe(ServerEvents.CREATURE_SLAPPED, (type, event) -> {
                throw new IllegalStateException("synthetic listener failure");
            });
            events.subscribe(ServerEvents.CREATURE_SLAPPED, (type, event) -> received.add(event));
            events.start();
            events.publishNow(ServerEvents.CREATURE_SLAPPED, creature);
            assertEquals(List.of(creature), received);
        }
    }

    private static void runOnSession(Runnable work) throws InterruptedException {
        var failure = new AtomicReference<Throwable>();
        Thread session = new Thread(() -> {
            try {
                work.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "synthetic-session");
        session.start();
        session.join();
        assertNull(failure.get(), () -> "Session failed: " + failure.get());
    }
}
