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
package toniarts.openkeeper.game.trigger.object;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * The tree of an object. In the original game the Object Claimed event is
 * raised when an object owned by the neutral player gets a new owner, and the
 * tree of an object is not walked at any other time.
 */
class ObjectClaimedTriggerTest {

    private static final short VALUE_EQUAL = 0x02 | 0x08; // Constant value, "="
    private static final short KEEPER1 = Player.KEEPER1_ID;
    private static final short KEEPER2 = Player.KEEPER2_ID;

    private final Map<Integer, Trigger> triggers = new HashMap<>();
    private final int[] flags = new int[128];
    private short owner = Player.NEUTRAL_PLAYER_ID;
    private boolean removed = false;

    private ObjectTriggerControl control(int rootId) {
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
                    case "getLevelScore" ->
                        0;
                    default ->
                        null;
                });
        IGameController gameController = (IGameController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IGameController.class},
                (proxy, method, args) -> null);
        IGameTimer gameTimer = () -> 0;

        ObjectTriggerControl control = new ObjectTriggerControl(gameController, levelInfo, gameTimer, null, null, rootId,
                Player.NEUTRAL_PLAYER_ID, null);
        control.setThing((IObjectController) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IObjectController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getOwnerId" ->
                        owner;
                    case "isRemoved" ->
                        removed;
                    default ->
                        null;
                }));
        return control;
    }

    private void claimed(int id, int next, int child) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, id, next, child, 1);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, TriggerGeneric.TargetType.OBJECT_CLAIMED);
    }

    private void flagIs(int id, int next, int child, int flagId, int value) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, id, next, child, 255);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, TriggerGeneric.TargetType.FLAG);
        invoke(TriggerGeneric.class, trigger, "setTargetValueComparison", TriggerGeneric.ComparisonType.class, TriggerGeneric.ComparisonType.EQUAL_TO);
        data(trigger, "targetId", (short) flagId);
        data(trigger, "flag", (short) 1);
        data(trigger, "value", value);
    }

    private void setFlag(int id, int next, int flagId, int value) {
        TriggerAction trigger = new TriggerAction(null);
        base(trigger, id, next, 0, 0);
        invoke(TriggerAction.class, trigger, "setType", TriggerAction.ActionType.class, TriggerAction.ActionType.FLAG);
        data(trigger, "flagId", (short) flagId);
        data(trigger, "flag", VALUE_EQUAL);
        data(trigger, "value", value);
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
    void claimingANeutralObjectRaisesTheEvent() {

        // Object claimed -> flag 1 = 1
        claimed(1, 0, 2);
        setFlag(2, 0, 1, 1);
        ObjectTriggerControl control = control(1);

        control.update(0.25f);
        assertEquals(0, flags[1]);

        owner = KEEPER1;
        control.update(0.25f);
        assertEquals(1, flags[1]);
    }

    @Test
    void anObjectChangingHandsBetweenKeepersIsNotClaimed() {
        claimed(1, 0, 2);
        setFlag(2, 0, 1, 1);
        owner = KEEPER1;
        ObjectTriggerControl control = control(1);

        owner = KEEPER2;
        control.update(0.25f);

        assertEquals(0, flags[1]);
    }

    @Test
    void aRemovedObjectIsNotClaimed() {
        claimed(1, 0, 2);
        setFlag(2, 0, 1, 1);
        ObjectTriggerControl control = control(1);

        removed = true;
        owner = KEEPER1;
        control.update(0.25f);

        assertEquals(0, flags[1]);
    }

    @Test
    void theTreeIsOnlyWalkedWhenTheObjectIsClaimed() {

        // [Flag 0 == 0 -> flag 2 = 1], [Object claimed -> flag 1 = 1]
        flagIs(1, 3, 2, 0, 0);
        setFlag(2, 0, 2, 1);
        claimed(3, 0, 4);
        setFlag(4, 0, 1, 1);
        ObjectTriggerControl control = control(1);

        control.update(0.25f);
        assertEquals(0, flags[2]);

        owner = KEEPER1;
        control.update(0.25f);
        assertEquals(1, flags[1]);
        assertEquals(0, flags[2]);
    }
}
