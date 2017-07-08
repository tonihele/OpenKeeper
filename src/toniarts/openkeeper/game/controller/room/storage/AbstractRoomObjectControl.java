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
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.IRoomController;

/**
 * Room object controller. FIXME: Cache the coordinates and listen to changes in
 * rooms
 *
 * @param <V> the value type to add
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractRoomObjectControl<V> implements IRoomObjectControl<V> {

    protected final IRoomController parent;
    protected final IObjectsController objectsController;
    protected final EntityData entityData;
    protected final Map<Point, Collection<EntityId>> objectsByCoordinate = new HashMap<>();

    public AbstractRoomObjectControl(IRoomController parent, IObjectsController objectsController, EntityData entityData) {
        this.parent = parent;
        this.objectsController = objectsController;
        this.entityData = entityData;
    }

    protected abstract int getObjectsPerTile();

    protected abstract int getNumberOfAccessibleTiles();

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
    public int getMaxCapacity() {
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

    /**
     * Remove an item
     *
     * @param object the object
     */
    @Override
    public void removeItem(EntityId object) {
        //object.setRoomObjectControl(null);
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
                //obj.removeObject();
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

}
