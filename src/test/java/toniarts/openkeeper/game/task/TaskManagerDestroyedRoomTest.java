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
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A destroyed room gets no room tasks. In DK II a ruined Dungeon Heart can be
 * claimed but never stores gold again.
 */
class TaskManagerDestroyedRoomTest {

    @Test
    void impsDoNotCarryGoldToAClaimedRuinedHeart() {
        RoomInstance ruins = new RoomInstance(new Room());
        ruins.setOwnerId(Player.KEEPER1_ID);
        ruins.addCoordinate(new Point(22, 11));

        // Asking for its gold storage would mean the imp is sent there
        IRoomController heart = stub(IRoomController.class, Map.of(
                "isDestroyed", true,
                "isFullCapacity", false,
                "getRoomInstance", ruins));
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, Map.of("getWidth", 0, "getHeight", 0));
        IMapController mapController = stub(IMapController.class, Map.of(
                "getMapData", mapData,
                "getRoomsByFunction", List.of(heart)));
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
        IPlayerController player = stub(IPlayerController.class, Map.of("getKeeper", keeper));
        TaskManager taskManager = new TaskManager(new DefaultEntityData(), stub(IGameWorldController.class, Map.of()), mapController,
                stub(IObjectsController.class, Map.of()), stub(ICreaturesController.class, Map.of()), stub(INavigationService.class, Map.of()),
                List.of(player), stub(ILevelInfo.class, Map.of("getLevelData", stub(IKwdFile.class, Map.of()))), stub(IEntityPositionLookup.class, Map.of()), Map.of());
        ICreatureController imp = stub(ICreatureController.class, Map.of(
                "getOwnerId", Player.KEEPER1_ID,
                "getCreatureCoordinates", new Point(20, 11)));

        assertFalse(taskManager.assignClosestRoomTask(imp, ObjectType.GOLD, null));
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
