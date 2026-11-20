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
package toniarts.openkeeper.game.navigation.pathfinding;

import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A room tile can be without a room for a moment while the room is built or
 * rebuilt (#670)
 */
class RoomTileWithoutRoomTest {

    @Test
    void roomTileWithoutRoomIsNotAccessible() {
        Terrain roomTerrain = TerrainFixtures.terrain((short) 14, TerrainFixtures.flags(Terrain.TerrainFlag.ROOM), 0, 0, 0, (short) 0);
        IMapTileInformation tile = stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getLocation" -> new Point(5, 5);
            case "getX", "getY" -> 5;
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getTerrain" -> roomTerrain;
            default -> null; // No room controller for the tile
        });
        IEntityPositionLookup entityPositionLookup = stub(IEntityPositionLookup.class, (name, args) -> switch (name) {
            case "isTileBlocked" -> false;
            default -> null;
        });
        INavigable imp = stub(INavigable.class, (name, args) -> switch (name) {
            case "getOwnerId" -> (short) 3;
            case "canFly", "canWalkOnWater", "canWalkOnLava", "canMoveDiagonally" -> false;
            default -> null;
        });

        assertNull(MapIndexedGraph.getCost(imp, null, tile, mapController, entityPositionLookup));
    }

    private interface Answer {

        Object answer(String methodName, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

}
