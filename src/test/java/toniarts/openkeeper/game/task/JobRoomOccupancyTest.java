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
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.TrainingRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.task.creature.Train;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobRoomOccupancyTest {

    private static final short KEEPER = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<MiscType, MiscVariable> gameSettings = new EnumMap<>(MiscType.class);

    @Test
    void trainingRoomCountsTheTraineesThatHaveATask() {
        TrainingRoomController room = trainingRoom();
        ICreatureController trainee = creature();
        assertEquals(0, room.getUsedCapacity(ObjectType.TRAINEE));

        Train train = train(room);
        train.assign(trainee, true);
        assertEquals(1, room.getUsedCapacity(ObjectType.TRAINEE));

        // The creature leaves, once is enough
        train.unassign(trainee);
        train.unassign(trainee);
        assertEquals(0, room.getUsedCapacity(ObjectType.TRAINEE));
    }

    @Test
    void traineeInAFullTrainingRoomKeepsItsTaskButANewcomerHasNone() {
        TrainingRoomController room = trainingRoom();
        ICreatureController trainee = creature();
        Train train = train(room);
        train.assign(trainee, true);
        assertEquals(room.getMaxCapacity(ObjectType.TRAINEE), room.getUsedCapacity(ObjectType.TRAINEE));

        assertTrue(train.isValid(trainee));
        assertFalse(train(room).isValid(creature()));
    }

    private TrainingRoomController trainingRoom() {
        TrainingRoomController room = new TrainingRoomController(entityData.createEntity(), entityData, null, roomInstance(7, 14), objectsController(), null);
        room.construct();
        return room;
    }

    /**
     * A 3x3 room with the one piece of furniture it gets
     */
    private static RoomInstance roomInstance(int roomId, int furnitureId) {
        RoomInstance roomInstance = new RoomInstance(RoomFixtures.roomWithObjects((short) roomId, (short) furnitureId, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0));
        roomInstance.setOwnerId(KEEPER);
        for (int x = 1; x <= 3; x++) {
            for (int y = 1; y <= 3; y++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        return roomInstance;
    }

    private IObjectsController objectsController() {
        return proxy(IObjectsController.class, (proxy, method, args) -> method.getName().equals("loadObject") ? entityData.createEntity() : null);
    }

    private Train train(TrainingRoomController room) {
        gameSettings.put(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, CreatureSleepFixtures.variable(MiscType.TRAINING_ROOM_MAX_EXPERIENCE_LEVEL, 10));
        gameSettings.put(MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, CreatureSleepFixtures.variable(MiscType.MODIFY_PLAYER_GOLD_WHILE_TRAINING_PER_SECOND, -1));
        Keeper keeper = new Keeper(false, KEEPER);
        keeper.setGold(100);
        IPlayerController playerController = proxy(IPlayerController.class, (proxy, method, args) -> method.getName().equals("getKeeper") ? keeper : null);
        return new Train(null, null, new Point(1, 1), KEEPER, room, taskManager(), null, gameSettings, playerController);
    }

    private ICreatureController creature() {
        EntityId creatureId = entityData.createEntity();
        return proxy(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getEntityId" ->
                creatureId;
            case "getOwnerId" ->
                KEEPER;
            case "getLevel" ->
                1;
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                null;
        });
    }

    private TaskManager taskManager() {
        IMapDataInformation<?> mapData = proxy(IMapDataInformation.class, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" ->
                0;
            default ->
                null;
        });
        IMapController mapController = proxy(IMapController.class, (proxy, method, args) -> method.getName().equals("getMapData") ? mapData : null);

        return new TaskManager(entityData, proxy(IGameWorldController.class, (proxy, method, args) -> null), mapController, null, null,
                null, List.of(),
                proxy(ILevelInfo.class, (proxy, method, args) -> method.getName().equals("getLevelData") ? proxy(IKwdFile.class, (p, m, a) -> null) : null), null,
                gameSettings);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
