/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.trigger.player;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.TriggerAction.ActionType;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A level can refer to an action point it doesn't have (user made levels). The
 * trigger action is then skipped instead of crashing the logic loop.
 */
class MissingActionPointTriggerTest {

    private final List<String> calls = new ArrayList<>();

    @Test
    void revealActionPointOfMissingActionPointIsSkipped() {
        PlayerTriggerControl control = control();

        assertDoesNotThrow(() -> control.doAction(action(ActionType.REVEAL_ACTION_POINT)));

        assertTrue(calls.isEmpty());
    }

    @Test
    void otherActionPointActionsAreSkippedToo() {
        PlayerTriggerControl control = control();

        for (ActionType type : new ActionType[]{ActionType.FLASH_ACTION_POINT,
            ActionType.ROTATE_AROUND_ACTION_POINT, ActionType.FOLLOW_CAMERA_PATH,
            ActionType.CREATE_HERO_PARTY, ActionType.ZOOM_TO_ACTION_POINT}) {
            assertDoesNotThrow(() -> control.doAction(action(type)), type.name());
        }

        assertEquals(List.of(), calls);
    }

    private static TriggerActionData action(ActionType type) {
        TriggerActionData action = new TriggerActionData(2, null, type);
        action.setUserData("actionPointId", (short) 5);
        action.setUserData("targetId", (short) 5);
        action.setUserData("available", (short) 0);
        action.setUserData("value", 1);
        action.setUserData("angle", 0);
        action.setUserData("time", 0);
        action.setUserData("pathId", (short) 0);
        action.setUserData("partyId", (short) 1);
        action.setUserData("type", (short) 0);
        return action;
    }

    private PlayerTriggerControl control() {
        IKwdFile kwd = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (p, m, a) -> {
                    if (m.getName().equals("getTriggers")) {
                        return Map.of(1, new TriggerGeneric(null));
                    }
                    throw new UnsupportedOperationException(m.getName());
                });
        ILevelInfo level = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (p, m, a) -> switch (m.getName()) {
                    case "getLevelData" -> kwd;
                    case "getActionPoint" -> null;
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        IMapController map = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (p, m, a) -> {
                    calls.add("map." + m.getName());
                    return null;
                });
        ICreaturesController creatures = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (p, m, a) -> {
                    calls.add("creatures." + m.getName());
                    return null;
                });
        // The player service is never reached for a missing action point
        return new PlayerTriggerControl(null, level, null, map, creatures, 1, (short) 1, null);
    }
}
