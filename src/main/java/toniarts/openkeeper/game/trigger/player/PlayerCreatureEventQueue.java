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
package toniarts.openkeeper.game.trigger.player;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/** Server input buffer; only simulation ticks drain events into player scripts. */
public final class PlayerCreatureEventQueue {

    private record Event(short playerId, TriggerGeneric.TargetType type) { }
    private final ConcurrentLinkedQueue<Event> events = new ConcurrentLinkedQueue<>();

    public void enqueuePickedUp(short playerId) {
        events.add(new Event(playerId, TriggerGeneric.TargetType.PLAYER_CREATURE_PICKED_UP));
    }

    public void enqueueDropped(short playerId) {
        events.add(new Event(playerId, TriggerGeneric.TargetType.PLAYER_CREATURE_DROPPED));
    }

    /** Called from the server simulation, before ordinary script traversal. */
    public void drain(BiConsumer<Short, TriggerGeneric.TargetType> dispatch) {
        Event event;
        while ((event = events.poll()) != null) {
            dispatch.accept(event.playerId(), event.type());
        }
    }

    public void clear() {
        events.clear();
    }
}
