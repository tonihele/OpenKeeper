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

import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A creature that finds no free spot in its keeper's Lair tells the keeper
 * that the Lair is too small (DKII.EXE 0x46F4F0, message 0x2A for room type 2:
 * speech 199, held 90 seconds)
 */
class TaskManagerLairTooSmallTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<Integer> speeches = new ArrayList<>();
    private final List<IRoomController> lairs = new ArrayList<>();
    private final ICreatureController creature = RoomTileWorld.stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getOwnerId" ->
            Player.KEEPER1_ID;
        case "getCreatureCoordinates" ->
            new Point(1, 1);
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });
    private double gameTime;

    @Test
    void theKeeperIsToldWhenEveryLairIsFull() {
        lairs.add(lair(true));
        lairs.add(lair(true));
        TaskManager taskManager = createTaskManager();

        assertFalse(taskManager.assignClosestRoomTask(creature, ObjectType.LAIR, null));
        assertEquals(List.of(199), speeches);

        // The message is held for 90 seconds
        gameTime = 89;
        taskManager.assignClosestRoomTask(creature, ObjectType.LAIR, null);
        assertEquals(List.of(199), speeches);
        gameTime = 90;
        taskManager.assignClosestRoomTask(creature, ObjectType.LAIR, null);
        assertEquals(List.of(199, 199), speeches);
    }

    @Test
    void aLairWithAFreeSpotIsNotTooSmall() {
        lairs.add(lair(true));
        lairs.add(lair(false));

        assertFalse(createTaskManager().assignClosestRoomTask(creature, ObjectType.LAIR, null));
        assertEquals(List.of(), speeches);
    }

    @Test
    void aKeeperWithoutALairIsNotTold() {
        assertFalse(createTaskManager().assignClosestRoomTask(creature, ObjectType.LAIR, null));
        assertEquals(List.of(), speeches);
    }

    private static IRoomController lair(boolean full) {
        Room room = DestroyRoomsFixtures.destroyRoomsRoom("Lair", (short) 11);
        DestroyRoomsFixtures.setRoomId(room, (short) 2);
        IRoomObjectControl<?> beds = RoomTileWorld.stub(IRoomObjectControl.class, (proxy, method, args) -> switch (method.getName()) {
            case "isFullCapacity" ->
                full;
            case "getAvailableCoordinates" ->
                List.of();
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        return RoomTileWorld.stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" ->
                room;
            case "getObjectControl" ->
                beds;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
    }

    private TaskManager createTaskManager() {
        PlayerService playerService = RoomTileWorld.stub(PlayerService.class, (proxy, method, args) -> {
            if (method.getName().equals("showMentorMessage")) {
                speeches.add((Integer) args[0]);
            }
            return RoomTileWorld.defaultValue(proxy, method);
        });
        RoomAttackMessages messages = new RoomAttackMessages(entityData, playerService, () -> gameTime, 4);
        IGameWorldController gameWorldController = RoomTileWorld.stub(IGameWorldController.class, (proxy, method, args)
                -> method.getName().equals("getRoomAttackMessages") ? messages : RoomTileWorld.defaultValue(proxy, method));
        IMapDataInformation<?> mapData = RoomTileWorld.stub(IMapDataInformation.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method));
        IMapController mapController = RoomTileWorld.stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                lairs;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
        IPlayerController player = RoomTileWorld.stub(IPlayerController.class, (proxy, method, args)
                -> method.getName().equals("getKeeper") ? keeper : RoomTileWorld.defaultValue(proxy, method));
        return new TaskManager(entityData, gameWorldController, mapController, null, null, null, List.of(player),
                RoomTileWorld.stub(ILevelInfo.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)), null, Map.of());
    }

}
