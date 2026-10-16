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
package toniarts.openkeeper.game.trigger;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 * Tests the walk of the trigger trees with trees built by hand, against the
 * behaviour of the original game: nesting is AND, siblings see each others'
 * side effects, repeat 1 = once and anything else = always, events only match
 * at the top level, unhandled actions are retried.
 */
class TriggerControlWalkTest {

    private static final short VALUE_EQUAL = 0x02 | 0x08; // Constant value, "="
    private static final short VALUE_PLUS = 0x02 | 0x10; // Constant value, "+="

    private final Map<Integer, Trigger> triggers = new HashMap<>();
    private final int[] flags = new int[128];
    private final GameTimer[] timers = new GameTimer[16];
    private double gameTime = 0;

    private TriggerControl control(int rootId) {
        for (int i = 0; i < timers.length; i++) {
            timers[i] = new GameTimer();
        }

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
                    case "getTimer" ->
                        timers[(Integer) args[0]];
                    case "getLevelScore" ->
                        0;
                    default ->
                        null;
                });
        IGameTimer gameTimer = () -> gameTime;

        return new TriggerControl(null, levelInfo, gameTimer, null, null, null, rootId);
    }

    /**
     * A flag condition: flag[id] == value
     */
    private void flagIs(int id, int next, int child, int repeat, int flagId, int value) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, id, next, child, repeat);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, TriggerGeneric.TargetType.FLAG);
        invoke(TriggerGeneric.class, trigger, "setTargetValueComparison", TriggerGeneric.ComparisonType.class, TriggerGeneric.ComparisonType.EQUAL_TO);
        data(trigger, "targetId", (short) flagId);
        data(trigger, "flag", (short) 1);
        data(trigger, "value", value);
    }

    /**
     * A timer condition: timer[id] >= seconds
     */
    private void timerAtLeast(int id, int next, int child, int repeat, int timerId, int seconds) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, id, next, child, repeat);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, TriggerGeneric.TargetType.TIMER);
        invoke(TriggerGeneric.class, trigger, "setTargetValueComparison", TriggerGeneric.ComparisonType.class, TriggerGeneric.ComparisonType.GREATER_OR_EQUAL_TO);
        data(trigger, "targetId", (short) timerId);
        data(trigger, "flag", (short) 1);
        data(trigger, "value", seconds);
    }

    private void event(int id, int next, int child, int repeat, TriggerGeneric.TargetType type) {
        TriggerGeneric trigger = new TriggerGeneric(null);
        base(trigger, id, next, child, repeat);
        invoke(TriggerGeneric.class, trigger, "setType", TriggerGeneric.TargetType.class, type);
    }

    private void setFlag(int id, int next, int flagId, short mode, int value) {
        TriggerAction trigger = new TriggerAction(null);
        base(trigger, id, next, 0, 0);
        invoke(TriggerAction.class, trigger, "setType", TriggerAction.ActionType.class, TriggerAction.ActionType.FLAG);
        data(trigger, "flagId", (short) flagId);
        data(trigger, "flag", mode);
        data(trigger, "value", value);
    }

    private void action(int id, int next, TriggerAction.ActionType type) {
        TriggerAction trigger = new TriggerAction(null);
        base(trigger, id, next, 0, 0);
        invoke(TriggerAction.class, trigger, "setType", TriggerAction.ActionType.class, type);
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
    void siblingsSeeEachOthersSideEffectsInTheSameWalk() {

        // [Set flag 5 = 1], [Flag 5 == 1 -> Set flag 6 = 1]
        setFlag(1, 2, 5, VALUE_EQUAL, 1);
        flagIs(2, 0, 3, 255, 5, 1);
        setFlag(3, 0, 6, VALUE_EQUAL, 1);

        control(1).update(0.25f);

        assertEquals(1, flags[5]);
        assertEquals(1, flags[6]);
    }

    @Test
    void repeatOneRunsTheActionsOnlyOnce() {

        // Flag 0 == 0 (repeat 1) -> flag 1 += 1
        flagIs(1, 0, 2, 1, 0, 0);
        setFlag(2, 0, 1, VALUE_PLUS, 1);

        TriggerControl control = control(1);
        control.update(0.25f);
        control.update(0.25f);
        control.update(0.25f);

        assertEquals(1, flags[1]);
    }

    @Test
    void otherRepeatsRunOnEveryWalk() {

        // 255 is "always", and so are 0 and 2 (the original never counts the repeat down)
        flagIs(1, 0, 2, 255, 0, 0);
        setFlag(2, 0, 1, VALUE_PLUS, 1);
        TriggerControl always = control(1);
        for (int i = 0; i < 3; i++) {
            always.update(0.25f);
        }
        assertEquals(3, flags[1]);

        triggers.clear();
        flags[1] = 0;
        flagIs(1, 0, 2, 2, 0, 0);
        setFlag(2, 0, 1, VALUE_PLUS, 1);
        TriggerControl twice = control(1);
        for (int i = 0; i < 3; i++) {
            twice.update(0.25f);
        }
        assertEquals(3, flags[1]);
    }

    @Test
    void conditionWithRepeatZeroIsDormant() {
        flagIs(1, 0, 2, 0, 0, 0);
        setFlag(2, 0, 1, VALUE_EQUAL, 1);

        control(1).update(0.25f);

        assertEquals(0, flags[1]);
    }

    @Test
    void flagsWrapAroundLikeBytes() {
        flags[1] = 255;
        setFlag(1, 0, 1, VALUE_PLUS, 1);

        control(1).update(0.25f);

        assertEquals(0, flags[1]);
    }

    @Test
    void timerThatHasNeverBeenStartedIsNeverTrue() {

        // Timer 3 >= 0 seconds -> flag 1 = 1
        timerAtLeast(1, 0, 2, 255, 3, 0);
        setFlag(2, 0, 1, VALUE_EQUAL, 1);
        TriggerControl control = control(1);

        control.update(0.25f);
        assertEquals(0, flags[1]);

        timers[3].initialize();
        control.update(0.25f);
        assertEquals(1, flags[1]);
    }

    @Test
    void timerComparesOnTicks() {

        // Timer 3 >= 10 seconds: false at 9.75 s, true at 10 s
        timerAtLeast(1, 0, 2, 255, 3, 10);
        setFlag(2, 0, 1, VALUE_EQUAL, 1);
        TriggerControl control = control(1);
        timers[3].initialize();

        for (int i = 0; i < 39; i++) {
            timers[3].update(0.25f);
        }
        control.update(0.25f);
        assertEquals(0, flags[1]);

        timers[3].update(0.25f);
        control.update(0.25f);
        assertEquals(1, flags[1]);
    }

    @Test
    void eventsMatchOnlyAtTheTopLevelOfTheTree() {

        // Top level: [Killed -> flag 1 = 1], nested: [Flag 0 == 0 -> [Killed -> flag 2 = 1]]
        event(1, 3, 2, 1, TriggerGeneric.TargetType.CREATURE_KILLED);
        setFlag(2, 0, 1, VALUE_EQUAL, 1);
        flagIs(3, 0, 4, 255, 0, 0);
        event(4, 0, 5, 1, TriggerGeneric.TargetType.CREATURE_KILLED);
        setFlag(5, 0, 2, VALUE_EQUAL, 1);
        TriggerControl control = control(1);

        // Walking does nothing, events are never true there
        control.update(0.25f);
        assertEquals(0, flags[1]);
        assertEquals(0, flags[2]);

        control.fire(TriggerGeneric.TargetType.CREATURE_KILLED, 0, 0);
        assertEquals(1, flags[1]);
        assertEquals(0, flags[2]);
    }

    @Test
    void eventSearchStopsAtTheFirstAction() {

        // [Set flag 3 = 1], [Killed -> flag 1 = 1]
        setFlag(1, 2, 3, VALUE_EQUAL, 1);
        event(2, 0, 3, 1, TriggerGeneric.TargetType.CREATURE_KILLED);
        setFlag(3, 0, 1, VALUE_EQUAL, 1);
        TriggerControl control = control(1);

        control.fire(TriggerGeneric.TargetType.CREATURE_KILLED, 0, 0);

        assertEquals(0, flags[1]);
    }

    @Test
    void unhandledActionStaysEnabledForRetry() {

        // Flag 0 == 0 (repeat 1) -> [Send to action point]. Nobody handles that
        flagIs(1, 0, 2, 1, 0, 0);
        action(2, 0, TriggerAction.ActionType.SEND_TO_AP);
        TriggerControl control = control(1);

        control.update(0.25f);

        TriggerData action = ((TriggerGenericData) control.root.getChild(0)).getChild(0);
        assertTrue(action.isEnabled());
    }

    @Test
    void handledOneShotActionDisablesItself() {
        flagIs(1, 0, 2, 1, 0, 0);
        setFlag(2, 0, 1, VALUE_EQUAL, 1);
        TriggerControl control = control(1);

        control.update(0.25f);

        TriggerData action = ((TriggerGenericData) control.root.getChild(0)).getChild(0);
        assertFalse(action.isEnabled());
    }

    @Test
    void armedTreeRunsEverythingOnce() {

        // A party tree: dormant, and when armed all the repeats are 1 whatever the map says
        flagIs(1, 0, 2, 255, 0, 0);
        setFlag(2, 0, 1, VALUE_PLUS, 1);
        TriggerControl control = control(1);
        control.setTreeArmed(false);

        control.update(0.25f);
        assertEquals(0, flags[1]);

        control.setTreeArmed(true);
        control.update(0.25f);
        control.update(0.25f);
        assertEquals(1, flags[1]);
    }
}
