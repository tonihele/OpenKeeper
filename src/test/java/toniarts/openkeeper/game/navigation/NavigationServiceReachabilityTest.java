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

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reachable areas are flood filled once a tick and shared by every start
 * in the same area
 */
class NavigationServiceReachabilityTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final EntityId DOOR = new EntityId(1);

    /**
     * Rows of land between rock: water, lava and the keeper's door in the
     * middle of them
     */
    private final char[][] map = {
        "..~..".toCharArray(),
        "#####".toCharArray(),
        "..=..".toCharArray(),
        "#####".toCharArray(),
        "..D..".toCharArray()
    };
    private final Map<Character, Terrain> terrains = Map.of(
            '.', TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0),
            'D', TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0),
            '#', TerrainFixtures.terrain((short) 2, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, (short) 0),
            '~', TerrainFixtures.terrain((short) 3, TerrainFixtures.flags(Terrain.TerrainFlag.WATER), 0, 0, 0, (short) 0),
            '=', TerrainFixtures.terrain((short) 4, TerrainFixtures.flags(Terrain.TerrainFlag.LAVA), 0, 0, 0, (short) 0));
    private final Map<Point, IMapTileInformation> tiles = new HashMap<>();
    private int terrainLookups;
    private final NavigationService navigationService = createNavigationService();

    private final INavigable walker = new Mover(KEEPER, false, false, false, false);
    private final INavigable swimmer = new Mover(KEEPER, false, true, false, false);
    private final INavigable flyer = new Mover(KEEPER, true, false, false, false);
    private final INavigable enemy = new Mover(ENEMY, false, false, false, false);
    private final INavigable doorBreaker = new Mover(ENEMY, false, false, false, true);

    @Test
    void waterLavaAndRockKeepTheMoversApart() {
        assertFalse(navigationService.isReachable(new Point(0, 0), new Point(4, 0), walker));
        assertTrue(navigationService.isReachable(new Point(0, 0), new Point(4, 0), swimmer));
        assertFalse(navigationService.isReachable(new Point(0, 2), new Point(4, 2), walker));
        assertTrue(navigationService.isReachable(new Point(0, 2), new Point(4, 2), flyer));
        assertFalse(navigationService.isReachable(new Point(0, 0), new Point(0, 2), flyer));
    }

    @Test
    void onlyTheOwnerGoesThroughTheDoor() {
        assertTrue(navigationService.isReachable(new Point(0, 4), new Point(4, 4), walker));
        assertFalse(navigationService.isReachable(new Point(0, 4), new Point(4, 4), enemy));
    }

    @Test
    void anEnemyOnTheDoorCanStepOffButNotBackOn() {
        assertTrue(navigationService.isReachable(new Point(2, 4), new Point(1, 4), enemy));
        assertTrue(navigationService.isReachable(new Point(2, 4), new Point(3, 4), enemy));
        assertFalse(navigationService.isReachable(new Point(1, 4), new Point(2, 4), enemy));
        assertFalse(navigationService.isReachable(new Point(1, 4), new Point(3, 4), enemy));
    }

    @Test
    void anEnemyThatBreaksTheDoorHasAnAreaOfItsOwn() {
        assertFalse(navigationService.isReachable(new Point(0, 4), new Point(4, 4), enemy));
        assertTrue(navigationService.isReachable(new Point(0, 4), new Point(4, 4), doorBreaker));
        assertTrue(navigationService.isReachable(new Point(4, 4), new Point(0, 4), doorBreaker));
        assertFalse(navigationService.isReachable(new Point(1, 4), new Point(4, 4), enemy));
    }

    @Test
    void everyStartInTheAreaUsesTheSameFloodFill() {
        assertTrue(navigationService.isReachable(new Point(0, 0), new Point(1, 0), walker));
        int lookups = terrainLookups;

        assertTrue(navigationService.isReachable(new Point(1, 0), new Point(0, 0), walker));
        assertFalse(navigationService.isReachable(new Point(1, 0), new Point(4, 0), walker));

        assertEquals(lookups, terrainLookups);
    }

    @Test
    void theAreasAreFilledAgainTheNextTick() {
        assertFalse(navigationService.isReachable(new Point(0, 0), new Point(4, 0), walker));

        // The water is filled in, but the area stays the same for the rest of the tick
        map[0][2] = '.';
        assertFalse(navigationService.isReachable(new Point(0, 0), new Point(4, 0), walker));

        navigationService.processTick(0.25f);

        assertTrue(navigationService.isReachable(new Point(0, 0), new Point(4, 0), walker));
    }

    private NavigationService createNavigationService() {
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth" -> map[0].length;
            case "getHeight" -> map.length;
            case "getTile" -> args.length == 1 ? getTile((Point) args[0]) : getTile(new Point((int) args[0], (int) args[1]));
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "getTerrain" -> {
                terrainLookups++;
                Point p = ((IMapTileInformation) args[0]).getLocation();
                yield terrains.get(map[p.y][p.x]);
            }
            default -> null;
        });
        IEntityPositionLookup entityPositionLookup = stub(IEntityPositionLookup.class, (name, args) -> {
            if ("getBlockingDoor".equals(name)) {
                return map[(int) args[1]][(int) args[0]] == 'D' && (short) args[2] != KEEPER ? DOOR : null;
            }
            return null;
        });

        return new NavigationService(mapController, entityPositionLookup);
    }

    private IMapTileInformation getTile(Point p) {
        if (p.x < 0 || p.y < 0 || p.y >= map.length || p.x >= map[0].length) {
            return null;
        }
        return tiles.computeIfAbsent(p, location -> stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getLocation" -> location;
            case "getX" -> location.x;
            case "getY" -> location.y;
            default -> null;
        }));
    }

    private record Mover(short getOwnerId, boolean canFly, boolean canWalkOnWater, boolean canWalkOnLava, boolean breaksDoors)
            implements INavigable {

        @Override
        public boolean canMoveDiagonally() {
            return true;
        }

        @Override
        public boolean canBreakDoor(EntityId door) {
            return breaksDoors;
        }

    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> answer.answer(method.getName(), args);
        });
    }

}
