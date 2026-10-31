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
 * Holds the gamblers of a Casino, one at each piece of its furniture
 */
public final class RoomGamblerControl extends AbstractRoomObjectControl<EntityId> {

    public RoomGamblerControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.GAMBLER);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    protected int getNumberOfAccessibleTiles() {
        return parent.getFloorFurnitureCount() + parent.getWallFurnitureCount();
    }

    @Override
    public EntityId addItem(EntityId gambler, Point p) {
        setRoomStorageToItem(gambler, false);
        addCurrentCapacity(1);

        return gambler;
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
