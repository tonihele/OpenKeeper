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
package toniarts.openkeeper.game.task;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Room tasks share a point when the room takes more creatures than it has
 * points, like one more trainee than the Training Room has dummies
 */
class RoomTaskSharingTest {

    @Test
    void roomTakesOneMoreCreatureThanItHasPoints() {
        TaskManager taskManager = taskManager(room(2));

        assertTrue(taskManager.assignClosestRoomTask(creature(), ObjectType.LAIR, null));
        assertTrue(taskManager.assignClosestRoomTask(creature(), ObjectType.LAIR, null));
        assertFalse(taskManager.assignClosestRoomTask(creature(), ObjectType.LAIR, null));
    }

    @Test
    void roomWithOnePlacePerPointDoesNotSharePoints() {
        TaskManager taskManager = taskManager(room(1));

        assertTrue(taskManager.assignClosestRoomTask(creature(), ObjectType.LAIR, null));
        assertFalse(taskManager.assignClosestRoomTask(creature(), ObjectType.LAIR, null));
    }

    @Test
    void casinoObjectTakesOneGambler() {
        TaskManager taskManager = taskManager(room(2));

        // DK II skips an object another gambler has (0x4D7460), the extra place in the Casino doesn't share it
        assertTrue(taskManager.assignClosestRoomTask(creature(), ObjectType.GAMBLER, null));
        assertFalse(taskManager.assignClosestRoomTask(creature(), ObjectType.GAMBLER, null));
    }

    private static TaskManager taskManager(IRoomController room) {
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" ->
                0;
            default ->
                null;
        });
        IMapController mapController = proxy(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                List.of(room);
            default ->
                null;
        });
        INavigationService navigationService = proxy(INavigationService.class, (proxy, method, args) -> new DefaultGraphPath<>());

        IGameWorldController gameWorldController = proxy(IGameWorldController.class, (proxy, method, args) -> null);

        Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);
        gameSettings.put(MiscType.CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH, CreatureSleepFixtures.variable(MiscType.CREATURE_SLEEPS_WHEN_BELOW_PERCENT_HEALTH, 50));
        for (MiscType type : new MiscType[]{MiscType.GAMBLING_MODIFY_CREATURE_MONEY_SMILES_PER_SECOND,
            MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, MiscType.GAMBLING_MODIFY_CREATURE_ANGER_SMILES_PER_SECOND,
            MiscType.GAMBLING_MODIFY_CREATURE_ANGER_MONEY_PER_SECOND, MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_SMILES_PER_SECOND,
            MiscType.GAMBLING_CREATURE_JACKPOT_CHANCE_MONEY_PER_SECOND, MiscType.GAMBLING_JACKPOT_BASIC_AMOUNT,
            MiscType.GAMBLING_JACKPOT_PAY_TIME_LIMIT, MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY}) {
            gameSettings.put(type, CasinoVariableFixtures.miscVariable(type, 1));
        }

        return new TaskManager(new DefaultEntityData(), gameWorldController, mapController, null, null, navigationService, List.of(), null, null, gameSettings);
    }

    /**
     * A room with one point to work at that takes the given number of
     * creatures
     */
    private static IRoomController room(int maxCapacity) {
        Point point = new Point(1, 1);
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.room((short) 7, 0));
        roomInstance.addCoordinate(point);
        IRoomObjectControl control = proxy(IRoomObjectControl.class, (proxy, method, args) -> switch (method.getName()) {
            case "getAvailableCoordinates" ->
                List.of(point);
            case "getMaxCapacity" ->
                maxCapacity;
            case "getCurrentCapacity" ->
                0;
            case "isFullCapacity" ->
                false;
            default ->
                null;
        });
        return proxy(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoomInstance" ->
                roomInstance;
            case "getObjectControl" ->
                control;
            case "isTileAccessible" ->
                true;
            case "isFullCapacity", "isDestroyed" ->
                false;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        });
    }

    private static ICreatureController creature() {
        return proxy(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getCreatureCoordinates" ->
                new Point(0, 0);
            case "getOwnerId" ->
                (short) 3;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
