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
package toniarts.openkeeper.game.controller;

import java.lang.reflect.Proxy;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.tools.convert.map.HeroGateFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropValidatorTest {

    private static final short PLAYER = Player.KEEPER1_ID;

    private static final short CLAIMED_PATH = 8;
    private static final short DUNGEON_HEART = 14;

    private static final Room HEART = HeroGateFixtures.heroGateRoom((short) 5, "Dungeon Heart", Room.TileConstruction._5_BY_5_ROTATED);
    private static final IKwdFile KWD_FILE = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
            (proxy, method, args) -> method.getName().equals("getRoomByTerrain") && (short) args[0] == DUNGEON_HEART ? HEART : null);

    private FakeMapData mapData;

    /**
     * The player's claimed land with the Dungeon Heart on the tiles from 1, 1
     * to 5, 5
     */
    @BeforeEach
    void setUp() {
        mapData = new FakeMapData(7, 7);
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 7; y++) {
                FakeMapTile tile = mapData.getTile(x, y);
                tile.terrainId = x >= 1 && x <= 5 && y >= 1 && y <= 5 ? DUNGEON_HEART : CLAIMED_PATH;
                tile.ownerId = PLAYER;
            }
        }
    }

    @Test
    void nothingIsDroppedOnADoor() {
        Point door = new Point(0, 3);

        assertTrue(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(door), door, true));
        assertTrue(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(door), door, false));
        assertFalse(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(door), new Point(0, 4), true));
    }

    @Test
    void fighterIsNotDroppedOnTheMiddleOfTheHeart() {
        assertTrue(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(), new Point(3, 3), true));
        assertTrue(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(), new Point(2, 2), true));
        assertTrue(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(), new Point(4, 3), true));
    }

    @Test
    void fighterIsDroppedOnTheRingOfTheHeart() {
        assertFalse(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(), new Point(1, 1), true));
        assertFalse(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(), new Point(3, 5), true));
        assertFalse(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(), new Point(0, 0), true));
    }

    @Test
    void workersAndObjectsAreDroppedOnTheMiddleOfTheHeart() {
        assertFalse(DropValidator.isBlocked(KWD_FILE, mapData, Set.of(), new Point(3, 3), false));
    }

}
