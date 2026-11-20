/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.MapController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerLogicController;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.*;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.PLAYER_ENEMY_BREACHED;
import static toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

class EnemyBreachTileWorkTest {
    private static final short WALL = 9;
    private static final short FLOOR = 3;
    private static final short ATTACKER = 3;
    private static final short OWNER = 4;
    private static final Point POINT = new Point(0, 0);

    @Test
    void finalHitDispatchesToAttackerBeforeTerrainReplacement() {
        try (Fixture f = new Fixture(OWNER, TerrainFixtures.flags(SOLID, OWNABLE), FLOOR)) {
            f.hit();
            assertTrue(f.events.isEmpty());
            assertEquals(5, f.tileHealth());
            f.hit();
            assertEquals(List.of(ATTACKER), f.events);
            assertEquals(1, f.flags[0]);
            assertEquals(0, f.flags[1]);
            assertEquals(FLOOR, f.map.getMapData().getTile(POINT).getTerrainId());
            assertEquals(Player.NEUTRAL_PLAYER_ID, f.map.getMapData().getTile(POINT).getOwnerId());
            f.scripts.processTick(.25f);
            assertEquals(1, f.flags[0]);
        }
    }

    @ParameterizedTest
    @CsvSource({"3,false", "4,true", "1,true", "2,true"})
    void comparesOwnershipWithoutFilteringPlayerKind(short owner, boolean expected) {
        try (Fixture f = new Fixture(owner, TerrainFixtures.flags(SOLID, OWNABLE), FLOOR)) {
            f.hit();
            f.hit();
            assertEquals(expected ? List.of(ATTACKER) : List.of(), f.events);
        }
    }

    @Test
    void diggingUnownedRockDoesNotEmit() {
        try (Fixture f = new Fixture(Player.NEUTRAL_PLAYER_ID, TerrainFixtures.flags(SOLID), FLOOR)) {
            f.hit();
            f.hit();
            assertTrue(f.events.isEmpty());
        }
    }

    @Test
    void destroyingClaimedFloorDoesNotEmit() {
        try (Fixture f = new Fixture(OWNER, TerrainFixtures.flags(OWNABLE), FLOOR)) {
            f.hit();
            f.hit();
            assertTrue(f.events.isEmpty());
        }
    }

    @Test
    void unchangedDestroyedTerrainDoesNotEmit() {
        try (Fixture f = new Fixture(OWNER, TerrainFixtures.flags(SOLID, OWNABLE), WALL)) {
            f.hit();
            f.hit();
            assertTrue(f.events.isEmpty());
        }
    }

    @Test
    void impenetrableTerrainDoesNotEmitEvenIfTheWorkPathReachesZero() {
        try (Fixture f = new Fixture(OWNER, TerrainFixtures.flags(SOLID, OWNABLE, IMPENETRABLE), FLOOR)) {
            f.hit();
            f.hit();
            assertTrue(f.events.isEmpty());
        }
    }

    @Test
    void anotherWallCollapseCanFireAgain() {
        try (Fixture f = new Fixture(OWNER, TerrainFixtures.flags(SOLID, OWNABLE), FLOOR)) {
            f.hit();
            f.hit();
            var tile = f.map.getMapData().getTile(POINT);
            tile.setTerrainId(WALL);
            tile.setOwnerId(OWNER);
            tile.setHealth(10);
            tile.setMaxHealth(10);
            f.hit();
            f.hit();
            assertEquals(List.of(ATTACKER, ATTACKER), f.events);
            assertEquals(2, f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        final List<Short> events = new ArrayList<>();
        final int[] flags = new int[2];
        final MapController map;
        final PlayerTriggerLogicController scripts;

        Fixture(short owner, java.util.EnumSet<Terrain.TerrainFlag> terrainFlags, short destroyedType) {
            Terrain wall = TerrainFixtures.terrain(WALL, terrainFlags, 0, 10, 10, WALL);
            wall.setDestroyedTypeTerrainId(destroyedType);
            Terrain floor = TerrainFixtures.terrain(FLOOR, TerrainFixtures.flags(), 0, 10, 10, FLOOR);
            floor.setDestroyedTypeTerrainId(FLOOR);
            GameMap rawMap = new GameMap(1, 1);
            rawMap.getTiles()[0][0] = new Tile() {
                @Override public short getTerrainId() { return WALL; }
                @Override public short getPlayerId() { return owner; }
            };
            Map<Short, Keeper> keepers = Map.of(ATTACKER, new Keeper(false, ATTACKER), OWNER, new Keeper(false, OWNER),
                    Player.GOOD_PLAYER_ID, new Keeper(false, Player.GOOD_PLAYER_ID),
                    Player.NEUTRAL_PLAYER_ID, new Keeper(false, Player.NEUTRAL_PLAYER_ID));
            Map<Integer, Trigger> records = Map.of(
                    1, PlayerBreachFixtures.condition(1, PLAYER_ENEMY_BREACHED, -1, 0, 255),
                    -1, PlayerBreachFixtures.action(-1, 0, 0),
                    2, PlayerBreachFixtures.condition(2, PLAYER_ENEMY_BREACHED, -2, 0, 255),
                    -2, PlayerBreachFixtures.action(-2, 1, 0));
            IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getMap" -> rawMap;
                case "getTerrain" -> (short) args[0] == WALL ? wall : floor;
                case "getTriggers" -> records;
                case "getPlayers" -> Map.of(ATTACKER, PlayerBreachFixtures.player(ATTACKER, 1),
                        OWNER, PlayerBreachFixtures.player(OWNER, 2),
                        Player.GOOD_PLAYER_ID, PlayerBreachFixtures.player(Player.GOOD_PLAYER_ID, 0),
                        Player.NEUTRAL_PLAYER_ID, PlayerBreachFixtures.player(Player.NEUTRAL_PLAYER_ID, 0));
                default -> throw new UnsupportedOperationException(method);
            });
            ILevelInfo level = proxy(ILevelInfo.class, (method, args) -> switch (method) {
                case "getLevelData" -> kwd;
                case "getPlayers" -> keepers;
                case "getFlag" -> flags[((Number) args[0]).intValue()];
                case "setFlag" -> { flags[((Number) args[0]).intValue()] = (int) args[1]; yield null; }
                default -> throw new UnsupportedOperationException(method);
            });
            scripts = new PlayerTriggerLogicController(null, level, null, null, null, null);
            Map<MiscType, Variable.MiscVariable> settings = new EnumMap<>(MiscType.class);
            for (MiscType type : new MiscType[]{MiscType.DIG_ENEMY_WALL_HEALTH,
                MiscType.DIG_OWN_WALL_HEALTH, MiscType.DIG_ROCK_HEALTH, MiscType.ATTACK_TILE_HEALTH}) {
                settings.put(type, CreatureSleepFixtures.variable(type, 5));
            }
            map = new MapController(kwd, null, settings, null, data, level,
                    proxy(GameEventService.class, (method, args) -> null), this::onBreach);
        }

        private void onBreach(short player) {
            events.add(player);
            assertEquals(WALL, map.getMapData().getTile(POINT).getTerrainId());
            assertEquals(0, tileHealth());
            scripts.onEnemyBreached(player);
        }

        int tileHealth() { return map.getMapData().getTile(POINT).getHealth(); }
        void hit() { map.damageTile(POINT, ATTACKER, null); }
        @Override public void close() { data.close(); }
    }

    @FunctionalInterface
    private interface Call { Object invoke(String method, Object[] args); }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> call.invoke(method.getName(), args)));
    }
}
