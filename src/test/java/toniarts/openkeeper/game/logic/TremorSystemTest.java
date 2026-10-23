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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
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
import toniarts.openkeeper.game.component.Tremor;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TremorSystemTest {

    private static final short FLOOR = 8;
    private static final short WALL = 9;
    private static final short ROCK = 2;
    private static final int DAMAGE_PER_TILE = 436;
    private static final Point CENTRE = new Point(5, 5);

    private final EntityData entityData = new DefaultEntityData();
    private final FakeMapData mapData = new FakeMapData(11, 11);
    private final Map<Short, Terrain> terrains = new HashMap<>();
    private final Map<Point, Integer> damageDone = new HashMap<>();
    private final ScriptedRandom random = new ScriptedRandom();
    private double gameTime = 10;
    private TremorSystem tremorSystem;

    @BeforeEach
    void setUp() {
        terrains.put(FLOOR, TerrainFixtures.terrain(FLOOR, TerrainFixtures.flags(Terrain.TerrainFlag.OWNABLE), 0, 660, 660, FLOOR));
        terrains.put(WALL, TerrainFixtures.terrain(WALL, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.OWNABLE), 0, 900, 900, WALL));
        terrains.put(ROCK, TerrainFixtures.terrain(ROCK, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 165, 660, WALL));
        for (FakeMapTile tile : mapData) {
            tile.terrainId = ROCK;
        }

        IGameTimer gameTimer = () -> gameTime;
        tremorSystem = new TremorSystem(entityData, gameTimer, mapController(), DAMAGE_PER_TILE, random);
        tremorSystem.start();
    }

    @Test
    void damagesPlayerOwnedReinforcedWallsAroundTheEpicentre() {

        // A single floor tile walled in, so the epicentre has to be the centre.
        // Walls of every keeper shake, the caster's own too, but not neutral ones
        setTile(CENTRE, FLOOR, Player.KEEPER1_ID);
        setTile(new Point(4, 4), WALL, Player.KEEPER1_ID);
        setTile(new Point(5, 4), WALL, Player.KEEPER2_ID);
        setTile(new Point(6, 6), WALL, Player.NEUTRAL_PLAYER_ID);
        addTremor(CENTRE, 0, 5);
        random.values.add(0); // The epicentre pick
        random.values.add(0); // Damage to (5, 4), the first wall clockwise from the north
        random.values.add(100); // Damage to (4, 4)

        tremorSystem.processTick(0.25f);

        assertEquals(Map.of(new Point(5, 4), DAMAGE_PER_TILE / 2, new Point(4, 4), DAMAGE_PER_TILE / 2 + 100), damageDone);
        assertEquals(List.of(1, DAMAGE_PER_TILE / 2, DAMAGE_PER_TILE / 2), random.bounds);
    }

    @Test
    void epicentreIsTheFirstOpenTileAroundTheRandomPick() {

        // The pick lands on (4, 4) in a radius 1 square, which is solid, so its
        // neighbours are tried clockwise from the north: (4, 3) is open
        setTile(new Point(4, 3), FLOOR, Player.KEEPER1_ID);
        setTile(new Point(3, 2), WALL, Player.KEEPER1_ID);
        setTile(CENTRE, WALL, Player.KEEPER1_ID);
        addTremor(CENTRE, 1, 5);
        random.values.add(0);

        tremorSystem.processTick(0.25f);

        assertEquals(Map.of(new Point(3, 2), DAMAGE_PER_TILE / 2), damageDone);
        assertEquals(9, random.bounds.get(0));
    }

    @Test
    void noShakeWhenThereIsNoOpenTileNearThePick() {
        setTile(CENTRE, WALL, Player.KEEPER1_ID);
        addTremor(CENTRE, 0, 5);
        random.values.add(0);

        tremorSystem.processTick(0.25f);

        assertTrue(damageDone.isEmpty());
    }

    @Test
    void radiusIsCappedAtFiveTiles() {
        addTremor(CENTRE, 9, 5);
        random.values.add(0);

        tremorSystem.processTick(0.25f);

        assertEquals(11 * 11, random.bounds.get(0));
    }

    @Test
    void tremorEndsAfterItsDuration() {
        EntityId tremor = addTremor(CENTRE, 0, 5);
        gameTime += 5;

        tremorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(tremor, Tremor.class));
        assertTrue(random.bounds.isEmpty());
    }

    @Test
    void atMostEightTremorsAtOnceTheOldestMakesWay() {
        List<EntityId> tremors = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            tremors.add(addTremor(CENTRE, 0, 5));
            gameTime += 0.25;
        }

        tremorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(tremors.get(0), Tremor.class));
        for (int i = 1; i < 9; i++) {
            assertNotNull(entityData.getComponent(tremors.get(i), Tremor.class));
        }
    }

    private EntityId addTremor(Point centre, int radius, double duration) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new Tremor(centre, radius, gameTime, gameTime + duration));
        return entity;
    }

    private void setTile(Point p, short terrainId, short ownerId) {
        FakeMapTile tile = mapData.getTile(p);
        tile.terrainId = terrainId;
        tile.ownerId = ownerId;
    }

    private IMapController mapController() {
        return (IMapController) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMapData":
                            return mapData;
                        case "getTerrain":
                            return terrains.get(((IMapTileInformation) args[0]).getTerrainId());
                        case "isSolid": {
                            FakeMapTile tile = mapData.getTile((Point) args[0]);
                            return tile == null || terrains.get(tile.terrainId).getFlags().contains(Terrain.TerrainFlag.SOLID);
                        }
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
