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
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TriggerAction.ActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import static org.junit.jupiter.api.Assertions.*;

class TimerSpeechTriggerTest {
    @Test
    void togglesCountdownSpeechForBothAvailableValues() {
        IKwdFile kwd = proxy(IKwdFile.class, (p, m, a) -> Map.of(1, new TriggerGeneric(null)));
        var enabled = new ArrayList<Boolean>();
        ILevelInfo level = proxy(ILevelInfo.class, (p, m, a) -> {
            if (m.getName().equals("getLevelData")) {
                return kwd;
            }
            assertEquals("setTimerSpeechEnabled", m.getName());
            enabled.add((boolean) a[0]);
            return null;
        });
        var control = new TriggerControl(null, level, null, null, null, 1);
        for (short value : new short[]{0, 1, 0, 1}) {
            var action = new TriggerActionData(2, null, ActionType.SET_TIMER_SPEECH);
            action.setUserData("available", value);
            control.doAction(action);
        }
        assertEquals(List.of(false, true, false, true), enabled);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
