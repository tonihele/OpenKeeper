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
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.CreatureControl;
import toniarts.openkeeper.world.object.GoldObjectControl;
import toniarts.openkeeper.world.object.ObjectControl;
import toniarts.openkeeper.world.room.GenericRoom;

/**
 * Not really a JME control currently. Manages how the gold places in the room.
 * Should be generalized to provide a control for any type of object owned by
 * the room. Either these controls or decorator pattern, lets see...
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomGoldControl extends RoomObjectControl {

    private int storedGold = 0;
    private final Map<Point, GoldObjectControl> goldPiles = new HashMap<>();

    public RoomGoldControl(GenericRoom parent) {
        super(parent);
    }

    @Override
    public int addItem(int sum, Point p, ThingLoader thingLoader, CreatureControl creature) {
        if (p != null) {
            sum = putGold(sum, p, thingLoader);
        }
        if (sum > 0) {
            List<Point> coordinates = parent.getRoomInstance().getCoordinates();
            for (Point coordinate : coordinates) {
                if (parent.isTileAccessible(coordinate.x, coordinate.y)) {
                    sum = putGold(sum, coordinate, thingLoader);
                    if (sum == 0) {
                        break;
                    }
                }
            }
        }
        return sum;
    }

    private int putGold(int sum, Point p, ThingLoader thingLoader) {
        int pointStoredGold = 0;
        GoldObjectControl goldPile = goldPiles.get(p);
        if (goldPile != null) {
            pointStoredGold = goldPile.getGold();
        }
        if (pointStoredGold < getObjectsPerTile()) {
            int goldToStore = Math.min(sum, getObjectsPerTile() - pointStoredGold);
            pointStoredGold += goldToStore;
            sum -= goldToStore;
            storedGold += goldToStore;

            // Add the visuals
            if (goldPile == null) {
                goldPile = thingLoader.addRoomGold(p, parent.getRoomInstance().getOwnerId(), goldToStore);
                goldPiles.put(p, goldPile);
            } else {

                // Adjust the gold sum
                goldPile.setGold(pointStoredGold);
            }
        }
        return sum;
    }

    @Override
    public int getCurrentCapacity() {
        return storedGold;
    }

    @Override
    public ObjectControl getItem(Point p) {
        return goldPiles.get(p);
    }

    @Override
    public GenericRoom.ObjectType getObjectType() {
        return GenericRoom.ObjectType.GOLD;
    }

}
