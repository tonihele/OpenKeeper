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
import toniarts.openkeeper.game.component.GameEvent;

/**
 * The client side of the {@link GameEvent}s. The events travel from the logic to the clients as entities, the client
 * then publishes them to the {@link EventBus} where whoever is interested (sounds, effects...) listens to them.
 */
public final class GameEventBus {

    public static final EventType<GameEvent> EVENT = EventType.create("GameEvent", GameEvent.class);

    private GameEventBus() {
        // Static
    }

    public static void publish(GameEvent event) {
        EventBus.publish(EVENT, event);
    }
}
