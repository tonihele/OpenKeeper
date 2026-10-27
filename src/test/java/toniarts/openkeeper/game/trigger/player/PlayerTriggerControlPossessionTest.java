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
package toniarts.openkeeper.game.trigger.player;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.TriggerActionData;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.TriggerAction;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II doesn't play the level's camera paths or widescreen while the keeper
 * possesses a creature
 */
class PlayerTriggerControlPossessionTest {

    private static final int TRIGGER_ID = 1;

    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private final List<String> calls = new ArrayList<>();
    private final PlayerTriggerControl control = new PlayerTriggerControl(stub(IGameController.class, null),
            stub(ILevelInfo.class, keeper), stub(IGameTimer.class, null), stub(IMapController.class, null),
            stub(ICreaturesController.class, null), TRIGGER_ID, Player.KEEPER1_ID, recordingPlayerService());

    @Test
    void cameraPathAndWidescreenArePlayedWhenNotPossessing() {
        control.doAction(followCameraPath());
        control.doAction(setWidescreen());

        assertEquals(List.of("doTransition", "setWidescreen"), calls);
    }

    @Test
    void cameraPathAndWidescreenAreSkippedWhilePossessing() {
        keeper.setPossession(true);

        control.doAction(followCameraPath());
        control.doAction(setWidescreen());

        assertEquals(List.of(), calls);
    }

    private static TriggerActionData followCameraPath() {
        TriggerActionData action = new TriggerActionData(2, null, TriggerAction.ActionType.FOLLOW_CAMERA_PATH);
        action.setUserData("pathId", (short) 50);
        action.setUserData("actionPointId", (short) 1);
        return action;
    }

    private static TriggerActionData setWidescreen() {
        TriggerActionData action = new TriggerActionData(3, null, TriggerAction.ActionType.SET_WIDESCREEN_MODE);
        action.setUserData("available", (short) 1);
        return action;
    }

    private PlayerService recordingPlayerService() {
        return (PlayerService) Proxy.newProxyInstance(PlayerService.class.getClassLoader(), new Class<?>[]{PlayerService.class},
                (proxy, method, args) -> {
                    calls.add(method.getName());
                    return null;
                });
    }

    /**
     * Stub where every method returns the keeper, an action point or a level
     * with one trigger when asked for one, and nothing otherwise
     */
    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Keeper keeper) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getReturnType() == Keeper.class) {
                return keeper;
            }
            if (method.getReturnType() == ActionPoint.class) {
                return new ActionPoint(new Thing.ActionPoint());
            }
            if (method.getReturnType() == IKwdFile.class) {
                return stub(IKwdFile.class, keeper);
            }
            if (method.getName().equals("getTriggers")) {
                return Map.of(TRIGGER_ID, new TriggerAction(null));
            }
            return null;
        });
    }
}
