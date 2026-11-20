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
package toniarts.openkeeper.game.task.worker;

import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Workers capture the knocked out enemies that lie on their keeper's land
 * (DKII.EXE 0x46D810)
 */
class CaptureEnemyCreatureTaskTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;

    @Test
    void enemyOnOurLandIsCaptured() {
        CaptureEnemyCreatureTask task = new CaptureEnemyCreatureTask(null, map(KEEPER), enemy(), KEEPER, null);

        assertTrue(task.isValid(null));
    }

    @Test
    void enemyOnUnclaimedLandIsLeft() {
        CaptureEnemyCreatureTask task = new CaptureEnemyCreatureTask(null, map(Player.NEUTRAL_PLAYER_ID), enemy(), KEEPER, null);

        assertFalse(task.isValid(null));
    }

    @Test
    void enemyOnEnemyLandIsLeft() {
        CaptureEnemyCreatureTask task = new CaptureEnemyCreatureTask(null, map(ENEMY), enemy(), KEEPER, null);

        assertFalse(task.isValid(null));
    }

    private static ICreatureController enemy() {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "isUnconscious" ->
                true;
            case "isDragged" ->
                false;
            case "getPosition" ->
                WorldUtils.pointToVector3f(1, 1);
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            case "toString" ->
                "enemy";
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    private static IMapController map(short tileOwner) {
        IMapTileController tile = proxy(IMapTileController.class, "getOwnerId", tileOwner);
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, "getTile", tile);
        IRoomController prison = proxy(IRoomController.class, "isFullCapacity", false);
        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                List.of(prison);
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, String methodName, Object result) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals(methodName)) {
                return result;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

}
