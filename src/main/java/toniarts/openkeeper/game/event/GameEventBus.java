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
package toniarts.openkeeper.game.event;

import com.simsilica.event.EventBus;
import com.simsilica.event.EventType;
import java.util.EnumMap;
import java.util.Map;
import toniarts.openkeeper.game.component.GameEvent;

/**
 * The client side of the {@link GameEvent}s. The events travel from the logic to the clients as entities, the client
 * then publishes them to the {@link EventBus} where whoever is interested (sounds, effects...) listens to them.
 * Listen to {@link #ANY} for everything, or to {@link #of(GameEventType)} for just one kind of event.
 */
public final class GameEventBus {

    /**
     * Published for every event, in addition to its own type
     */
    public static final EventType<GameEvent> ANY = EventType.create("GameEvent", GameEvent.class);

    private static final Map<GameEventType, EventType<GameEvent>> TYPES = new EnumMap<>(GameEventType.class);

    static {
        for (GameEventType type : GameEventType.values()) {
            TYPES.put(type, EventType.create("GameEvent." + type.name(), GameEvent.class));
        }
    }

    private GameEventBus() {
        // Static
    }

    public static EventType<GameEvent> of(GameEventType type) {
        return TYPES.get(type);
    }

    public static void publish(GameEvent event) {
        EventBus.publish(of(event.type), event);
        EventBus.publish(ANY, event);
    }
}
