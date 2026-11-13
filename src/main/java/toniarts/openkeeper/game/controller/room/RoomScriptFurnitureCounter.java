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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * DKII's PLAYER_ROOM_FURNITURE counter. These native room-ID rules intentionally
 * differ from visible furniture counts and from room storage capacities.
 */
final class RoomScriptFurnitureCounter {

    private static final int UNSIGNED_SHORT_MAX = 0xffff;
    private static final short GRAVEYARD_CRYPT_ID = 75;

    private RoomScriptFurnitureCounter() {
    }

    static int count(RoomInstance room, EntityData entityData) {
        int count = switch (room.getRoom().getRoomId()) {
            case 3 -> 0; // Portal
            case 11 -> UNSIGNED_SHORT_MAX; // Prison: generic sentinel, not cells or inmates.
            case 6, 7, 10, 15 -> { // Library, Training Room, Workshop, Casino
                int objects = countConfiguredObjects(room, entityData) & UNSIGNED_SHORT_MAX;
                // Native code adds one for each nonempty room, not once per player.
                yield objects == 0 ? 0 : objects + 1;
            }
            case 12 -> countConfiguredObjects(room, entityData); // Torture Chamber
            // Temple and Combat Pit count the boundary, not the enclosed interior.
            case 13, 16 -> room.getCoordinates().size() - countInteriorTiles(room, Set.of());
            case 14 -> countInteriorTiles(room, cryptTiles(room, entityData)); // Graveyard
            default -> room.getCoordinates().size();
        };
        // The native room field is 16-bit; PlayerRoomControl sums these wider.
        return count & UNSIGNED_SHORT_MAX;
    }

    private static int countConfiguredObjects(RoomInstance room, EntityData entityData) {
        List<Short> configured = room.getRoom().getObjects();
        Set<Short> types = new HashSet<>(configured.subList(0, Math.min(6, configured.size())));
        types.remove((short) 0);
        int count = 0;
        for (short type : types) {
            EntitySet objects = objectsOfType(entityData, type);
            try {
                for (Entity object : objects) {
                    // Room membership is spatial: neutral floor/wall furniture counts too.
                    if (entityData.getComponent(object.getId(), Death.class) == null
                            && room.hasCoordinate(WorldUtils.vectorToPoint(object.get(Position.class).position))) {
                        count++;
                    }
                }
            } finally {
                objects.release();
            }
        }
        return count;
    }

    private static Set<Point> cryptTiles(RoomInstance room, EntityData entityData) {
        Set<Point> result = new HashSet<>();
        EntitySet crypts = objectsOfType(entityData, GRAVEYARD_CRYPT_ID);
        try {
            for (Entity crypt : crypts) {
                Point tile = WorldUtils.vectorToPoint(crypt.get(Position.class).position);
                if (entityData.getComponent(crypt.getId(), Death.class) == null && room.hasCoordinate(tile)) {
                    result.add(tile);
                }
            }
        } finally {
            crypts.release();
        }
        return result;
    }

    private static EntitySet objectsOfType(EntityData entityData, short type) {
        return entityData.getEntities(new FieldFilter<>(ObjectComponent.class, "objectId", type),
                ObjectComponent.class, Position.class);
    }

    private static int countInteriorTiles(RoomInstance room, Set<Point> excluded) {
        int count = 0;
        for (Point tile : room.getCoordinates()) {
            if (!excluded.contains(tile) && isInterior(room, tile)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isInterior(RoomInstance room, Point tile) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (!room.hasCoordinate(new Point(tile.x + dx, tile.y + dy))) {
                    return false;
                }
            }
        }
        return true;
    }
}
