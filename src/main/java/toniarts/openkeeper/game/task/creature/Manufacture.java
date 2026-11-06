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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.ITrapsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerManufactureControl;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.ResearchableType;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Manufacture traps for the keeper's blueprints in the workshop. The
 * finished trap is a crate in the workshop.
 */
public final class Manufacture extends AbstractCapacityCriticalRoomTask {

    private final PlayerManufactureControl manufactureControl;
    private final ITrapsController trapsController;
    private final IObjectsController objectsController;
    private Vector2f workPlace;

    public Manufacture(final INavigationService navigationService, final IMapController mapController, Point p, short playerId, IRoomController room,
            TaskManager taskManager, PlayerManufactureControl manufactureControl, ITrapsController trapsController, IObjectsController objectsController) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.manufactureControl = manufactureControl;
        this.trapsController = trapsController;
        this.objectsController = objectsController;
    }

    @Override
    public boolean isValid(ICreatureController creature) {

        // The workshop is there and has room for another crate, without a creature only that is asked
        return super.isValid(creature)
                && (creature == null || creature.getManufacturePerSecond() > 0)
                && trapsController.getTrapToManufacture(playerId) != null;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {

        // Next to the workbench, it takes up the middle of its tile. Looked up once, the target is asked every tick
        if (workPlace == null) {
            workPlace = getAccessibleTargetNextToLocation(creature);
        }
        return workPlace;
    }

    @Override
    public boolean isFaceTarget() {
        return true;
    }

    @Override
    public Vector2f getWorkPosition(ICreatureController creature) {

        // Halfway from the middle of the tile next to the workbench to the workbench
        Vector2f target = getTarget(creature);
        if (target == null) {
            return null;
        }
        return getWorkPosition(target, getTaskLocation());
    }

    static Vector2f getWorkPosition(Vector2f nextToWorkbench, Point workbench) {
        return WorldUtils.pointToVector2f(workbench).addLocal(nextToWorkbench).multLocal(0.5f);
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.CRATE;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        // Work a second at a time, the logic runs several ticks a second
        if (executionDuration - getExecutionDuration(creature) < 1.0f) {
            return;
        }
        setExecutionDuration(creature, getExecutionDuration(creature) + 1.0f);

        Trap trap = trapsController.getTrapToManufacture(playerId);
        if (trap == null || !manufactureControl.manufacture(creature.getManufacturePerSecond(), trap.getManufToBuild())) {
            return;
        }

        // The trap is done, put its crate into the workshop
        EntityId crate = objectsController.addRoomCrate(trap.getManufCrateObjectId(), playerId, getTaskLocation().x, getTaskLocation().y,
                ResearchableType.TRAP, trap.getTrapId());
        if (getRoomObjectControl().addItem(crate, null) != null) {

            // No room for it after all
            objectsController.createController(crate).remove();
        }
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.MANUFACTURE;
    }

}
