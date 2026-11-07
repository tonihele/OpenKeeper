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
import com.simsilica.event.EventType;
import java.util.Objects;

/** Server gameplay facts, consumed independently by interested systems. */
public final class ServerEvents {

    public static final EventType<CreatureEvent> CREATURE_SLAPPED =
            EventType.create("ServerCreatureSlapped", CreatureEvent.class);
    public static final EventType<CreatureEvent> CREATURE_ANGRY =
            EventType.create("ServerCreatureAngry", CreatureEvent.class);
    public static final EventType<CreatureEvent> CREATURE_AFRAID =
            EventType.create("ServerCreatureAfraid", CreatureEvent.class);

    /** The subject of a creature event; no mutable controller or script binding. */
    public record CreatureEvent(EntityId creature) {
        public CreatureEvent {
            Objects.requireNonNull(creature);
        }
    }

    private ServerEvents() {
    }
}
