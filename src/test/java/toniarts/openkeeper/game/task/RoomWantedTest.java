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

import com.simsilica.es.base.DefaultEntityData;
import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.RoomWanted;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.CasinoVariableFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A creature that looks for a room its keeper has none of tells its keeper,
 * as DK II's room searches post the "room wanted" message (0x29)
 */
class RoomWantedTest {

    private final TaskManager taskManager = new TaskManager(new DefaultEntityData(), stub(IGameWorldController.class, Map.of()),
            stub(IMapController.class, Map.of("getMapData", stub(IMapDataInformation.class, Map.of("getWidth", 0, "getHeight", 0)),
                    "getRoomsByFunction", List.of())),
            stub(IObjectsController.class, Map.of()), stub(ICreaturesController.class, Map.of()), stub(INavigationService.class, Map.of()),
            List.of(stub(IPlayerController.class, Map.of("getKeeper", new Keeper(false, Player.KEEPER1_ID)))),
            stub(ILevelInfo.class, Map.of("getLevelData", stub(IKwdFile.class, Map.of()))), stub(IEntityPositionLookup.class, Map.of()),
            Map.of(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND,
                    CasinoVariableFixtures.miscVariable(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, -8)));
    private final List<RoomWanted> asked = new ArrayList<>();
    private final EventListener<RoomWanted> listener = (type, event) -> asked.add(event);

    @BeforeEach
    void listen() {
        EventBus.addListener(ServerEvents.ROOM_WANTED, listener);
    }

    @AfterEach
    void stopListening() {
        EventBus.removeListener(ServerEvents.ROOM_WANTED, listener);
    }

    @Test
    void aCreatureAsksForTheRoomOfItsJobAndForAHatcheryWhenItsKeeperHasNone() {
        ICreatureController creature = creature(0);

        assertFalse(taskManager.isTaskAvailable(creature, Creature.JobType.TRAIN));
        assertFalse(taskManager.assignEatTask(creature));

        assertEquals(List.of(new RoomWanted(Player.KEEPER1_ID, ObjectType.TRAINEE), new RoomWanted(Player.KEEPER1_ID, ObjectType.FOOD)), asked);
    }

    @Test
    void aCreatureAsksForACasinoOnlyWithMoreGoldThanTheStake() {

        // DK II's Drink job (0x4560F0) looks at the gold held before it looks for a Casino. The stake here is 8
        assertFalse(taskManager.isTaskAvailable(creature(8), Creature.JobType.DRINK));
        assertFalse(taskManager.isTaskAvailable(creature(9), Creature.JobType.DRINK));

        assertEquals(List.of(new RoomWanted(Player.KEEPER1_ID, ObjectType.GAMBLER)), asked);
    }

    private static ICreatureController creature(int gold) {
        return stub(ICreatureController.class, Map.of(
                "getOwnerId", Player.KEEPER1_ID,
                "getCreatureCoordinates", new Point(5, 5),
                "getGold", gold));
    }

    /**
     * Answers the given methods, void methods do nothing and anything else
     * fails the test
     */
    private static <T> T stub(Class<T> type, Map<String, Object> answers) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (answers.containsKey(method.getName())) {
                return answers.get(method.getName());
            }
            return switch (method.getName()) {
                case "hashCode" ->
                    System.identityHashCode(proxy);
                case "equals" ->
                    proxy == args[0];
                case "toString" ->
                    type.getSimpleName();
                default -> {
                    if (method.getReturnType() == void.class) {
                        yield null;
                    }
                    throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
                }
            };
        }));
    }

}
