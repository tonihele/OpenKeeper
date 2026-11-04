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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractCapacityCriticalRoomTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The Torture job, the happy job of the Mistress. As in the original (job 7,
 * 0x483AE0 and 0x483B90), the creature whips a victim on a torture device
 * if there is one (0x483FF0), otherwise it gets on a free device and is
 * tortured itself, as it likes (0x484110, 0x484590).
 */
public final class Torture extends AbstractCapacityCriticalRoomTask {

    private final EntityData entityData;
    private final boolean whip;

    /**
     * Torturing at a torture device
     *
     * @param navigationService the navigation service
     * @param mapController the map controller
     * @param p the torture device
     * @param playerId the owner of the torture chamber
     * @param room the torture chamber
     * @param taskManager the task manager
     * @param entityData the entity data
     */
    public Torture(final INavigationService navigationService, final IMapController mapController, Point p, short playerId,
            IRoomController room, TaskManager taskManager, EntityData entityData) {
        super(navigationService, mapController, p, playerId, room, taskManager);

        this.entityData = entityData;
        this.whip = getVictim() != null;
    }

    @Override
    public boolean isValid(ICreatureController creature) {

        // The whip goes on while the victim is there, a creature on a device gets off when a victim is put on it.
        // A room full of victims still has some to whip
        return !room.isRemoved() && room.getOwnerId() == playerId && !room.isDestroyed()
                && whip == (getVictim() != null) && (whip || getDevice() != null);
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return whip ? getAccessibleTargetNextToLocation(creature) : WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public boolean isFaceTarget() {
        return whip;
    }

    /**
     * The victim the creature whips, or the device it is tortured on
     *
     * @return the victim or the device
     */
    @Override
    public EntityId getTaskTarget() {
        return whip ? getVictim() : getDevice();
    }

    @Override
    protected ObjectType getRoomObjectType() {
        return ObjectType.TORTUREE;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // Nothing changes, the creature just likes it
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.TORTURE;
    }

    @Override
    public boolean isRemovable() {
        return true;
    }

    private EntityId getVictim() {
        Collection<EntityId> victims = getRoomObjectControl().getItems(getTaskLocation());
        return victims == null || victims.isEmpty() ? null : victims.iterator().next();
    }

    private EntityId getDevice() {
        Set<EntityId> devices = new HashSet<>(room.getFloorFurniture());
        devices.addAll(room.getWallFurniture());
        for (EntityId device : devices) {
            Position position = entityData.getComponent(device, Position.class);
            if (position != null && getTaskLocation().equals(WorldUtils.vectorToPoint(position.position))) {
                return device;
            }
        }
        return null;
    }

}
