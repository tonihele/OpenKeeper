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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Workers rescue the knocked out creatures of their keeper, also the ones
 * without a lair (DKII.EXE 0x46D810, 0x46E610, 0x468420)
 */
class RescueCreatureTaskTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;

    private final List<String> calls = new ArrayList<>();

    @Test
    void creatureWithoutLairIsRescuedWhenLairHasRoom() {
        RescueCreatureTask task = new RescueCreatureTask(null, null, map(KEEPER, List.of(new Point(5, 5))), creature(), KEEPER);

        assertTrue(task.isValid(null));
    }

    @Test
    void creatureWithoutLairIsLeftWhenThereIsNoRoomForOne() {
        RescueCreatureTask task = new RescueCreatureTask(null, null, map(KEEPER, List.of()), creature(), KEEPER);

        assertFalse(task.isValid(null));
    }

    @Test
    void creatureOnEnemyLandIsLeft() {
        RescueCreatureTask task = new RescueCreatureTask(null, null, map(ENEMY, List.of(new Point(5, 5))), creature(), KEEPER);

        assertFalse(task.isValid(null));
    }

    @Test
    void creatureWithoutLairWakesUpInTheLair() {
        CarryCreatureToLairTask task = new CarryCreatureToLairTask(null, null, creature(), new Point(5, 5), KEEPER);

        task.executeTask(null, 0);

        assertEquals(List.of("wakeUp", "setHaulable"), calls);
    }

    private ICreatureController creature() {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "isUnconscious" ->
                true;
            case "isDragged", "isRecuperating", "hasLair", "isDead", "isWorker" ->
                false;
            case "getPosition" ->
                WorldUtils.pointToVector3f(1, 1);
            case "wakeUp", "sleep", "setHaulable" -> {
                calls.add(method.getName());
                yield null;
            }
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            case "toString" ->
                "creature";
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    private static IMapController map(short tileOwner, List<Point> freeLairTiles) {
        IMapTileController tile = proxy(IMapTileController.class, "getOwnerId", tileOwner);
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, "getTile", tile);
        IRoomObjectControl<?> lairs = proxy(IRoomObjectControl.class, "getAvailableCoordinates", freeLairTiles);
        IRoomController lair = proxy(IRoomController.class, "getObjectControl", lairs);
        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                List.of(lair);
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
