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

import com.jme3.math.Vector2f;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Trigger;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

/**
 * The CREATE_CREATURE trigger action places the creature on the raw tile of
 * the action, as DKII.EXE 0x518AD0 does.
 */
public class CreateCreatureTileTest {

    private static <T> T stub(Class<T> type, String methodName, Object result, Object[][] captured) {
        InvocationHandler handler = (proxy, method, args) -> {
            switch (method.getName()) {
                case "toString":
                    return type.getSimpleName() + " stub";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    break;
            }
            if (!method.getName().equals(methodName)) {
                throw new UnsupportedOperationException(method.getName());
            }
            captured[0] = args;
            return result;
        };
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    @Test
    void createCreatureUsesRawTile() {
        Object[][] ignored = new Object[1][];
        Map<Integer, Trigger> triggers = Map.of(1, new TriggerAction(null));
        IKwdFile kwd = stub(IKwdFile.class, "getTriggers", triggers, ignored);
        ILevelInfo levelInfo = stub(ILevelInfo.class, "getLevelData", kwd, ignored);
        Object[][] spawn = new Object[1][];
        ICreaturesController creatures = stub(ICreaturesController.class, "spawnCreature", null, spawn);
        TriggerControl control = new TriggerControl(null, levelInfo, null, null, creatures, 1);

        TriggerActionData action = new TriggerActionData(2, null, TriggerAction.ActionType.CREATE_CREATURE);
        action.setUserData("creatureId", (short) 1);
        action.setUserData("playerId", (short) 3);
        action.setUserData("level", (short) 1);
        action.setUserData("flag", (short) 0);
        action.setUserData("posX", 10);
        action.setUserData("posY", 20);
        control.doAction(action);

        assertNotNull(spawn[0], "spawnCreature not called");
        assertEquals(new Vector2f(10, 20), spawn[0][3]);
    }
}
