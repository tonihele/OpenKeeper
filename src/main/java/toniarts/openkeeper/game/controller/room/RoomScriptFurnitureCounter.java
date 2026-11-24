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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
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

    static int count(IRoomController controller, EntityData entityData) {
        RoomInstance room = controller.getRoomInstance();
        int count = switch (room.getRoom().getRoomId()) {
            case 3 -> 0; // Portal
            case 11 -> UNSIGNED_SHORT_MAX; // Prison: generic sentinel, not cells or inmates.
            case 6, 7, 10, 15 -> { // Library, Training Room, Workshop, Casino
                int objects = countConfiguredObjects(controller, entityData) & UNSIGNED_SHORT_MAX;
                // Native code adds one for each nonempty room, not once per player.
                yield objects == 0 ? 0 : objects + 1;
            }
            case 12 -> countConfiguredObjects(controller, entityData); // Torture Chamber
            // Temple and Combat Pit count the boundary, not the enclosed interior.
            case 13, 16 -> room.getCoordinates().size() - countInteriorTiles(room, Set.of());
            case 14 -> countInteriorTiles(room, cryptTiles(controller, entityData)); // Graveyard
            default -> room.getCoordinates().size();
        };
        // The native room field is 16-bit; PlayerRoomControl sums these wider.
        return count & UNSIGNED_SHORT_MAX;
    }

    private static int countConfiguredObjects(IRoomController controller, EntityData entityData) {
        RoomInstance room = controller.getRoomInstance();
        List<Short> configured = room.getRoom().getObjects();
        Set<Short> types = new HashSet<>(configured.subList(0, Math.min(6, configured.size())));
        types.remove((short) 0);
        int count = 0;
        for (EntityId furniture : furniture(controller)) {
            ObjectComponent object = entityData.getComponent(furniture, ObjectComponent.class);
            if (object != null && types.contains(object.objectId)
                    && liveFurnitureTile(room, entityData, furniture) != null) {
                count++;
            }
        }
        return count;
    }

    private static Set<Point> cryptTiles(IRoomController controller, EntityData entityData) {
        Set<Point> result = new HashSet<>();
        for (EntityId furniture : furniture(controller)) {
            ObjectComponent object = entityData.getComponent(furniture, ObjectComponent.class);
            if (object != null && object.objectId == GRAVEYARD_CRYPT_ID) {
                Point tile = liveFurnitureTile(controller.getRoomInstance(), entityData, furniture);
                if (tile != null) {
                    result.add(tile);
                }
            }
        }
        return result;
    }

    private static Set<EntityId> furniture(IRoomController controller) {
        Set<EntityId> furniture = new HashSet<>(controller.getFloorFurniture());
        furniture.addAll(controller.getWallFurniture());
        return furniture;
    }

    private static Point liveFurnitureTile(RoomInstance room, EntityData entityData, EntityId furniture) {
        Position position = entityData.getComponent(furniture, Position.class);
        if (position == null || entityData.getComponent(furniture, Death.class) != null) {
            return null;
        }
        // Neutral furniture counts too, provided it is still on a tile in this room.
        Point tile = WorldUtils.vectorToPoint(position.position);
        return room.hasCoordinate(tile) ? tile : null;
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
