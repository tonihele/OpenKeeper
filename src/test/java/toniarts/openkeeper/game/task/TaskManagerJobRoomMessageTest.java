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
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A creature that wants a job and finds no room for it tells its keeper that
 * the room is needed (DKII.EXE message 0x29: Library speech 1506, Temple
 * 1511) or too small (message 0x2A: Library speech 210), each held 300
 * seconds
 */
class TaskManagerJobRoomMessageTest {

    private final EntityData entityData = new DefaultEntityData();
    private final List<Integer> speeches = new ArrayList<>();
    private final List<IRoomController> libraries = new ArrayList<>();
    private final Room library = room("Library", (short) 6);
    private final Room temple = room("Temple", (short) 13);
    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private List<Room> buildable = List.of();
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
    void aKeeperThatCanBuildTheRoomIsToldItNeedsOne() {
        buildable = List.of(library);
        TaskManager taskManager = createTaskManager();

        assertFalse(taskManager.assignTask(creature, Creature.JobType.RESEARCH));
        assertEquals(List.of(1506), speeches);

        // The message is held for 300 seconds
        gameTime = 299;
        taskManager.assignTask(creature, Creature.JobType.RESEARCH);
        assertEquals(List.of(1506), speeches);
        gameTime = 300;
        taskManager.assignTask(creature, Creature.JobType.RESEARCH);
        assertEquals(List.of(1506, 1506), speeches);
    }

    @Test
    void aKeeperThatCanNotBuildTheRoomIsNotTold() {
        assertFalse(createTaskManager().assignTask(creature, Creature.JobType.PRAY));
        assertEquals(List.of(), speeches);

        buildable = List.of(temple);
        assertFalse(createTaskManager().assignTask(creature, Creature.JobType.PRAY));
        assertEquals(List.of(1511), speeches);
    }

    @Test
    void theKeeperIsToldWhenEveryRoomOfTheTypeIsFull() {
        libraries.add(library(true));
        libraries.add(library(true));

        assertFalse(createTaskManager().assignTask(creature, Creature.JobType.RESEARCH));
        assertEquals(List.of(210), speeches);
    }

    @Test
    void aRoomWithSpaceLeftIsNotTooSmall() {
        libraries.add(library(true));
        libraries.add(library(false));

        assertFalse(createTaskManager().assignTask(creature, Creature.JobType.RESEARCH));
        assertEquals(List.of(), speeches);
    }

    @Test
    void otherJobsTellNothing() {
        assertFalse(createTaskManager().assignTask(creature, Creature.JobType.MANUFACTURE));
        assertEquals(List.of(), speeches);
    }

    private static Room room(String name, short roomId) {
        Room room = DestroyRoomsFixtures.destroyRoomsRoom(name, (short) 20, Room.RoomFlag.BUILDABLE);
        DestroyRoomsFixtures.setRoomId(room, roomId);
        return room;
    }

    private IRoomController library(boolean full) {
        IRoomObjectControl<?> bookCases = RoomTileWorld.stub(IRoomObjectControl.class, (proxy, method, args) -> switch (method.getName()) {
            case "isFullCapacity" ->
                full;
            case "getAvailableCoordinates" ->
                List.of();
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        return RoomTileWorld.stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" ->
                library;
            case "getObjectControl" ->
                bookCases;
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
                libraries;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        PlayerRoomControl roomControl = new PlayerRoomControl(keeper, buildable);
        for (Room room : buildable) {
            roomControl.setTypeAvailable(room, true, true);
        }
        IPlayerController player = RoomTileWorld.stub(IPlayerController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" ->
                keeper;
            case "getRoomControl" ->
                roomControl;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        return new TaskManager(entityData, gameWorldController, mapController, null, null, null, List.of(player),
                RoomTileWorld.stub(ILevelInfo.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)), null, Map.of());
    }

}
