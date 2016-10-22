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
package toniarts.openkeeper.world.room.control;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Room object controller
 *
 * @param <T> the held object type
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomObjectControl<T extends ObjectControl> {

    protected final GenericRoom parent;
    protected final Map<Point, T> objects = new HashMap<>();

    public RoomObjectControl(GenericRoom parent) {
        this.parent = parent;
    }

    protected abstract int getObjectsPerTile();

    protected abstract int getNumberOfAccessibleTiles();

    /**
     * Get the room currently used capacity
     *
     * @return the currently used capacity in number of objects
     */
    public abstract int getCurrentCapacity();

    /**
     * Get the type of object that can be stored in here
     *
     * @return the object type
     */
    public abstract GenericRoom.ObjectType getObjectType();

    /**
     * When the room gets destroyed, controls need to be destroyed
     */
    public abstract void destroy();

    /**
     * Add item to room
     *
     * @param sum the number of items to add
     * @param p preferred dropping point for the item
     * @param thingLoader thing loader for displaying the actual item
     * @param creature the creature adding the item
     * @return the item count that doesn't fit
     */
    public abstract int addItem(int sum, Point p, ThingLoader thingLoader, CreatureControl creature);

    /**
     * Get a room object
     *
     * @param p the object from point
     * @return the object in given point
     */
    public T getItem(Point p) {
        return objects.get(p);
    }

    /**
     * Get the max capacity of the room
     *
     * @return max capacity in number of objects
     */
    public int getMaxCapacity() {
        return getObjectsPerTile() * getNumberOfAccessibleTiles();
    }

    /**
     * Is the room at full capacity
     *
     * @return max capacity used
     */
    public boolean isFullCapacity() {
        return getCurrentCapacity() >= getMaxCapacity();
    }

    /**
     * Remove an item
     *
     * @param object the object
     */
    public void removeItem(T object) {
        object.setRoomObjectControl(null);
        objects.values().remove(object);
    }

    /**
     * Removes all objects (for real)
     */
    protected void removeAllObjects() {
        List<ObjectControl> objectList = new ArrayList<>(objects.values());
        for (ObjectControl obj : objectList) {
            obj.removeObject();
        }
    }

}
