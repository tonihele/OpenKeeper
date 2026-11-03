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

/** Scripted cinematic visibility must not overwrite permanent exploration. */
class ScriptedRevealTest {

    private static final short FLOOR_ID = 1;
    private static final int MAP_SIZE = 5;

    private final List<Point> dirtyTiles = new ArrayList<>();

    private DefaultEntityData entityData;
    private FogOfWarController controller;
    private final short[][] owners = new short[MAP_SIZE][MAP_SIZE];

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        for (short[] column : owners) {
            Arrays.fill(column, Player.NEUTRAL_PLAYER_ID);
        }
        Terrain floor = TerrainFixtures.terrain(FLOOR_ID, TerrainFixtures.flags(), 0, 0, 0, FLOOR_ID);
        IKwdFile kwdFile = proxy(IKwdFile.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTerrain" ->
                floor;
            case "getThings" ->
                List.of();
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
    void endingCinematicKeepsTheStartingOwnedRoomVisible() {
        Point owned = new Point(2, 2);
        owners[owned.x][owned.y] = Player.KEEPER1_ID;
        controller.seedLevelStart();
        controller.revealActionPointTiles(List.of(owned), true);
        controller.revealActionPointTiles(List.of(owned), false);
        assertTrue(controller.isExplored(owned));
        assertTrue(controller.isVisible(owned));
    }

    @Test
    void discoveryDuringTheCinematicSurvivesItsEnd() {
        Point discovered = new Point(2, 2);
        Point unknown = new Point(3, 2);
        controller.revealActionPointTiles(List.of(discovered, unknown), true);
        assertTrue(controller.isVisible(unknown));
        owners[discovered.x][discovered.y] = Player.KEEPER1_ID;
        controller.onTileOwnerChanged(List.of(discovered));
        controller.revealActionPointTiles(List.of(discovered, unknown), false);
        assertTrue(controller.isExplored(discovered));
        assertTrue(controller.isVisible(discovered));
        assertFalse(controller.isExplored(unknown));
        assertFalse(controller.isVisible(unknown));
    }

    @Test
    void revealAndConcealRedrawValidTilesOnlyWhenTheyChange() {
        Point point = new Point(2, 2);
        List<Point> points = List.of(point, new Point(-1, 2));
        controller.revealActionPointTiles(points, true);
        controller.revealActionPointTiles(points, true);
        controller.revealActionPointTiles(points, false);
        assertEquals(List.of(point, point), dirtyTiles);
    }

    @Test
    void resetRemovesTemporaryVisibilityAndRedrawsIt() {
        Point unknown = new Point(2, 2);
        controller.revealActionPointTiles(List.of(unknown), true);
        dirtyTiles.clear();
        controller.resetToLevelStart();
        assertFalse(controller.isVisible(unknown));
        assertTrue(dirtyTiles.contains(unknown));
    }

    @SuppressWarnings("unchecked")
    private IMapDataInformation<IMapTileInformation> floorMap() {
        IMapTileInformation[][] tiles = new IMapTileInformation[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                Point location = new Point(x, y);
                tiles[x][y] = proxy(IMapTileInformation.class, (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrainId" ->
                        FLOOR_ID;
                    case "getOwnerId" ->
                        owners[location.x][location.y];
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
