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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * The creatures manufacturing in a workshop, one at each piece of furniture
 */
public abstract class RoomManufacturerControl extends AbstractRoomObjectControl<EntityId> {

    public RoomManufacturerControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.MANUFACTURER);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    public EntityId addItem(EntityId manufacturer, Point p) {
        setRoomStorageToItem(manufacturer, false);
        addCurrentCapacity(1);
        return manufacturer;
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);
        addCurrentCapacity(-1);
    }

    @Override
    public void captured(short playerId) {

    }

    @Override
    protected Collection<Point> getCoordinates() {

        // Only furniture
        List<Point> coordinates = new ArrayList<>(parent.getFloorFurnitureCount() + parent.getWallFurnitureCount());
        for (EntityId oc : parent.getFloorFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(entityData.getComponent(oc, Position.class).position));
        }
        for (EntityId oc : parent.getWallFurniture()) {
            coordinates.add(WorldUtils.vectorToPoint(entityData.getComponent(oc, Position.class).position));
        }
        return coordinates;
    }

}
