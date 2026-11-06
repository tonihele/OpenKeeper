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
package toniarts.openkeeper.game.task.worker;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.Objects;
import toniarts.openkeeper.game.component.Crate;
import toniarts.openkeeper.game.component.HauledBy;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.AbstractTask;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A worker fetches a crate, from the workshop or from the floor, and carries
 * it to its blueprint. As soon as it gets there, the blueprint becomes the
 * real thing, a trap or a door.
 */
public abstract class AbstractDeliverCrateTask extends AbstractTask {

    private enum Step {
        FETCH, DELIVER
    }

    protected final EntityData entityData;
    private final IObjectsController objectsController;
    protected final EntityId blueprint;
    private final EntityId crate;
    private final Point blueprintTile;
    private final short playerId;
    private Step step = Step.FETCH;
    private boolean installed = false;

    public AbstractDeliverCrateTask(final INavigationService navigationService, final IMapController mapController, EntityData entityData,
            IObjectsController objectsController, EntityId blueprint, Point blueprintTile, EntityId crate, short playerId) {
        super(navigationService, mapController);

        this.entityData = entityData;
        this.objectsController = objectsController;
        this.blueprint = blueprint;
        this.blueprintTile = blueprintTile;
        this.crate = crate;
        this.playerId = playerId;
    }

    public EntityId getBlueprint() {
        return blueprint;
    }

    public EntityId getCrate() {
        return crate;
    }

    /**
     * Whether the blueprint still waits for its crate
     *
     * @return false if the blueprint is gone or isn't a blueprint anymore
     */
    protected abstract boolean isBlueprint();

    /**
     * The crate has arrived, the blueprint becomes the real thing
     */
    protected abstract void install();

    /**
     * The task type while the worker fetches the crate
     *
     * @return the task type
     */
    protected abstract TaskType getFetchTaskType();

    /**
     * The task type while the worker carries the crate to the blueprint
     *
     * @return the task type
     */
    protected abstract TaskType getDeliverTaskType();

    /**
     * What is done at the blueprint, for {@link #toString()}
     *
     * @return e.g. "Arming trap"
     */
    protected abstract String getDescription();

    @Override
    public Point getTaskLocation() {
        if (step == Step.FETCH) {
            Position position = entityData.getComponent(crate, Position.class);
            if (position != null) {
                return WorldUtils.vectorToPoint(position.position);
            }
        }
        return blueprintTile;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(getTaskLocation());
    }

    @Override
    public EntityId getTaskTarget() {
        return step == Step.FETCH ? crate : blueprint;
    }

    /**
     * Done, or it can't be done anymore: the crate is installed, the blueprint
     * is sold or the crate is gone. Unlike {@link #isValid}, which also says
     * no while somebody else carries the crate for a moment.
     */
    @Override
    public boolean isRemovable() {
        if (installed || entityData.getComponent(crate, Crate.class) == null) {
            return true;
        }
        return !isBlueprint();
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (isRemovable()) {
            return false;
        }

        // Somebody else may not carry our crate, and we must still carry it after picking it up
        HauledBy hauledBy = entityData.getComponent(crate, HauledBy.class);
        if (step == Step.FETCH) {
            return hauledBy == null || (creature != null && hauledBy.entityId.equals(creature.getEntityId()));
        }
        return creature == null || (hauledBy != null && hauledBy.entityId.equals(creature.getEntityId()));
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        switch (step) {
            case FETCH -> {
                pickUpCrate(creature);
                step = Step.DELIVER;
                creature.updateAssignedTask(this);
            }
            case DELIVER -> {

                // Let go of the crate first, so it isn't moved along anymore once it is gone
                IObjectController crateController = objectsController.createController(crate);
                crateController.setHaulable(null);
                install();
                crateController.remove();
                installed = true;
            }
        }
    }

    private void pickUpCrate(ICreatureController creature) {

        // Out of the workshop storage, the crates left in the stack settle
        Stored stored = entityData.getComponent(crate, Stored.class);
        if (stored != null) {
            IRoomController room = mapController.getRoomControllerByCoordinates(getTaskLocation());
            if (room != null && room.getObjectControl(stored.objectType) != null) {
                room.getObjectControl(stored.objectType).removeItem(crate);
            } else {
                entityData.removeComponent(crate, Stored.class);
            }
        }
        objectsController.createController(crate).setHaulable(creature);
    }

    @Override
    public void unassign(ICreatureController creature) {
        super.unassign(creature);

        // The crate is dropped where the worker is, somebody can fetch it from there
        if (step != Step.FETCH && !installed) {
            HauledBy hauledBy = entityData.getComponent(crate, HauledBy.class);
            if (hauledBy != null && hauledBy.entityId.equals(creature.getEntityId())) {
                objectsController.createController(crate).setHaulable(null);
            }
            step = Step.FETCH;
        }
    }

    @Override
    public TaskType getTaskType() {
        return switch (step) {
            case FETCH ->
                getFetchTaskType();
            case DELIVER ->
                getDeliverTaskType();
        };
    }

    @Override
    public String toString() {
        return getDescription() + " at " + blueprintTile + " with crate " + crate + " (" + step + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(blueprint, playerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final AbstractDeliverCrateTask other = (AbstractDeliverCrateTask) obj;
        return playerId == other.playerId && Objects.equals(blueprint, other.blueprint);
    }

}
