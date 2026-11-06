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

import com.simsilica.es.EntityId;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DoorPathCostTest {

    private static final short HERO = 1;
    private static final Point DOOR_TILE = new Point(2, 2);

    private IMapController mapController;
    private IEntityPositionLookup positionLookup;
    private EntityId door;
    private boolean breaksDoors;

    @BeforeEach
    void setUp() {
        Terrain floor = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getTerrain" -> floor;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        door = new EntityId(7);
        positionLookup = (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(),
                new Class<?>[]{IEntityPositionLookup.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getBlockingDoor" -> (int) args[0] == DOOR_TILE.x && (int) args[1] == DOOR_TILE.y ? door : null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    @Test
    void aDoorThatIsNotBrokenStopsThePath() {
        breaksDoors = false;

        assertNull(navigable().getCost(null, tile(DOOR_TILE.x, DOOR_TILE.y), mapController, positionLookup));
    }

    @Test
    void aDoorThatIsBrokenOnlyCostsMore() {
        breaksDoors = true;

        assertEquals(INavigable.DEFAULT_COST + INavigable.DOOR_COST,
                navigable().getCost(null, tile(DOOR_TILE.x, DOOR_TILE.y), mapController, positionLookup));
    }

    @Test
    void aTileWithoutDoorCostsTheUsual() {
        breaksDoors = true;

        assertEquals(INavigable.DEFAULT_COST, navigable().getCost(null, tile(1, 1), mapController, positionLookup));
    }

    private INavigable navigable() {
        return new INavigable() {
            @Override
            public short getOwnerId() {
                return HERO;
            }

            @Override
            public boolean canFly() {
                return false;
            }

            @Override
            public boolean canWalkOnWater() {
                return false;
            }

            @Override
            public boolean canWalkOnLava() {
                return false;
            }

            @Override
            public boolean canMoveDiagonally() {
                return false;
            }

            @Override
            public boolean canBreakDoor(EntityId door) {
                return breaksDoors;
            }
        };
    }

    private static IMapTileInformation tile(int x, int y) {
        Point location = new Point(x, y);
        return (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLocation" -> location;
            case "getX" -> x;
            case "getY" -> y;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

}
