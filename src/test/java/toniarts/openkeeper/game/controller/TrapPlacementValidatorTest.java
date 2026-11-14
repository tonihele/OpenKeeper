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

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.ROOM;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;

class TrapPlacementValidatorTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;

    private static final short DIRT_PATH = 3;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final short LAIR = 30;

    /**
     * The Sentry Trap costs 1000 gold
     */
    private static final Trap SENTRY = TrapFixtures.trap((short) 3, 1000);
    private static final Point TILE = new Point(2, 2);

    private FakeMapData mapData;
    private FakeMapInformation mapInformation;

    @BeforeEach
    void setUp() {
        mapData = new FakeMapData(5, 5);
        mapInformation = new FakeMapInformation(mapData, Map.of(
                DIRT_PATH, terrain(DIRT_PATH),
                CLAIMED_PATH, terrain(CLAIMED_PATH, OWNABLE),
                REINFORCED_WALL, terrain(REINFORCED_WALL, SOLID, OWNABLE),
                LAIR, terrain(LAIR, OWNABLE, ROOM)));
        setTile(TILE, CLAIMED_PATH, PLAYER);
    }

    @Test
    void ownClaimedFloorTakesATrap() {
        assertEquals(TrapPlacementValidator.Failure.NONE, validate(TILE, 1000));
    }

    @Test
    void trapCostsItsGold() {
        assertEquals(TrapPlacementValidator.Failure.NOT_ENOUGH_GOLD, validate(TILE, 999));
    }

    @Test
    void floorOfAnotherKeeperIsNotValid() {
        setTile(TILE, CLAIMED_PATH, ENEMY);

        assertEquals(TrapPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 1000));
    }

    @Test
    void unclaimedFloorIsNotValid() {
        setTile(TILE, DIRT_PATH, Player.NEUTRAL_PLAYER_ID);

        assertEquals(TrapPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 1000));
    }

    @Test
    void wallIsNotValid() {
        setTile(TILE, REINFORCED_WALL, PLAYER);

        assertEquals(TrapPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 1000));
    }

    @Test
    void roomIsNotValid() {
        setTile(TILE, LAIR, PLAYER);

        assertEquals(TrapPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 1000));
    }

    @Test
    void tileWithATrapOrDoorIsOccupied() {
        assertEquals(TrapPlacementValidator.Failure.OCCUPIED,
                TrapPlacementValidator.validate(mapInformation, Set.of(TILE), TILE, PLAYER, SENTRY, 1000));
    }

    @Test
    void tileOutsideTheMapIsNotValid() {
        assertEquals(TrapPlacementValidator.Failure.OUT_OF_BOUNDS, validate(new Point(5, 0), 1000));
        assertEquals(TrapPlacementValidator.Failure.OUT_OF_BOUNDS, validate(new Point(-1, 2), 1000));
    }

    @Test
    void unknownTrapIsNotValid() {
        assertEquals(TrapPlacementValidator.Failure.INVALID_TRAP,
                TrapPlacementValidator.validate(mapInformation, Set.of(), TILE, PLAYER, null, 1000));
    }

    private TrapPlacementValidator.Failure validate(Point tile, int gold) {
        return TrapPlacementValidator.validate(mapInformation, Set.of(), tile, PLAYER, SENTRY, gold);
    }

    private void setTile(Point point, short terrainId, short ownerId) {
        FakeMapTile tile = mapData.getTile(point.x, point.y);
        tile.terrainId = terrainId;
        tile.ownerId = ownerId;
    }

    private static Terrain terrain(short terrainId, Terrain.TerrainFlag... flags) {
        return TerrainFixtures.terrain(terrainId, TerrainFixtures.flags(flags), 0, 1, 1, terrainId);
    }

}
