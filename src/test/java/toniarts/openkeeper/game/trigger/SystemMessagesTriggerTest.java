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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import toniarts.openkeeper.game.controller.GameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

/**
 * The Set System Messages script action. In the original game it is one
 * level-wide switch: every level starts with the system messages on, and the
 * action turns them off or back on for all the players.
 */
class SystemMessagesTriggerTest {

    private final Map<Integer, Trigger> triggers = new HashMap<>();
    private final List<Boolean> sentToPlayers = new ArrayList<>();
    private Boolean levelSetting = null;

    private TriggerControl control(int rootId) {
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> method.getName().equals("getTriggers") ? triggers : null);
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ILevelInfo.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getLevelData" ->
                        kwdFile;
                    case "setSystemMessagesEnabled" -> {
                        levelSetting = (Boolean) args[0];
                        yield null;
                    }
                    default ->
                        null;
                });
        PlayerService playerService = (PlayerService) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{PlayerService.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("setSystemMessagesEnabled")) {
                        sentToPlayers.add((Boolean) args[0]);
                    }
                    return null;
                });
        IGameTimer gameTimer = () -> 0;

        return new TriggerControl(null, levelInfo, gameTimer, null, null, playerService, rootId);
    }

    private void setSystemMessages(int id, int next, int available) {
        TriggerAction trigger = new TriggerAction(null);
        invoke(Trigger.class, trigger, "setId", int.class, id);
        invoke(Trigger.class, trigger, "setIdNext", int.class, next);
        invoke(Trigger.class, trigger, "setIdChild", int.class, 0);
        invoke(Trigger.class, trigger, "setRepeatTimes", short.class, (short) 1);
        invoke(TriggerAction.class, trigger, "setType", TriggerAction.ActionType.class, TriggerAction.ActionType.SET_SYSTEM_MESSAGES);
        invoke(Trigger.class, trigger, "setUserData", String.class, "available", Number.class, (short) available);
        triggers.put(id, trigger);
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
    void turningTheSystemMessagesOffReachesTheLevelAndAllThePlayers() {
        setSystemMessages(1, 0, 0);

        control(1).update(0.25f);

        assertEquals(Boolean.FALSE, levelSetting);
        assertEquals(List.of(false), sentToPlayers);
    }

    @Test
    void turningTheSystemMessagesBackOn() {

        // [System messages off], [System messages on]
        setSystemMessages(1, 2, 0);
        setSystemMessages(2, 0, 1);

        control(1).update(0.25f);

        assertEquals(Boolean.TRUE, levelSetting);
        assertEquals(List.of(false, true), sentToPlayers);
    }

    @Test
    void everyLevelStartsWithTheSystemMessagesOn() throws ReflectiveOperationException {
        Class<?> levelInfoClass = Class.forName(GameController.class.getName() + "$LevelInfo");
        Constructor<?> constructor = levelInfoClass.getDeclaredConstructor(IKwdFile.class);
        constructor.setAccessible(true);

        ILevelInfo levelInfo = (ILevelInfo) constructor.newInstance((IKwdFile) null);
        assertTrue(levelInfo.isSystemMessagesEnabled());

        levelInfo.setSystemMessagesEnabled(false);
        assertFalse(levelInfo.isSystemMessagesEnabled());

        // The next level gets a new level info
        ILevelInfo nextLevelInfo = (ILevelInfo) constructor.newInstance((IKwdFile) null);
        assertTrue(nextLevelInfo.isSystemMessagesEnabled());
    }
}
