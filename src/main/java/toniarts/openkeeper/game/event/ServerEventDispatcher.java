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

import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import com.simsilica.event.EventType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;

/**
 * One game's Simsilica bus and input buffer. Create before world loading,
 * subscribe after constructing consumers, and start on the simulation thread.
 * Queued payloads must be immutable. Immediate publication is explicit because
 * some scripts must run before their producer continues or removes an entity.
 *
 * <p>Uses Simsilica's listener failure policy: log/report dispatch errors and
 * continue with other listeners. Listener order must not define gameplay order.
 * Nested immediate publications complete before the outer publication resumes.
 * This bus is server-local; it does not send presentation/network GameEvents.</p>
 */
public final class ServerEventDispatcher implements IGameLogicUpdatable, AutoCloseable {

    // EventBus's protected constructor permits independent game/test instances.
    private static final class GameBus extends EventBus {
    }

    private record Pending<E>(EventType<E> type, E event) {
        void publish(EventBus bus) {
            bus.publishEvent(type, event);
        }
    }

    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    private final EventBus bus = new GameBus();
    private final ArrayDeque<Pending<?>> pending = new ArrayDeque<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private volatile Thread simulationThread;
    private boolean closed;

    /** Register during setup, or on the simulation thread after start. */
    public synchronized <E> Subscription subscribe(EventType<E> type, EventListener<E> listener) {
        checkOpen();
        if (simulationThread != null) {
            checkSimulationThread();
        }
        Objects.requireNonNull(type);
        Objects.requireNonNull(listener);
        bus.addEventListener(type, listener);
        Subscription subscription = new Subscription() {
            private boolean removed;

            @Override
            public void close() {
                synchronized (ServerEventDispatcher.this) {
                    if (!removed) {
                        bus.removeEventListener(type, listener);
                        subscriptions.remove(this);
                        removed = true;
                    }
                }
            }
        };
        subscriptions.add(subscription);
        return subscription;
    }

    /** Keep every accepted event, in insertion order, including before start. */
    public synchronized <E> boolean enqueue(EventType<E> type, E event) {
        if (closed) {
            return false;
        }
        pending.add(new Pending<>(Objects.requireNonNull(type), Objects.requireNonNull(event)));
        return true;
    }

    /** Deliver before returning, only on the thread bound by start(). */
    public <E> void publishNow(EventType<E> type, E event) {
        checkSimulationThread();
        synchronized (this) {
            checkOpen();
        }
        bus.publishEvent(Objects.requireNonNull(type), Objects.requireNonNull(event));
    }

    @Override
    public synchronized void start() {
        checkOpen();
        if (simulationThread != null && simulationThread != Thread.currentThread()) {
            throw new IllegalStateException("Server events already belong to another simulation thread");
        }
        simulationThread = Thread.currentThread();
    }

    /** Drain at the configured simulation phase, after consumers are ready. */
    @Override
    public void processTick(float tpf) {
        checkSimulationThread();
        while (true) {
            Pending<?> event;
            synchronized (this) {
                if (closed || (event = pending.poll()) == null) {
                    return;
                }
            }
            event.publish(bus);
        }
    }

    @Override
    public void stop() {
        close();
    }

    /** Stop on the simulation thread (or during setup if never started). */
    @Override
    public synchronized void close() {
        if (simulationThread != null) {
            checkSimulationThread();
        }
        if (closed) {
            return;
        }
        closed = true;
        pending.clear();
        for (Subscription subscription : List.copyOf(subscriptions)) {
            subscription.close();
        }
    }

    private void checkSimulationThread() {
        if (simulationThread != Thread.currentThread()) {
            throw new IllegalStateException("Server event dispatch requires the simulation thread");
        }
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("Server event dispatcher is closed");
        }
    }
}
