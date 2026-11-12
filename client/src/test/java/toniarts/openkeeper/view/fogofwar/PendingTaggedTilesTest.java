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
package toniarts.openkeeper.view.fogofwar;

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tiles tagged in the fog look tagged at once. A drag can run past the map
 * edge, and those points must not be passed on as tiles to redraw.
 */
class PendingTaggedTilesTest {

    private static final short FLOOR_ID = 1;
    private static final int MAP_SIZE = 5;

    private final List<Point> dirtyTiles = new ArrayList<>();

    private DefaultEntityData entityData;
    private FogOfWarController controller;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        Terrain floor = TerrainFixtures.terrain(FLOOR_ID, TerrainFixtures.flags(), 0, 0, 0, FLOOR_ID);
        IKwdFile kwdFile = proxy(IKwdFile.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTerrain" ->
                floor;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        controller = new FogOfWarController(entityData, kwdFile, floorMap(), Player.KEEPER1_ID,
                points -> dirtyTiles.addAll(Arrays.asList(points)), entity -> {
                });
    }

    @AfterEach
    void tearDown() {
        entityData.close();
    }

    @Test
    void tagsTheTilesInTheFog() {
        controller.markPendingTagged(List.of(new Point(2, 2), new Point(3, 2)), true);

        assertEquals(List.of(new Point(2, 2), new Point(3, 2)), dirtyTiles);
        assertTrue(controller.isPendingTagged(new Point(2, 2)));
    }

    @Test
    void leavesOutPointsBeyondTheMapEdge() {
        controller.markPendingTagged(List.of(new Point(-1, 2), new Point(4, 2), new Point(5, 2),
                new Point(4, MAP_SIZE)), true);

        assertEquals(List.of(new Point(4, 2)), dirtyTiles);
        assertFalse(controller.isPendingTagged(new Point(5, 2)));
    }

    @SuppressWarnings("unchecked")
    private static IMapDataInformation<IMapTileInformation> floorMap() {
        IMapTileInformation[][] tiles = new IMapTileInformation[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                tiles[x][y] = proxy(IMapTileInformation.class, (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrainId" ->
                        FLOOR_ID;
                    default ->
                        throw new UnsupportedOperationException(method.getName());
                });
            }
        }
        return proxy(IMapDataInformation.class, (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return switch (method.getName()) {
                case "getWidth", "getHeight" ->
                    MAP_SIZE;
                case "getTile" -> {
                    int x = (int) args[0];
                    int y = (int) args[1];
                    yield x >= 0 && y >= 0 && x < MAP_SIZE && y < MAP_SIZE ? tiles[x][y] : null;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
