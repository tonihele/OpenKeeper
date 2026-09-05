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
package toniarts.openkeeper.game.controller.room.storage;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Controls creature lairs in a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomLairControl extends AbstractRoomObjectControl<EntityId> {

    private final IObjectsController objectsController;

    public RoomLairControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer, IObjectsController objectsController) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.LAIR);

        this.objectsController = objectsController;
    }

    @Override
    public EntityId addItem(EntityId creature, Point p) {
        Collection<EntityId> objects = objectsByCoordinate.get(p);
        if (objects != null && !objects.isEmpty()) {
            return objects.iterator().next(); // Already a lair here
        }

        // FIXME: KWD stuff should not be used anymore in this level, all data must be in in-game objects
        Owner owner = entityData.getComponent(creature, Owner.class);
        CreatureComponent creatureComponent = entityData.getComponent(creature, CreatureComponent.class);
        EntityId object = objectsController.loadObject(kwdFile.getCreature(creatureComponent.creatureId).getLairObjectId(), owner.ownerId, p.x, p.y);
        if (objects == null) {
            objects = new ArrayList<>(1);
        }
        objects.add(object);
        objectsByCoordinate.put(p, objects);
        setRoomStorageToItem(object, true);
        addCurrentCapacity(1);

        return object;
    }

    /**
     * Adds a lair object that already exists. Used when room topology changes
     * so creatures can retain the same lair entity.
     *
     * @param object the existing lair object
     * @param p its tile
     */
    public void addExistingItem(EntityId object, Point p) {
        Collection<EntityId> objects = objectsByCoordinate.computeIfAbsent(p, key -> new ArrayList<>(1));
        if (!objects.contains(object)) {
            objects.add(object);
            setRoomStorageToItem(object, false);
            addCurrentCapacity(1);
        }
    }

    /**
     * Detaches all lairs without deleting them. The caller must either attach
     * them to a resulting room or delete them after rebuilding room topology.
     *
     * @return detached lair entities
     */
    public Collection<EntityId> detachAllItems() {
        List<EntityId> objects = new ArrayList<>();
        for (Collection<EntityId> coordinateObjects : objectsByCoordinate.values()) {
            objects.addAll(coordinateObjects);
        }
        objectsByCoordinate.clear();
        for (EntityId object : objects) {
            entityData.removeComponent(object, Stored.class);
        }
        addCurrentCapacity(-objects.size());
        return objects;
    }

    @Override
    public void destroy() {
        super.destroy();

        // Just release all the lairs
        removeAllObjects();
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);
        addCurrentCapacity(-1);

        // Lairs get removed for real
        entityData.removeEntity(object);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

}
