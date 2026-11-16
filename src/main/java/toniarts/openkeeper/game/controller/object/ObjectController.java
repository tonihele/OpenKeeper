/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.controller.object;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.filter.FieldFilter;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.FurnitureUse;
import toniarts.openkeeper.game.component.InHand;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.data.FurnitureAnimationPolicy;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.tools.convert.map.GameObject;

/**
 * Controller for object type entities
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ObjectController extends EntityController implements IObjectController {

    private final GameObject object;

    public ObjectController(EntityId entityId, EntityData entityData, GameObject object,
            IObjectsController objectsController, IMapController mapController) {
        super(entityId, entityData, objectsController, mapController);

        this.object = object;
    }

    @Override
    public void beginUse(EntityId creatureId) {
        Owner userOwner = entityData.getComponent(creatureId, Owner.class);
        if (entityData.getComponent(entityId, ObjectViewState.class) == null
                || entityData.getComponent(entityId, Position.class) == null
                || entityData.getComponent(entityId, Death.class) != null
                || entityData.getComponent(creatureId, Position.class) == null
                || entityData.getComponent(creatureId, Death.class) != null
                || entityData.getComponent(creatureId, InHand.class) != null
                || userOwner == null) {
            return;
        }
        FurnitureUse use = entityData.getComponent(creatureId, FurnitureUse.class);
        if (use != null && entityId.equals(use.objectEntityId)) {
            return;
        }
        if (use != null) {
            if (entityData.getComponent(use.objectEntityId, ObjectComponent.class) != null) {
                objectsController.createController(use.objectEntityId).endUse(creatureId);
            } else {
                entityData.removeComponent(creatureId, FurnitureUse.class);
            }
        }
        entityData.setComponent(creatureId, new FurnitureUse(entityId));
        updateUsePresentation(true);
    }

    @Override
    public void endUse(EntityId creatureId) {
        FurnitureUse use = entityData.getComponent(creatureId, FurnitureUse.class);
        if (use != null && !entityId.equals(use.objectEntityId)) {
            return;
        }
        // Task cleanup may run after the creature itself was removed.
        entityData.removeComponent(creatureId, FurnitureUse.class);
        boolean inUse = entityData.findEntity(new FieldFilter<>(FurnitureUse.class,
                "objectEntityId", entityId), FurnitureUse.class) != null;
        updateUsePresentation(inUse);
    }

    private void updateUsePresentation(boolean inUse) {
        ObjectViewState view = entityData.getComponent(entityId, ObjectViewState.class);
        // Other furniture keeps its current presentation until its native policy is known.
        if (view == null || !FurnitureAnimationPolicy.pausesWhenIdle(view.objectId)
                || entityData.getComponent(entityId, Death.class) != null) {
            return;
        }
        GameObject.State state = inUse ? GameObject.State.ACTIVATE_ANIM : GameObject.State.NONE;
        if (view.state != state) {
            entityData.setComponent(entityId, new ObjectViewState(view.objectId, state,
                    view.animState, view.visible, view.lairCreatureTypeId));
        }
    }

    @Override
    public short getObjectId() {
        return object.getObjectId();
    }

    @Override
    public int getPickUpPriority() {
        return object.getPickUpPriority();
    }

    @Override
    public AbstractRoomController.ObjectType getType() {
        return entityData.getComponent(entityId, ObjectComponent.class).objectType;
    }

    @Override
    public boolean isStoredInRoom() {
        return entityData.getComponent(entityId, Stored.class) != null;
    }

    @Override
    public boolean isPickableByPlayerCreature(short playerId) {
        if (entityData.getComponent(entityId, Placeable.class) == null) {
            return false;
        }

        // In the keeper's hand the object has no position, the creatures can't get to it
        return getPosition() != null && getTile().getOwnerId() == playerId;
    }

    @Override
    public boolean isHaulable() {
        return entityData.getComponent(entityId, Placeable.class) != null && getType() != AbstractRoomController.ObjectType.GOLD;
    }

    @Override
    public boolean creaturePicksUp(ICreatureController creature) {
        if (isHaulable()) {
            setHaulable(creature);
            return false;
        } else {
            creature.giveObject(this);
            return true;
        }
    }

}
