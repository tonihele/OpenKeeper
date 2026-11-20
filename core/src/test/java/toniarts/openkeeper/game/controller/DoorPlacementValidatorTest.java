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
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.ROOM;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;

class DoorPlacementValidatorTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;

    private static final short ROCK = 1;
    private static final short DIRT_PATH = 3;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final short LAIR = 30;

    /**
     * The Wooden Door costs 250 gold
     */
    private static final Door WOODEN_DOOR = DoorFixtures.door((short) 1, 250);
    private static final Door BARRICADE = DoorFixtures.door((short) 5, 100, Door.DoorFlag.IS_BARRICADE);
    private static final Point TILE = new Point(2, 2);

    private FakeMapData mapData;
    private FakeMapInformation mapInformation;

    @BeforeEach
    void setUp() {
        mapData = new FakeMapData(5, 5);
        mapInformation = new FakeMapInformation(mapData, Map.of(
                ROCK, terrain(ROCK, SOLID),
                DIRT_PATH, terrain(DIRT_PATH),
                CLAIMED_PATH, terrain(CLAIMED_PATH, OWNABLE),
                REINFORCED_WALL, terrain(REINFORCED_WALL, SOLID, OWNABLE),
                LAIR, terrain(LAIR, OWNABLE, ROOM)));
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                setTile(new Point(x, y), CLAIMED_PATH, PLAYER);
            }
        }

        // A corridor from west to east
        setTile(new Point(2, 1), REINFORCED_WALL, PLAYER);
        setTile(new Point(2, 3), REINFORCED_WALL, PLAYER);
    }

    @Test
    void doorGoesBetweenWallsNorthAndSouth() {
        assertEquals(DoorPlacementValidator.Failure.NONE, validate(TILE, 250));
    }

    @Test
    void doorGoesBetweenWallsEastAndWest() {
        setTile(new Point(2, 1), CLAIMED_PATH, PLAYER);
        setTile(new Point(2, 3), CLAIMED_PATH, PLAYER);
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);

        assertEquals(DoorPlacementValidator.Failure.NONE, validate(TILE, 250));
    }

    @Test
    void anySolidTileIsAWall() {
        setTile(new Point(2, 1), REINFORCED_WALL, ENEMY);
        setTile(new Point(2, 3), ROCK, Player.NEUTRAL_PLAYER_ID);

        assertEquals(DoorPlacementValidator.Failure.NONE, validate(TILE, 250));
    }

    @Test
    void wallOnOneSideIsNotEnough() {
        setTile(new Point(2, 3), CLAIMED_PATH, PLAYER);

        assertEquals(DoorPlacementValidator.Failure.NOT_BETWEEN_WALLS, validate(TILE, 250));
    }

    @Test
    void wallsAroundACornerAreNotEnough() {
        setTile(new Point(2, 3), CLAIMED_PATH, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);

        assertEquals(DoorPlacementValidator.Failure.NOT_BETWEEN_WALLS, validate(TILE, 250));
    }

    @Test
    void barricadeNeedsNoWalls() {
        setTile(new Point(2, 1), CLAIMED_PATH, PLAYER);
        setTile(new Point(2, 3), CLAIMED_PATH, PLAYER);

        assertEquals(DoorPlacementValidator.Failure.NONE,
                DoorPlacementValidator.validate(mapInformation, Set.of(), TILE, PLAYER, BARRICADE, 100));
    }

    @Test
    void doorCostsItsGold() {
        assertEquals(DoorPlacementValidator.Failure.NOT_ENOUGH_GOLD, validate(TILE, 249));
    }

    @Test
    void floorOfAnotherKeeperIsNotValid() {
        setTile(TILE, CLAIMED_PATH, ENEMY);

        assertEquals(DoorPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 250));
    }

    @Test
    void unclaimedFloorIsNotValid() {
        setTile(TILE, DIRT_PATH, Player.NEUTRAL_PLAYER_ID);

        assertEquals(DoorPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 250));
    }

    @Test
    void wallIsNotValid() {
        setTile(TILE, REINFORCED_WALL, PLAYER);

        assertEquals(DoorPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 250));
    }

    @Test
    void roomIsNotValid() {
        setTile(TILE, LAIR, PLAYER);

        assertEquals(DoorPlacementValidator.Failure.INVALID_TERRAIN, validate(TILE, 250));
    }

    @Test
    void tileWithATrapOrDoorIsOccupied() {
        assertEquals(DoorPlacementValidator.Failure.OCCUPIED,
                DoorPlacementValidator.validate(mapInformation, Set.of(TILE), TILE, PLAYER, WOODEN_DOOR, 250));
    }

    @Test
    void tileOutsideTheMapIsNotValid() {
        assertEquals(DoorPlacementValidator.Failure.OUT_OF_BOUNDS, validate(new Point(5, 0), 250));
        assertEquals(DoorPlacementValidator.Failure.OUT_OF_BOUNDS, validate(new Point(-1, 2), 250));
    }

    @Test
    void doorAtTheEdgeOfTheMapHasNoWallOutside() {
        setTile(new Point(1, 0), REINFORCED_WALL, PLAYER);

        assertEquals(DoorPlacementValidator.Failure.NOT_BETWEEN_WALLS, validate(new Point(0, 0), 250));
    }

    @Test
    void unknownDoorIsNotValid() {
        assertEquals(DoorPlacementValidator.Failure.INVALID_DOOR,
                DoorPlacementValidator.validate(mapInformation, Set.of(), TILE, PLAYER, null, 250));
    }

    private DoorPlacementValidator.Failure validate(Point tile, int gold) {
        return DoorPlacementValidator.validate(mapInformation, Set.of(), tile, PLAYER, WOODEN_DOOR, gold);
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
