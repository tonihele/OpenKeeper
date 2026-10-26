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
package toniarts.openkeeper.game.controller.room.storage;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Keeps the specials, the boxes the imps bring in, on the floor of the room.
 * One box a tile, the furniture takes up its tiles.
 */
public abstract class RoomSpecialControl extends AbstractRoomObjectControl<EntityId> {

    protected RoomSpecialControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.SPECIAL);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    public EntityId addItem(EntityId value, Point p) {
        Collection<Point> availableCoordinates = getAvailableCoordinates();
        if (p == null || !availableCoordinates.contains(p)) {
            if (availableCoordinates.isEmpty()) {
                return value; // No room for it
            }
            p = availableCoordinates.iterator().next();
        }

        Position position = entityData.getComponent(value, Position.class);
        Vector3f newPosition = WorldUtils.pointToVector3f(p);
        newPosition.y = position.position.y;
        entityData.setComponent(value, new Position(position.rotation, newPosition));

        objectsByCoordinate.computeIfAbsent(p, (point) -> new ArrayList<>(getObjectsPerTile())).add(value);
        setRoomStorageToItem(value, true);
        addCurrentCapacity(1);
        return null;
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);
        addCurrentCapacity(-1);
    }

    @Override
    public void destroy() {

        // The boxes stay where they are, no longer stored. Before the storage itself is gone, removing a box updates
        // its capacity
        for (Collection<EntityId> specials : new ArrayList<>(objectsByCoordinate.values())) {
            for (EntityId special : new ArrayList<>(specials)) {
                removeItem(special);
            }
        }

        super.destroy();
    }

    @Override
    protected Collection<Point> getCoordinates() {

        // The floor, the furniture takes up its tiles
        Set<Point> furniture = new HashSet<>(parent.getFloorFurnitureCount());
        for (EntityId oc : parent.getFloorFurniture()) {
            furniture.add(WorldUtils.vectorToPoint(entityData.getComponent(oc, Position.class).position));
        }
        List<Point> coordinates = new ArrayList<>(super.getCoordinates());
        coordinates.removeAll(furniture);
        return coordinates;
    }

}
