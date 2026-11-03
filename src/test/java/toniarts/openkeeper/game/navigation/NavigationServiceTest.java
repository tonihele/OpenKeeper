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
package toniarts.openkeeper.game.navigation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import com.simsilica.es.EntityId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigationServiceTest {

    private static final short KEEPER = 3;
    private static final Point START = new Point(15, 15);

    @Test
    void idleWalkGoesFourToSevenTilesOnTheKeepersLand() {
        NavigationService navigation = navigation(p -> KEEPER, List.of());
        Set<Integer> distances = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            distances.add(distance(navigation.findIdleWalkTile(START, keepersCreature())));
        }

        assertEquals(Set.of(4, 5, 6, 7), distances);
    }

    @Test
    void idleWalkTakesASmallerRingWhenTheKeeperHasNoLandFurtherOut() {
        NavigationService navigation = navigation(p -> distance(p) <= 2 ? KEEPER : 0, List.of());
        for (int i = 0; i < 50; i++) {
            assertEquals(2, distance(navigation.findIdleWalkTile(START, keepersCreature())));
        }
    }

    @Test
    void oneIdleWalkInEightGoesToTheKeepersOldestRoom() {
        Point oldest = new Point(28, 28);
        List<IRoomController> rooms = List.of(room(9, 1, new Point(1, 1)), room(5, 6, oldest), room(2, 3, new Point(28, 1)));
        NavigationService navigation = navigation(p -> KEEPER, rooms);
        int toTheRoom = 0;
        for (int i = 0; i < 800; i++) {
            Point p = navigation.findIdleWalkTile(START, keepersCreature());
            if (p.equals(oldest)) {
                toTheRoom++;
            } else {
                assertTrue(distance(p) >= 4 && distance(p) <= 7, p::toString);
            }
        }

        // The Portal is older but isn't walked to, about 100 of the 800 walks go to the Library
        assertTrue(toTheRoom > 50 && toTheRoom < 150, "walks to the room: " + toTheRoom);
    }

    private static IRoomController room(long entityId, int roomId, Point tile) {
        RoomInstance instance = new RoomInstance(RoomFixtures.room((short) roomId, 0));
        instance.addCoordinate(tile);
        return proxy(IRoomController.class, (object, method, args) -> switch (method.getName()) {
            case "getOwnerId" -> KEEPER;
            case "getRoomId" -> (short) roomId;
            case "isDestroyed" -> false;
            case "getEntityId" -> new EntityId(entityId);
            case "getRoomInstance" -> instance;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static int distance(Point p) {
        return Math.max(Math.abs(p.x - START.x), Math.abs(p.y - START.y));
    }

    private static NavigationService navigation(ToIntFunction<Point> owners, List<IRoomController> rooms) {
        Terrain floor = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        IMapDataInformation<?> map = proxy(IMapDataInformation.class, (object, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" -> 31;
            case "getTile" -> args.length == 1 ? tile((Point) args[0], owners) : tile(new Point((int) args[0], (int) args[1]), owners);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IMapController mapController = proxy(IMapController.class, (object, method, args) -> switch (method.getName()) {
            case "getMapData" -> map;
            case "getTerrain" -> floor;
            case "getRoomControllerByCoordinates" -> null;
            case "getRoomControllers" -> rooms;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        // Nothing stands in the way
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class,
                (object, method, args) -> method.getReturnType() == boolean.class ? false : null);
        return new NavigationService(mapController, lookup);
    }

    private static IMapTileInformation tile(Point p, ToIntFunction<Point> owners) {
        if (p.x < 0 || p.y < 0 || p.x > 30 || p.y > 30) {
            return null;
        }
        return proxy(IMapTileInformation.class, (object, method, args) -> switch (method.getName()) {
            case "getOwnerId" -> (short) owners.applyAsInt(p);
            case "getLocation" -> p;
            case "getX" -> p.x;
            case "getY" -> p.y;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static INavigable keepersCreature() {
        return proxy(INavigable.class, (object, method, args) -> switch (method.getName()) {
            case "getOwnerId" -> KEEPER;
            case "canFly", "canWalkOnWater", "canWalkOnLava", "canMoveDiagonally" -> false;
            default -> InvocationHandler.invokeDefault(object, method, args);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
