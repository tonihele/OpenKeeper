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
package toniarts.openkeeper.game.trigger.player;

import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.MapController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.GameMap;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Tile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

/**
 * The "dungeon breached" and "enemy breached" conditions of a player tree. A
 * breach is when a claimed wall of one player is brought down by another
 * player: the owner of the wall gets the dungeon breached event, the one who
 * brought it down the enemy breached event
 */
class DungeonBreachTriggerTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short OTHER_KEEPER = Player.KEEPER2_ID;
    private static final short WALL_ID = 9;
    private static final int FLAG_ID = 7;

    private final Map<Integer, Trigger> triggers = new HashMap<>();
    private final int[] flags = new int[16];
    private MapController mapController;

    @Test
    void nothingHappensBeforeAWallIsBroughtDown() {
        PlayerTriggerControl control = control(KEEPER, TargetType.PLAYER_DUNGEON_BREACHED, KEEPER);

        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theOwnerOfTheWallGetsTheDungeonBreached() {
        PlayerTriggerControl control = control(KEEPER, TargetType.PLAYER_DUNGEON_BREACHED, KEEPER);

        bringDownWall(OTHER_KEEPER);
        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void theOwnerDoesNotGetTheEnemyBreached() {
        PlayerTriggerControl control = control(KEEPER, TargetType.PLAYER_ENEMY_BREACHED, KEEPER);

        bringDownWall(OTHER_KEEPER);
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void theOneWhoBroughtItDownGetsTheEnemyBreached() {
        PlayerTriggerControl control = control(OTHER_KEEPER, TargetType.PLAYER_ENEMY_BREACHED, KEEPER);

        bringDownWall(OTHER_KEEPER);
        control.update(0.25f);

        assertEquals(1, flags[FLAG_ID]);
    }

    @Test
    void theOneWhoBroughtItDownDoesNotGetTheDungeonBreached() {
        PlayerTriggerControl control = control(OTHER_KEEPER, TargetType.PLAYER_DUNGEON_BREACHED, KEEPER);

        bringDownWall(OTHER_KEEPER);
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void diggingYourOwnWallIsNoBreach() {
        PlayerTriggerControl control = control(KEEPER, TargetType.PLAYER_DUNGEON_BREACHED, KEEPER);

        bringDownWall(KEEPER);
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    @Test
    void aBreachIsOneEventNotAState() {
        PlayerTriggerControl control = control(KEEPER, TargetType.PLAYER_DUNGEON_BREACHED, KEEPER);

        bringDownWall(OTHER_KEEPER);
        control.update(0.25f);
        flags[FLAG_ID] = 0;
        control.update(0.25f);

        assertEquals(0, flags[FLAG_ID]);
    }

    private void bringDownWall(short playerId) {
        mapController.damageTile(new Point(0, 0), playerId, null);
    }

    /**
     * A tree of the player with the breach condition setting a flag, and a map
     * of one wall owned by the wall owner
     */
    private PlayerTriggerControl control(short playerId, TargetType type, short wallOwnerId) {
        TriggerGeneric condition = new TriggerGeneric(null);
        base(condition, 1, 2, 1);
        invoke(TriggerGeneric.class, condition, "setType", TargetType.class, type);
        data(condition, "playerId", (short) 0); // Any, like in level 1
        TriggerAction action = new TriggerAction(null);
        base(action, 2, 0, 0);
        invoke(TriggerAction.class, action, "setType", TriggerAction.ActionType.class, TriggerAction.ActionType.FLAG);
        data(action, "flagId", (short) FLAG_ID);
        data(action, "flag", (short) (0x02 | 0x08)); // Constant value, "="
        data(action, "value", 1);

        Terrain wall = TerrainFixtures.terrain(WALL_ID, flags(Terrain.TerrainFlag.SOLID, Terrain.TerrainFlag.OWNABLE), 0, 10, 10, WALL_ID);
        Terrain rock = TerrainFixtures.terrain((short) 0, flags(Terrain.TerrainFlag.SOLID), 0, 100, 100, WALL_ID);
        GameMap map = new GameMap(1, 1);
        map.getTiles()[0][0] = new Tile() {

            @Override
            public short getTerrainId() {
                return WALL_ID;
            }

            @Override
            public short getPlayerId() {
                return wallOwnerId;
            }
        };
        IKwdFile kwdFile = proxy(IKwdFile.class, (name, args) -> switch (name) {
            case "getMap" ->
                map;
            case "getTerrain" ->
                (short) args[0] == WALL_ID ? wall : rock;
            case "getTriggers" ->
                triggers;
            default ->
                null;
        });
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (name, args) -> switch (name) {
            case "getPlayers" ->
                Map.of(KEEPER, new Keeper(false, KEEPER), OTHER_KEEPER, new Keeper(false, OTHER_KEEPER));
            case "getLevelData" ->
                kwdFile;
            case "getFlag" ->
                flags[(Integer) args[0]];
            case "setFlag" -> {
                flags[(Integer) args[0]] = (Integer) args[1];
                yield null;
            }
            default ->
                null;
        });
        mapController = new MapController(kwdFile, null,
                Map.of(MiscType.DIG_ENEMY_WALL_HEALTH, CreatureSleepFixtures.variable(MiscType.DIG_ENEMY_WALL_HEALTH, 100),
                        MiscType.DIG_OWN_WALL_HEALTH, CreatureSleepFixtures.variable(MiscType.DIG_OWN_WALL_HEALTH, 100)),
                null, new DefaultEntityData(), levelInfo, proxy(GameEventService.class, (name, args) -> null));
        IPlayerController playerController = proxy(IPlayerController.class,
                (name, args) -> name.equals("getStatsControl") ? new PlayerStatsControl() : null);
        IGameController gameController = proxy(IGameController.class,
                (name, args) -> name.equals("getPlayerController") ? playerController : null);

        return new PlayerTriggerControl(gameController, levelInfo, () -> 0, mapController, null, 1, playerId, null);
    }

    private void base(Trigger trigger, int id, int child, int repeat) {
        invoke(Trigger.class, trigger, "setId", int.class, id);
        invoke(Trigger.class, trigger, "setIdChild", int.class, child);
        invoke(Trigger.class, trigger, "setRepeatTimes", short.class, (short) repeat);
        triggers.put(id, trigger);
    }

    private static void data(Trigger trigger, String key, Number value) {
        invoke(Trigger.class, trigger, "setUserData", String.class, key, Number.class, value);
    }

    private static void invoke(Class<?> clazz, Object target, String name, Object... typesAndArgs) {
        try {
            Class<?>[] types = new Class<?>[typesAndArgs.length / 2];
            Object[] args = new Object[types.length];
            for (int i = 0; i < types.length; i++) {
                types[i] = (Class<?>) typesAndArgs[i * 2];
                args[i] = typesAndArgs[i * 2 + 1];
            }
            Method method = clazz.getDeclaredMethod(name, types);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface Handler {

        Object handle(String name, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Handler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = handler.handle(method.getName(), args);
            if (result == null && method.getReturnType().isPrimitive()) {
                return method.getReturnType() == boolean.class ? (Object) false : (Object) 0;
            }
            return result;
        }));
    }
}
