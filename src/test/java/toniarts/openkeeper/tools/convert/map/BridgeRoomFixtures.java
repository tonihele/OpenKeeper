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
import java.util.EnumSet;

/**
 * Builds the three bridge {@link Room}s of DK II for tests, with the values
 * from the original game data. Lives in {@code Room}'s own package because
 * its setters are {@code protected}.
 */
public final class BridgeRoomFixtures {

    public static final short WOODEN_BRIDGE_ID = 8;
    public static final short WOODEN_BRIDGE_TERRAIN_ID = 17;
    public static final short STONE_BRIDGE_ID = 17;
    public static final short STONE_BRIDGE_TERRAIN_ID = 26;
    public static final short HERO_STONE_BRIDGE_ID = 22;
    public static final short HERO_STONE_BRIDGE_TERRAIN_ID = 36;

    private BridgeRoomFixtures() {
    }

    public static Room woodenBridge() {
        return bridge(WOODEN_BRIDGE_ID, "Wooden Bridge", WOODEN_BRIDGE_TERRAIN_ID, 200,
                EnumSet.of(Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA,
                        Room.RoomFlag.BUILDABLE, Room.RoomFlag.HAS_FLAME));
    }

    public static Room stoneBridge() {
        return bridge(STONE_BRIDGE_ID, "Stone Bridge", STONE_BRIDGE_TERRAIN_ID, 500,
                EnumSet.of(Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA,
                        Room.RoomFlag.BUILDABLE, Room.RoomFlag.HAS_FLAME));
    }

    public static Room heroStoneBridge() {
        return bridge(HERO_STONE_BRIDGE_ID, "Hero Stone Bridge", HERO_STONE_BRIDGE_TERRAIN_ID, 200,
                EnumSet.of(Room.RoomFlag.PLACEABLE_ON_WATER, Room.RoomFlag.PLACEABLE_ON_LAVA,
                        Room.RoomFlag.IS_GOOD));
    }

    private static Room bridge(short roomId, String name, short terrainId, int cost,
            EnumSet<Room.RoomFlag> flags) {
        Room room = new Room();
        room.setRoomId(roomId);
        room.setName(name);
        room.setTerrainId(terrainId);
        room.setCost(cost);
        room.setFlags(flags);
        room.setTileConstruction(Room.TileConstruction.QUAD);
        room.setObjects(Collections.nCopies(8, (short) 0));
        return room;
    }
}
