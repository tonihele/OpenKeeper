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
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.KeeperComponent;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.RoomAttackMessages;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerGoldControl;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
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
    private final Room casino = room("Casino", (short) 15);
    private final Room trainingRoom = room("Training Room", (short) 7);
    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private List<Room> buildable = List.of();
    private Map<MiscType, MiscVariable> settings = Map.of();
    private final ICreatureController creature = RoomTileWorld.stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getOwnerId" ->
            Player.KEEPER1_ID;
        case "getCreatureCoordinates" ->
            new Point(1, 1);
        case "getGold" ->
            100;
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
    void aCreatureThatWantsToGambleAsksForACasino() {

        // DK II's Casino finder 0x4D75F0: speech 1508 with none, 205 with every Casino full
        buildable = List.of(casino);
        settings = Map.of(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND,
                CreatureSleepFixtures.variable(MiscType.GAMBLING_MODIFY_CREATURE_MONEY_MONEY_PER_SECOND, -10));
        assertFalse(createTaskManager().assignTask(creature, Creature.JobType.DRINK));
        assertEquals(List.of(1508), speeches);

        libraries.add(jobRoom(casino, true));
        assertFalse(createTaskManager().assignTask(creature, Creature.JobType.DRINK));
        assertEquals(List.of(1508, 205), speeches);
    }

    @Test
    void aKeeperWithTooLittleGoldForTrainingIsToldItCanNotAffordIt() {

        // DK II's training state 0x47B2D0: message 0x37 variant 5, speech 1343, held 240 seconds
        TaskManager taskManager = trainingTaskManager(99);

        assertFalse(taskManager.assignTask(creature, Creature.JobType.TRAIN));
        assertEquals(List.of(1343), speeches);
        gameTime = 239;
        taskManager.assignTask(creature, Creature.JobType.TRAIN);
        assertEquals(List.of(1343), speeches);
        gameTime = 240;
        taskManager.assignTask(creature, Creature.JobType.TRAIN);
        assertEquals(List.of(1343, 1343), speeches);
    }

    @Test
    void aKeeperThatCanPayForTrainingOrHasNoRoomForItIsNotToldItCanNotAffordIt() {
        assertFalse(trainingTaskManager(100).assignTask(creature, Creature.JobType.TRAIN));
        assertEquals(List.of(), speeches);

        // With the Training Room full the keeper hears that it is too small instead
        libraries.clear();
        libraries.add(jobRoom(trainingRoom, true));
        assertFalse(trainingTaskManager(0).assignTask(creature, Creature.JobType.TRAIN));
        assertEquals(List.of(201), speeches);
    }

    private TaskManager trainingTaskManager(int gold) {
        settings = Map.of(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, CreatureSleepFixtures.variable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, 4),
                MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, CreatureSleepFixtures.variable(MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, -100));
        if (libraries.isEmpty()) {
            libraries.add(jobRoom(trainingRoom, false));
        }
        entityData.setComponents(entityData.createEntity(), new KeeperComponent(Player.KEEPER1_ID), new Gold(gold, Integer.MAX_VALUE));
        return createTaskManager();
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
        return jobRoom(library, full);
    }

    private IRoomController jobRoom(Room type, boolean full) {
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
                type;
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
            case "getGoldControl" ->
                new PlayerGoldControl(keeper, entityData);
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        return new TaskManager(entityData, gameWorldController, mapController, null, null, null, List.of(player),
                RoomTileWorld.stub(ILevelInfo.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)), null, settings);
    }

}
