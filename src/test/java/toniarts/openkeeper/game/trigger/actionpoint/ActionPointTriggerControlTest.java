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
package toniarts.openkeeper.game.trigger.actionpoint;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.ApTriggerFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * The action point scope: the possessed creature condition. It holds while a
 * creature that the player controls from the inside (first person) is on the
 * action point, as in DKII.EXE (0x004B06D0 looks at the creatures on the
 * tiles of the point).
 */
class ActionPointTriggerControlTest {

    private static final short VALUE_EQUAL = 0x02 | 0x08; // Constant value, "="
    private static final short CREATURE = 3;
    private static final short OBJECT = 6;

    private final Map<Integer, Trigger> triggers = new HashMap<>();
    private final int[] flags = new int[128];
    private final Map<String, List<ICreatureController>> creaturesByTile = new HashMap<>();

    private ICreatureController creature(short ownerId, short creatureId, boolean possessed) {
        Creature creatureData = new Creature();
        invoke(Creature.class, creatureData, "setCreatureId", short.class, creatureId);
        return (ICreatureController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getOwnerId" ->
                        ownerId;
                    case "getCreature" ->
                        creatureData;
                    case "isPossessed" ->
                        possessed;
                    default ->
                        null;
                });
    }

    private void put(int x, int y, ICreatureController creature) {
        creaturesByTile.computeIfAbsent(x + "," + y, key -> new ArrayList<>()).add(creature);
    }

    private ActionPointTriggerControl control() {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
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
        IGameController gameController = (IGameController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IGameController.class},
                (proxy, method, args) -> null);
        IEntityPositionLookup lookup = (IEntityPositionLookup) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> method.getName().equals("getEntityTypesInLocation") && args.length == 3 && args[0] instanceof Integer
                ? creaturesByTile.getOrDefault(args[0] + "," + args[1], List.of()) : List.of());
        IGameTimer gameTimer = () -> 0.0;

        // The action point covers the tiles (5,5) to (6,5)
        ActionPoint actionPoint = new ActionPoint(ApTriggerFixtures.actionPoint(1, 5, 5, 6, 5));
        return new ActionPointTriggerControl(gameController, levelInfo, gameTimer, null, null, 1, actionPoint, null, lookup);
    }

    /**
     * [AP possessed creature enters (player, creature type, kind) -> flag 1 = 1]
     */
    private void possessedEnters(int playerId, int creatureId, int kind) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, 1, 0, 2, 255);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, TriggerGeneric.TargetType.AP_POSESSED_CREATURE_ENTERS);
        data(trigger, "playerId", (short) playerId);
        data(trigger, "targetId", (short) creatureId);
        data(trigger, "targetType", (short) kind);
        data(trigger, "value", 0);

        TriggerAction action = new TriggerAction(null);
        base(action, 2, 0, 0, 0);
        invoke(TriggerAction.class, action, "setType", TriggerAction.ActionType.class, TriggerAction.ActionType.FLAG);
        data(action, "flagId", (short) 1);
        data(action, "flag", VALUE_EQUAL);
        data(action, "value", 1);
    }

    private void base(Trigger trigger, int id, int next, int child, int repeat) {
        invoke(Trigger.class, trigger, "setId", int.class, id);
        invoke(Trigger.class, trigger, "setIdNext", int.class, next);
        invoke(Trigger.class, trigger, "setIdChild", int.class, child);
        invoke(Trigger.class, trigger, "setRepeatTimes", short.class, (short) repeat);
        triggers.put(id, trigger);
    }

    private void data(Trigger trigger, String key, Number value) {
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

    @Test
    void holdsWhileAPossessedCreatureOfThePlayerIsOnThePoint() {
        possessedEnters(3, 0, CREATURE);
        put(6, 5, creature((short) 3, (short) 9, true));

        control().update(0.25f);

        assertEquals(1, flags[1]);
    }

    @Test
    void aCreatureThatIsNotPossessedDoesNotCount() {
        possessedEnters(3, 0, CREATURE);
        put(5, 5, creature((short) 3, (short) 9, false));

        control().update(0.25f);

        assertEquals(0, flags[1]);
    }

    @Test
    void aPossessedCreatureOutsideThePointDoesNotCount() {
        possessedEnters(3, 0, CREATURE);
        put(7, 5, creature((short) 3, (short) 9, true));

        control().update(0.25f);

        assertEquals(0, flags[1]);
    }

    @Test
    void thePlayerFilterSeparatesThePlayers() {
        possessedEnters(3, 0, CREATURE);
        put(5, 5, creature((short) 4, (short) 9, true));

        control().update(0.25f);
        assertEquals(0, flags[1]);

        put(5, 5, creature((short) 3, (short) 9, true));
        control().update(0.25f);
        assertEquals(1, flags[1]);
    }

    @Test
    void playerZeroMeansAnyPlayer() {
        possessedEnters(0, 0, CREATURE);
        put(5, 5, creature((short) 4, (short) 9, true));

        control().update(0.25f);

        assertEquals(1, flags[1]);
    }

    @Test
    void theCreatureTypeFilterSeparatesTheTypes() {
        possessedEnters(0, 12, CREATURE);
        put(5, 5, creature((short) 3, (short) 9, true));

        control().update(0.25f);
        assertEquals(0, flags[1]);

        put(6, 5, creature((short) 3, (short) 12, true));
        control().update(0.25f);
        assertEquals(1, flags[1]);
    }

    @Test
    void possessedObjectsAreNotSupportedAndNeverHold() {
        possessedEnters(0, 0, OBJECT);
        put(5, 5, creature((short) 3, (short) 9, true));

        control().update(0.25f);

        assertEquals(0, flags[1]);
    }
}
