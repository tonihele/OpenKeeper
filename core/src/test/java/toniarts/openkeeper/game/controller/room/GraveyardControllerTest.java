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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveyardControllerTest {

    private final Random random = new Random(1);

    @Test
    void bodiesLieOnlyOnTheInsideTiles() {

        // A 3x3 graveyard has one inside tile and no crypt
        assertEquals(1, buildGraveyard(3).getMaxCapacity(ObjectType.CORPSE));
    }

    @Test
    void cryptTakesAnInsideTile() {

        // A 4x4 graveyard has four inside tiles, one of them with the crypt
        assertEquals(3, buildGraveyard(4).getMaxCapacity(ObjectType.CORPSE));
    }

    @Test
    void graveyardWithoutInsideTilesHoldsNoBodies() {
        assertEquals(0, buildGraveyard(2).getMaxCapacity(ObjectType.CORPSE));
    }

    @Test
    void oneCryptForEveryTenTiles() {
        assertEquals(0, GraveyardController.getCryptCount(9));
        assertEquals(1, GraveyardController.getCryptCount(10));
        assertEquals(2, GraveyardController.getCryptCount(29));
    }

    @Test
    void cryptsStandOnlyOnTilesSurroundedByTheGraveyard() {
        boolean[][] map = room(10, 1);

        assertTrue(GraveyardController.chooseCryptTiles(map, 1, random).isEmpty());
    }

    @Test
    void cryptsAreNeverNextToEachOther() {
        for (int i = 0; i < 50; i++) {
            boolean[][] map = room(6, 7);
            List<Point> crypts = GraveyardController.chooseCryptTiles(map, 4, random);

            assertTrue(crypts.size() >= 1 && crypts.size() <= 4);
            for (Point crypt : crypts) {
                assertTrue(crypt.x > 0 && crypt.y > 0 && crypt.x < 5 && crypt.y < 6, "Crypt on the edge " + crypt);
                for (Point other : crypts) {
                    if (other != crypt) {
                        assertTrue(Math.abs(other.x - crypt.x) > 1 || Math.abs(other.y - crypt.y) > 1, "Crypts next to each other " + crypts);
                    }
                }
            }
        }
    }

    @Test
    void aSingleCentreTileTakesTheOnlyCrypt() {
        boolean[][] map = room(3, 3);

        List<Point> crypts = GraveyardController.chooseCryptTiles(map, 2, random);

        assertEquals(List.of(new Point(1, 1)), crypts);
    }

    private static boolean[][] room(int width, int height) {
        boolean[][] map = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y] = true;
            }
        }
        return map;
    }

    private static GraveyardController buildGraveyard(int size) {
        EntityData entityData = new DefaultEntityData();
        IObjectsController objectsController = (IObjectsController) Proxy.newProxyInstance(
                IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class},
                (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);

        // Graveyard objects in DK II: the four Tomb Stones
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 14, List.of((short) 31, (short) 32, (short) 33, (short) 34, (short) 0, (short) 0)));
        for (int x = 1; x <= size; x++) {
            for (int y = 1; y <= size; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }

        GraveyardController room = new GraveyardController(entityData.createEntity(), entityData, null, roomInstance, objectsController, null);
        room.construct();
        return room;
    }
}
