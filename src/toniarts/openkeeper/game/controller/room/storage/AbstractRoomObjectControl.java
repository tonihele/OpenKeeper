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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Storage;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ObjectsController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Room object controller. FIXME: Cache the coordinates and listen to changes in
 * rooms
 *
 * @param <V> the value type to add
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractRoomObjectControl<V> implements IRoomObjectControl<V> {

    protected final KwdFile kwdFile;
    protected final IRoomController parent;
    protected final EntityData entityData;
    private final IGameTimer gameTimer;
    protected final Map<Point, Collection<EntityId>> objectsByCoordinate = new HashMap<>();
    private final EntityId entityId;

    public AbstractRoomObjectControl(KwdFile kwdFile, IRoomController parent, EntityData entityData,
            IGameTimer gameTimer, AbstractRoomController.ObjectType objectType) {
        this.kwdFile = kwdFile;
        this.parent = parent;
        this.entityData = entityData;
        this.gameTimer = gameTimer;

        entityId = entityData.createEntity();
        entityData.setComponent(entityId, new Storage(parent.getEntityId(), objectType, 0, calculateMaxCapacity()));
    }

    protected abstract int getObjectsPerTile();

    protected abstract int getNumberOfAccessibleTiles();

    @Override
    public void destroy() {
        entityData.removeEntity(entityId);
    }

    /**
     * Get a room objects
     *
     * @param p the object from point
     * @return the objects in given point
     */
    @Override
    public Collection<EntityId> getItems(Point p) {
        return objectsByCoordinate.get(p);
    }

    /**
     * Get the max capacity of the room
     *
     * @return max capacity in number of objects
     */
    @Override
    public final int getMaxCapacity() {
        return entityData.getComponent(entityId, Storage.class).maxCapacity;
    }

    protected int calculateMaxCapacity() {
        return getObjectsPerTile() * getNumberOfAccessibleTiles();
    }

    /**
     * Is the room at full capacity
     *
     * @return max capacity used
     */
    @Override
    public boolean isFullCapacity() {
        return getCurrentCapacity() >= getMaxCapacity();
    }

    @Override
    public final int getCurrentCapacity() {
        return entityData.getComponent(entityId, Storage.class).currentCapacity;
    }

    @Override
    public final AbstractRoomController.ObjectType getObjectType() {
        return entityData.getComponent(entityId, Storage.class).objectType;
    }

    protected final void addCurrentCapacity(int amount) {
        Storage currentStorage = entityData.getComponent(entityId, Storage.class);
        entityData.setComponent(entityId, new Storage(currentStorage.room, currentStorage.objectType, currentStorage.currentCapacity + amount, currentStorage.maxCapacity));
    }

    /**
     * Remove an item
     *
     * @param object the object
     */
    @Override
    public void removeItem(EntityId object) {
        entityData.removeComponent(object, Stored.class);
        addNoRoomDecay(object);
        for (Collection<EntityId> objects : objectsByCoordinate.values()) {
            if (objects.remove(object)) {
                break;
            }
        }
    }

    /**
     * Removes all objects (for real)
     */
    protected void removeAllObjects() {
        List<Collection<EntityId>> objectList = new ArrayList<>(objectsByCoordinate.values());
        for (Collection<EntityId> objects : objectList) {
            for (EntityId obj : objects) {
                entityData.removeEntity(obj);
            }
        }
    }

    /**
     * Gets all coordinates, coordinates that can handle the objects
     *
     * @return list of all coordinates
     */
    protected Collection<Point> getCoordinates() {
        List<Point> coordinates = new ArrayList<>(parent.getRoomInstance().getCoordinates());
        Iterator<Point> iter = coordinates.iterator();
        while (iter.hasNext()) {
            Point p = iter.next();
            if (!parent.isTileAccessible(null, p)) {
                iter.remove();
            }
        }
        return coordinates;
    }

    /**
     * Gets available coordinates
     *
     * @return list of available coordinates
     */
    @Override
    public Collection<Point> getAvailableCoordinates() {
        List<Point> coordinates = new ArrayList<>(getCoordinates());
        Iterator<Point> iter = coordinates.iterator();
        while (iter.hasNext()) {
            Point p = iter.next();
            Collection<EntityId> items = getItems(p);
            if (items != null && items.size() == getObjectsPerTile()) {
                iter.remove();
            }
        }
        return coordinates;
    }

    protected void setRoomStorageToItem(EntityId entityId, boolean changeOwner) {
        entityData.setComponent(entityId, new Stored(parent.getEntityId(), getObjectType()));
        ObjectComponent objectComponent = entityData.getComponent(entityId, ObjectComponent.class);
        if (objectComponent != null && kwdFile.getObject(objectComponent.objectId).getFlags().contains(GameObject.ObjectFlag.DIE_OVER_TIME_IF_NOT_IN_ROOM)) {
            entityData.removeComponent(entityId, Decay.class);
        }

        // Also set the owner if there is one already
        changeEntityOwner(entityId, parent.getOwnerId(), changeOwner);
    }

    @Override
    public void captured(short playerId) {

        // The new owner shall also own the riches we hold
        List<Collection<EntityId>> objectList = new ArrayList<>(objectsByCoordinate.values());
        for (Collection<EntityId> objects : objectList) {
            for (EntityId obj : objects) {
                changeEntityOwner(obj, playerId, true);
            }
        }
    }

    private void changeEntityOwner(EntityId entity, short playerId, boolean changeOwner) {
        Owner owner = entityData.getComponent(entity, Owner.class);
        if (owner != null && (owner.ownerId != playerId || owner.controlId != playerId)) {
            entityData.setComponent(entity, new Owner(changeOwner ? playerId : owner.ownerId, playerId));
        }
    }

    /**
     * Adds decay for the object for not being in the room if such is defined on
     * the object
     *
     * @param object the entity ID
     */
    protected void addNoRoomDecay(EntityId object) {
        ObjectComponent objectComponent = entityData.getComponent(object, ObjectComponent.class);
        if (objectComponent != null && kwdFile.getObject(objectComponent.objectId).getFlags().contains(GameObject.ObjectFlag.DIE_OVER_TIME_IF_NOT_IN_ROOM)) {
            entityData.setComponent(object, new Decay(gameTimer.getGameTime(), ObjectsController.OBJECT_TIME_TO_LIVE));
        }
    }

}
