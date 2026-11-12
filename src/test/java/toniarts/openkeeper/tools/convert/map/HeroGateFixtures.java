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
package toniarts.openkeeper.tools.convert.map;

import java.util.Collections;

/**
 * Builds hero gate {@link Room} fixtures for tests. Lives in {@code Room}'s
 * own package because its setters are {@code protected} (parser-only).
 */
public final class HeroGateFixtures {

    private HeroGateFixtures() {
    }

    /**
     * Creates a room with no furniture objects, like all the hero gate rooms
     * in the original data
     *
     * @param roomId the room ID
     * @param name the room name
     * @param tileConstruction the tile construction type
     * @return the room
     */
    public static Room heroGateRoom(short roomId, String name, Room.TileConstruction tileConstruction) {
        Room room = new Room();
        room.setRoomId(roomId);
        room.setName(name);
        room.setTileConstruction(tileConstruction);
        room.setObjects(Collections.nCopies(8, (short) 0));
        return room;
    }

}
