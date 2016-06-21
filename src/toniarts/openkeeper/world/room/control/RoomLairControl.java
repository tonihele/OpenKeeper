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
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Controls creature lairs in a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomLairControl extends RoomObjectControl {

    private final Map<Point, ObjectControl> lairs = new HashMap<>();

    public RoomLairControl(GenericRoom parent) {
        super(parent);
    }

    @Override
    public int getCurrentCapacity() {
        return lairs.size();
    }

    @Override
    public GenericRoom.ObjectType getObjectType() {
        return GenericRoom.ObjectType.LAIR;
    }

    @Override
    public int addItem(int sum, Point p, ThingLoader thingLoader, CreatureControl creature) {
        if (lairs.containsKey(p)) {
            return sum; // Already a lair here
        }
        lairs.put(p, thingLoader.addObject(p, creature.getCreature().getLairObjectId(), creature.getOwnerId()));
        return 0;
    }

    @Override
    public ObjectControl getItem(Point p) {
        return lairs.get(p);
    }

}
