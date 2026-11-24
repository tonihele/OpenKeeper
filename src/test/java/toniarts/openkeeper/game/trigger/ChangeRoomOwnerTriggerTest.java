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
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerAction.ActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class ChangeRoomOwnerTriggerTest {
    @Test
    void dispatchesOwnerAndConvertsCoordinatesToZeroBased() {
        IKwdFile kwd = proxy(IKwdFile.class, (p, m, a) -> Map.of(1, new TriggerGeneric(null)));
        ILevelInfo level = proxy(ILevelInfo.class, (p, m, a) -> kwd);
        var calls = new ArrayList<List<Object>>();
        IMapController map = proxy(IMapController.class, (p, m, a) -> {
            assertEquals("changeRoomOwner", m.getName());
            calls.add(List.of(a));
            return null;
        });
        var control = new TriggerControl(null, level, null, map, null, 1);
        var action = new TriggerActionData(2, null, ActionType.CHANGE_ROOM_OWNER);
        action.setUserData("playerId", Player.KEEPER2_ID);
        action.setUserData("posX", 4);
        action.setUserData("posY", 9);
        control.doAction(action);
        assertEquals(List.of(List.of(new Point(3, 8), Player.KEEPER2_ID)), calls);
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
