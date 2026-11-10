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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TriggerAction.ActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import static org.junit.jupiter.api.Assertions.*;

class EffectGeneratorTriggerTest {
    @Test
    void sendsTheRawGeneratorIdAndNativeEnableValue() {
        var kwd = proxy(IKwdFile.class, (p, m, a) -> Map.of(1, new TriggerGeneric(null)));
        var level = proxy(ILevelInfo.class, (p, m, a) -> kwd);
        var calls = new ArrayList<List<Object>>();
        var game = proxy(IGameController.class, (p, m, a) -> {
            assertEquals("setEffectGeneratorEnabled", m.getName());
            calls.add(List.of(a));
            return null;
        });
        var control = new TriggerControl(game, level, null, null, null, 1);
        for (short available : new short[]{0, 1, 2}) {
            var action = new TriggerActionData(2, null, ActionType.TOGGLE_EFFECT_GENERATOR);
            action.setUserData("generatorId", (short) 7);
            action.setUserData("available", available);
            control.doAction(action);
        }
        assertEquals(List.of(List.of((short) 7, false), List.of((short) 7, true), List.of((short) 7, false)), calls);
    }

    @Test
    void unsupportedNativeMusicActionIsAnExplicitNoOp() {
        var kwd = proxy(IKwdFile.class, (p, m, a) -> Map.of(1, new TriggerGeneric(null)));
        var level = proxy(ILevelInfo.class, (p, m, a) -> kwd);
        var control = new TriggerControl(null, level, null, null, null, 1);
        assertDoesNotThrow(() -> control.doAction(new TriggerActionData(2, null, ActionType.SET_MUSIC_LEVEL)));
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
