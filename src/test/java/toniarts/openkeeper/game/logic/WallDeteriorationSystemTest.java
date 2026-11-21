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
package toniarts.openkeeper.game.logic;

import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WallDeteriorationSystemTest {

    private static final short FLOOR = 8;
    private static final short WALL = 9;
    private static final short ROCK = 2;
    private static final short GOLD = 6;
    private static final short IMPENETRABLE = 1;

    private final FakeMapData mapData = new FakeMapData(7, 7);
    private final Map<Short, Terrain> terrains = new HashMap<>();
    private final Map<Point, Integer> damageDone = new HashMap<>();
    private final ScriptedRandom random = new ScriptedRandom();
    private final List<IPlayerController> players = new ArrayList<>();

    @BeforeEach
    void setUp() {
        terrains.put(FLOOR, TerrainFixtures.terrain(FLOOR, TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE), 0, 660, 660, FLOOR));
        terrains.put(WALL, TerrainFixtures.terrain(WALL, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.OWNABLE), 0, 900, 900, WALL));
        terrains.put(ROCK, TerrainFixtures.terrain(ROCK, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 165, 660, WALL));
        terrains.put(GOLD, TerrainFixtures.terrain(GOLD, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 3000, 660, 660, GOLD));
        terrains.put(IMPENETRABLE, TerrainFixtures.terrain(IMPENETRABLE, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.IMPENETRABLE), 0, 1, 1, IMPENETRABLE));
        for (FakeMapTile tile : mapData) {
            tile.terrainId = ROCK;
        }
        players.add(player(Player.GOOD_PLAYER_ID));
        players.add(player(Player.NEUTRAL_PLAYER_ID));
        players.add(player(Player.KEEPER1_ID));
    }

    @Test
    void ownWallNextToOwnLandTakesTheDamage() {
        setTile(new Point(3, 2), WALL, Player.KEEPER1_ID);
        setTile(new Point(3, 3), FLOOR, Player.KEEPER1_ID);
        random.values.add(2); // x = 3
        random.values.add(1); // y = 2

        system(1, -10).processTick(0.25f);

        assertEquals(Map.of(new Point(3, 2), 10), damageDone);

        // Only the keepers pick, and never on the map edge
        assertEquals(List.of(5, 5), random.bounds);
    }

    @Test
    void otherSolidTilesAreLeftAlone() {

        // A wall of another keeper next to our land, our wall touching our land
        // only at a corner, and our own gold and impenetrable rock next to our land
        setTile(new Point(3, 3), FLOOR, Player.KEEPER1_ID);
        setTile(new Point(3, 2), WALL, Player.KEEPER2_ID);
        setTile(new Point(2, 2), WALL, Player.KEEPER1_ID);
        setTile(new Point(4, 3), GOLD, Player.KEEPER1_ID);
        setTile(new Point(3, 4), IMPENETRABLE, Player.KEEPER1_ID);
        random.values.addAll(List.of(2, 1, 1, 1, 3, 2, 2, 3));

        system(4, -10).processTick(0.25f);

        assertTrue(damageDone.isEmpty());
        assertEquals(8, random.bounds.size());
    }

    @Test
    void wallNextToAnotherKeepersLandIsLeftAlone() {
        setTile(new Point(3, 2), WALL, Player.KEEPER1_ID);
        setTile(new Point(3, 3), FLOOR, Player.KEEPER2_ID);
        random.values.add(2);
        random.values.add(1);

        system(1, -10).processTick(0.25f);

        assertTrue(damageDone.isEmpty());
    }

    @Test
    void everyKeeperPicksItsOwnTiles() {
        players.add(player(Player.KEEPER2_ID));
        setTile(new Point(3, 2), WALL, Player.KEEPER2_ID);
        setTile(new Point(3, 3), FLOOR, Player.KEEPER2_ID);

        // Keeper 1 picks first and misses with both, keeper 2 hits with its second
        random.values.addAll(List.of(2, 1, 2, 1, 0, 0, 2, 1));

        system(2, -10).processTick(0.25f);

        assertEquals(Map.of(new Point(3, 2), 10), damageDone);
        assertEquals(8, random.bounds.size());
    }

    @Test
    void offWhenTheVariablesDoNotDamage() {
        setTile(new Point(3, 2), WALL, Player.KEEPER1_ID);
        setTile(new Point(3, 3), FLOOR, Player.KEEPER1_ID);

        system(0, -10).processTick(0.25f);
        system(1, 0).processTick(0.25f);

        assertTrue(damageDone.isEmpty());
        assertTrue(random.bounds.isEmpty());
    }

    private WallDeteriorationSystem system(int tilesPerTick, int healthChange) {
        return new WallDeteriorationSystem(mapController(), players, tilesPerTick, healthChange, random);
    }

    private void setTile(Point p, short terrainId, short ownerId) {
        FakeMapTile tile = mapData.getTile(p);
        tile.terrainId = terrainId;
        tile.ownerId = ownerId;
    }

    private static IPlayerController player(short playerId) {
        Keeper keeper = new Keeper(false, playerId);
        return (IPlayerController) Proxy.newProxyInstance(WallDeteriorationSystemTest.class.getClassLoader(),
                new Class<?>[]{IPlayerController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getKeeper")) {
                        return keeper;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private IMapController mapController() {
        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapData":
                            return mapData;
                        case "getTerrain":
                            return terrains.get(((IMapTileInformation) args[0]).getTerrainId());
                        case "applyTerrainDamage":
                            damageDone.merge((Point) args[0], (Integer) args[1], Integer::sum);
                            return null;
                        default:
                            throw new UnsupportedOperationException(method.getName());
                    }
                });
    }

    /**
     * Hands out scripted values (zero when out of them) and records the bounds
     * asked for
     */
    private static final class ScriptedRandom extends Random {

        private final Deque<Integer> values = new ArrayDeque<>();
        private final List<Integer> bounds = new ArrayList<>();

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            Integer value = values.poll();
            return value != null ? value : 0;
        }
    }

}
