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
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Placeable;
import toniarts.openkeeper.game.component.RoomStorage;
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
public class ObjectController extends EntityController implements IObjectController {

    private final GameObject object;

    public ObjectController(EntityId entityId, EntityData entityData, GameObject object,
            IObjectsController objectsController, IMapController mapController) {
        super(entityId, entityData, objectsController, mapController);

        this.object = object;
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
        return entityData.getComponent(entityId, RoomStorage.class) != null;
    }

    @Override
    public boolean isPickableByPlayerCreature(short playerId) {
        return entityData.getComponent(entityId, Placeable.class) != null && getTile().getOwnerId() == playerId;
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
