/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.Objects;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.game.task.TaskManager;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A task for creatures to rescue a fellow comrade from the harsh world
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class RescueCreatureTask extends AbstractTileTask {

    private final ICreatureController creature;
    private final TaskManager taskManager;

    public RescueCreatureTask(final TaskManager taskManager, final INavigationService navigationService, final IMapController mapController, ICreatureController creature, short playerId) {
        super(navigationService, mapController, WorldUtils.vectorToPoint(creature.getPosition()), playerId);
        this.creature = creature;
        this.taskManager = taskManager;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation()); // FIXME 0.5f not needed?
    }

    @Override
    public Point getTaskLocation() {
        Vector3f pos = creature.getPosition();
        if (pos != null) {
            return WorldUtils.vectorToPoint(creature.getPosition());
        }
        return null;
    }

    /**
     * Like in DK II, a worker rescues a knocked out creature of its keeper
     * that lies on the keeper's land, if it has a lair or there is room for
     * one in a Lair (DKII.EXE 0x46E390, 0x46D810, 0x46E610)
     */
    @Override
    public boolean isValid(ICreatureController creature) {
        Point location = getTaskLocation();
        return this.creature.isUnconscious() && !this.creature.isDragged() && !this.creature.isRecuperating()
                && location != null && mapController.getMapData().getTile(location).getOwnerId() == playerId
                && getLairLocation() != null;
    }

    /**
     * Where the creature is carried to: its own lair, without one the closest
     * free spot in a Lair (DKII.EXE 0x46E610)
     *
     * @return the tile to carry to, {@code null} if there is none
     */
    private Point getLairLocation() {
        if (creature.hasLair()) {
            return creature.getLairLocation();
        }
        Point location = getTaskLocation();
        Point closest = null;
        for (IRoomController room : mapController.getRoomsByFunction(ObjectType.LAIR, playerId)) {
            IRoomObjectControl<?> lairs = room.getObjectControl(ObjectType.LAIR);
            for (Point p : lairs.getAvailableCoordinates()) {
                if (closest == null || WorldUtils.calculateDistance(location, p) < WorldUtils.calculateDistance(location, closest)) {
                    closest = p;
                }
            }
        }
        return closest;
    }

    @Override
    public boolean isRemovable() {
        return !this.creature.isUnconscious();
    }

    @Override
    public String toString() {
        return "Rescuing a creature at " + getTaskLocation();
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // A worker gets up as soon as it is picked up (DKII.EXE 0x468420)
        if (this.creature.isWorker()) {
            this.creature.wakeUp();
            return;
        }
        this.creature.setHaulable(creature);

        // Assign carry to lair
        CarryCreatureToLairTask task = new CarryCreatureToLairTask(navigationService, mapController, this.creature, getLairLocation(), playerId);
        taskManager.addTask(playerId, task);
        task.assign(creature, true);
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.RESCUE_CREATURE;
    }

    @Override
    public EntityId getTaskTarget() {
        return creature.getEntityId();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + Objects.hashCode(this.creature);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RescueCreatureTask other = (RescueCreatureTask) obj;
        if (!Objects.equals(this.creature, other.creature)) {
            return false;
        }
        return true;
    }

}
