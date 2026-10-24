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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.hero.RoomTileWorld;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The task manager gives a hero that destroys rooms the job of wrecking the
 * room tile he stepped on, but only when it is a keeper's room he may wreck
 */
class TaskManagerDestroyRoomTaskTest {

    private final EntityData entityData = new DefaultEntityData();
    private final RoomTileWorld world = new RoomTileWorld();
    private final AtomicReference<Task> assignedTask = new AtomicReference<>();
    private final ICreatureController hero = RoomTileWorld.stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
        case "getOwnerId" ->
            Player.GOOD_PLAYER_ID;
        case "setAssignedTask" -> {
            assignedTask.set((Task) args[0]);
            yield null;
        }
        default ->
            RoomTileWorld.defaultValue(proxy, method);
    });
    private final TaskManager taskManager = createTaskManager();

    @Test
    void assignsTheRoomTileUnderTheHero() {
        assertTrue(taskManager.assignDestroyRoomTask(hero, RoomTileWorld.TILE));

        Task task = assignedTask.get();
        assertEquals(TaskType.DESTROY_ROOM_TILE, task.getTaskType());
        assertEquals(RoomTileWorld.TILE, task.getTaskLocation());
        assertSame(task, taskManager.getTaskById(task.getId()));
    }

    @Test
    void doesNotAssignTilesTheHeroMayNotWreck() {
        world.ownerId = Player.GOOD_PLAYER_ID;

        assertFalse(taskManager.assignDestroyRoomTask(hero, RoomTileWorld.TILE));
        assertNull(assignedTask.get());
    }

    private TaskManager createTaskManager() {
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
        IPlayerController player = RoomTileWorld.stub(IPlayerController.class, (proxy, method, args) -> method.getName().equals("getKeeper") ? keeper : RoomTileWorld.defaultValue(proxy, method));
        return new TaskManager(entityData, world.gameWorldController, world.mapController,
                RoomTileWorld.stub(IObjectsController.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                RoomTileWorld.stub(ICreaturesController.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                RoomTileWorld.stub(INavigationService.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                List.of(player),
                RoomTileWorld.stub(ILevelInfo.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                RoomTileWorld.stub(IEntityPositionLookup.class, (proxy, method, args) -> RoomTileWorld.defaultValue(proxy, method)),
                world.gameSettings);
    }

}
