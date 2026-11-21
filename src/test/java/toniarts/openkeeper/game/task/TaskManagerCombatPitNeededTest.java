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
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.CombatExperienceFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.DestroyRoomsFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A creature that would train but is too experienced for the Training Room,
 * and not yet at the Combat Pit's top level, has its keeper told that a Combat
 * Pit is needed (DKII.EXE 0x485240 and 0x4D8870, message 0x29 for room type
 * 16: speech 1510)
 */
class TaskManagerCombatPitNeededTest {

    private static final int TRAINING_ROOM_MAX_LEVEL = 4;
    private static final int COMBAT_PIT_MAX_LEVEL = 8;

    private final EntityData entityData = new DefaultEntityData();
    private final List<Integer> speeches = new ArrayList<>();
    private final List<IRoomController> rooms = new ArrayList<>();
    private final Room combatPit = DestroyRoomsFixtures.destroyRoomsRoom("Combat Pit", (short) 25, Room.RoomFlag.BUILDABLE);
    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private final PlayerRoomControl roomControl;
    private int level = TRAINING_ROOM_MAX_LEVEL;
    private final ICreatureController creature = RoomTileWorld.stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getOwnerId" ->
            Player.KEEPER1_ID;
        case "getCreatureCoordinates" ->
            new Point(1, 1);
        case "getLevel" ->
            level;
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });

    TaskManagerCombatPitNeededTest() {
        DestroyRoomsFixtures.setRoomId(combatPit, (short) 16);
        roomControl = new PlayerRoomControl(keeper, List.of(combatPit));
        roomControl.setTypeAvailable(combatPit, true, true);
    }

    @Test
    void theKeeperIsToldWhenACreaturePastTheTrainingRoomWouldTrain() {
        assertFalse(createTaskManager().isTaskAvailable(creature, Creature.JobType.TRAIN));

        assertEquals(List.of(1510), speeches);
    }

    @Test
    void creaturesThatCanStillTrainOrAreAtThePitsTopLevelDoNotAsk() {
        TaskManager taskManager = createTaskManager();

        level = TRAINING_ROOM_MAX_LEVEL - 1;
        taskManager.isTaskAvailable(creature, Creature.JobType.TRAIN);
        level = COMBAT_PIT_MAX_LEVEL;
        taskManager.isTaskAvailable(creature, Creature.JobType.TRAIN);

        assertEquals(List.of(), speeches);
    }

    @Test
    void aKeeperWithACombatPitIsNotTold() {
        rooms.add(RoomTileWorld.stub(IRoomController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getRoom" ->
                combatPit;
            case "getOwnerId" ->
                Player.KEEPER1_ID;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        }));

        createTaskManager().isTaskAvailable(creature, Creature.JobType.TRAIN);

        assertEquals(List.of(), speeches);
    }

    @Test
    void aKeeperThatCanNotBuildACombatPitIsNotTold() {
        roomControl.setTypeAvailable(combatPit, false, false);

        createTaskManager().isTaskAvailable(creature, Creature.JobType.TRAIN);

        assertEquals(List.of(), speeches);
    }

    private TaskManager createTaskManager() {
        PlayerService playerService = RoomTileWorld.stub(PlayerService.class, (proxy, method, args) -> {
            if (method.getName().equals("showMentorMessage")) {
                speeches.add((Integer) args[0]);
            }
            return RoomTileWorld.defaultValue(proxy, method);
        });
        RoomAttackMessages messages = new RoomAttackMessages(entityData, playerService, () -> 0, 4);
        IGameWorldController gameWorldController = RoomTileWorld.stub(IGameWorldController.class, (proxy, method, args)
                -> method.getName().equals("getRoomAttackMessages") ? messages : RoomTileWorld.defaultValue(proxy, method));
        IMapDataInformation<?> mapData = RoomTileWorld.stub(IMapDataInformation.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method));
        IMapController mapController = RoomTileWorld.stub(IMapController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getRoomsByFunction" ->
                List.of();
            case "getRoomControllers" ->
                rooms;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        IPlayerController player = RoomTileWorld.stub(IPlayerController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" ->
                keeper;
            case "getRoomControl" ->
                roomControl;
            default ->
                RoomTileWorld.defaultValue(proxy, method);
        });
        return new TaskManager(entityData, gameWorldController, mapController, null, null, null, List.of(player),
                RoomTileWorld.stub(ILevelInfo.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)), null,
                Map.of(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, CombatExperienceFixtures.miscVariable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, TRAINING_ROOM_MAX_LEVEL),
                        MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, CombatExperienceFixtures.miscVariable(MiscType.COMBAT_PIT_MAX_EXPERIENCE_LEVEL, COMBAT_PIT_MAX_LEVEL)));
    }

}
