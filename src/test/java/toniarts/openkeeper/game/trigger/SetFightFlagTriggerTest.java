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
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TriggerAction.ActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Set Fight Flag script action turns the creature's "will fight" flag on
 * only for 1, like DK II (0x49AC80)
 */
class SetFightFlagTriggerTest {

    @Test
    void setsWillFightOnlyForOne() throws ReflectiveOperationException {
        IKwdFile kwd = proxy(IKwdFile.class, (p, m, a) -> Map.of(1, new TriggerGeneric(null)));
        ILevelInfo level = proxy(ILevelInfo.class, (p, m, a) -> kwd);
        List<Object> calls = new ArrayList<>();
        ICreatureController creature = proxy(ICreatureController.class, (p, m, a) -> {
            assertEquals("setWillFight", m.getName());
            calls.add(a[0]);
            return null;
        });
        CreatureTriggerControl creatureControl = new CreatureTriggerControl(null, level, null, null, null, 1, (short) 3, null, false);
        // The thing is set when the creature spawns, setThing is not visible from here
        var setThing = CreatureTriggerControl.class.getDeclaredMethod("setThing", ICreatureController.class);
        setThing.setAccessible(true);
        setThing.invoke(creatureControl, creature);
        TriggerControl control = creatureControl;

        for (short available : new short[]{0, 1, 2}) {
            TriggerActionData action = new TriggerActionData(2, null, ActionType.SET_FIGHT_FLAG);
            action.setUserData("available", available);
            control.doAction(action);
        }

        assertEquals(List.of(false, true, false), calls);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
