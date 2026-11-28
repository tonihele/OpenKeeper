/*
 * Copyright (C) 2014-2026 OpenKeeper
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

import com.simsilica.event.EventType;

/**
 * Holds server side event types, raised where the game logic learns of
 * something so that other parts don't have to look for it every tick
 */
public final class ServerEvents {

    /**
     * A creature looked for a room its keeper has none of
     */
    public static final EventType<RoomWanted> ROOM_WANTED = EventType.create("RoomWanted", RoomWanted.class);

    private ServerEvents() {
        // Just collection of events we can use with our event bus
    }

}
