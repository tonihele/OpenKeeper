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
package toniarts.openkeeper.game.trigger.creature;

import com.simsilica.es.EntityId;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * Server input buffer created before world loading. Producers may run on loading
 * or session threads; only the simulation drains events into creature scripts.
 */
public final class CreatureTriggerEventQueue {

    private record Event(EntityId entityId, TriggerGeneric.TargetType type) { }
    private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();

    public void enqueueSlapped(EntityId entityId) {
        events.add(new Event(entityId, TriggerGeneric.TargetType.CREATURE_SLAPPED));
    }

    public void enqueueStunned(EntityId entityId) {
        events.add(new Event(entityId, TriggerGeneric.TargetType.CREATURE_STUNNED));
    }

    public void enqueueImprisoned(EntityId entityId) {
        events.add(new Event(entityId, TriggerGeneric.TargetType.CREATURE_IMPRISONED));
    }

    void drain(BiConsumer<EntityId, TriggerGeneric.TargetType> dispatch) {
        Event event;
        while ((event = events.poll()) != null) {
            dispatch.accept(event.entityId(), event.type());
        }
    }

    void clear() {
        events.clear();
    }
}
