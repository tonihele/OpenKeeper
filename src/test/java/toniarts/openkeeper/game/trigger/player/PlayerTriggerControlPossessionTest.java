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
 * DK II doesn't play the level's camera paths, widescreen or camera zooms and
 * rotations while the keeper possesses a creature
 */
class PlayerTriggerControlPossessionTest {

    private static final int TRIGGER_ID = 1;

    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private final List<String> calls = new ArrayList<>();
    private final PlayerTriggerControl control = new PlayerTriggerControl(stub(IGameController.class, null),
            stub(ILevelInfo.class, keeper), stub(IGameTimer.class, null), stub(IMapController.class, null),
            stub(ICreaturesController.class, null), TRIGGER_ID, Player.KEEPER1_ID, recordingPlayerService());

    @Test
    void cameraMovesArePlayedWhenNotPossessing() {
        doCameraActions();

        assertEquals(List.of("doTransition", "setWidescreen", "zoomViewToPoint", "rotateViewAroundPoint"), calls);
    }

    @Test
    void cameraMovesAreSkippedWhilePossessing() {
        keeper.setPossession(true);

        doCameraActions();

        assertEquals(List.of(), calls);
    }

    private void doCameraActions() {
        TriggerActionData followCameraPath = new TriggerActionData(2, null, TriggerAction.ActionType.FOLLOW_CAMERA_PATH);
        followCameraPath.setUserData("pathId", (short) 50);
        followCameraPath.setUserData("actionPointId", (short) 1);
        control.doAction(followCameraPath);

        TriggerActionData setWidescreen = new TriggerActionData(3, null, TriggerAction.ActionType.SET_WIDESCREEN_MODE);
        setWidescreen.setUserData("available", (short) 1);
        control.doAction(setWidescreen);

        TriggerActionData zoomToActionPoint = new TriggerActionData(4, null, TriggerAction.ActionType.ZOOM_TO_ACTION_POINT);
        zoomToActionPoint.setUserData("targetId", (short) 1);
        control.doAction(zoomToActionPoint);

        TriggerActionData rotateAroundActionPoint = new TriggerActionData(5, null, TriggerAction.ActionType.ROTATE_AROUND_ACTION_POINT);
        rotateAroundActionPoint.setUserData("actionPointId", (short) 1);
        rotateAroundActionPoint.setUserData("available", (short) 0);
        rotateAroundActionPoint.setUserData("angle", 90);
        rotateAroundActionPoint.setUserData("time", 5);
        control.doAction(rotateAroundActionPoint);
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
