/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureGenerator;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * Portal is the only one I think
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ThreeByThreeController extends AbstractRoomController implements ICreatureEntrance {

    public ThreeByThreeController(EntityId entityId, EntityData entityData, KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, null);

        entityData.setComponent(entityId, new CreatureGenerator(roomInstance.getCenter(), Double.MIN_VALUE));
    }

    @Override
    public boolean isTileAccessible(Integer fromX, Integer fromY, int toX, int toY) {

        // The center tile is not accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(toX, toY);
        return !(roomPoint.x == 1 && roomPoint.y == 1);
    }

    @Override
    public Point getEntranceCoordinate() {
        return roomInstance.getCenter();
    }

    @Override
    public double getLastSpawnTime() {
        return getEntityComponent(CreatureGenerator.class).lastSpawnTime;
    }

    @Override
    public void onSpawn(double time, EntityId entityId) {
        entityData.setComponent(entityId, new CreatureGenerator(roomInstance.getCenter(), time));
    }

    @Override
    public void captured(short playerId) {
        super.captured(playerId);
        entityData.setComponent(entityId, new CreatureGenerator(roomInstance.getCenter(), Double.MIN_VALUE));
    }
}
