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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Holds out the spell books
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomSpellBookControl extends AbstractRoomObjectControl<EntityId> {

    protected RoomSpellBookControl(KwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.SPELL_BOOK);
    }

    @Override
    protected int getObjectsPerTile() {
        return 2; // 2 spell books per floor furniture tile
    }

    @Override
    public EntityId addItem(EntityId value, Point p) {
        Collection<EntityId> spellBooks = null;
        if (p != null) {
            spellBooks = objectsByCoordinate.get(p);
            if (spellBooks != null && spellBooks.size() == getObjectsPerTile()) {
                return value; // Already max amount of books there
            }
        } else {

            // Find a spot
            Collection<Point> coordinates = getAvailableCoordinates();
            for (Point coordinate : coordinates) {
                p = new Point(coordinate.x, coordinate.y);
                spellBooks = objectsByCoordinate.get(p);
                if (spellBooks == null || spellBooks.size() < getObjectsPerTile()) {
                    break;
                }
            }
        }

        // Set the position correctly
        Position oldPos = entityData.getComponent(value, Position.class);
        Vector3f newPos = WorldUtils.pointToVector3f(p);
        newPos.y = oldPos.position.y;
        entityData.setComponent(value, new Position(oldPos.rotation, newPos));

        if (spellBooks == null) {
            spellBooks = new ArrayList<>(getObjectsPerTile());
        }
        spellBooks.add(value);
        objectsByCoordinate.put(p, spellBooks);
        setRoomStorageToItem(value, true);
        addCurrentCapacity(1);

        return null;
    }

    @Override
    public void destroy() {
        super.destroy();

        // The keeper has no more access to the spells
        List<Collection<EntityId>> objectList = new ArrayList<>(objectsByCoordinate.values());
        for (Collection<EntityId> objects : objectList) {
            for (EntityId obj : new ArrayList<>(objects)) {
                removeItem(obj);
            }
        }
    }

    @Override
    protected Collection<Point> getCoordinates() {

        // Only floor furniture
        List<Point> coordinates = new ArrayList<>(parent.getFloorFurnitureCount());
        for (EntityId oc : parent.getFloorFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(entityData.getComponent(oc, Position.class).position));
        }
        return coordinates;
    }

}
