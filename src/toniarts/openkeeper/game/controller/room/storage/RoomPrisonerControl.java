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
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Holds out the prisoners populating a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class RoomPrisonerControl extends AbstractRoomObjectControl<EntityId> {

    public RoomPrisonerControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.PRISONER);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }


    @Override
    public EntityId addItem(EntityId creature, Point p) {
        setRoomStorageToItem(creature, false);
        addCurrentCapacity(1);

        return creature;
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);

        addCurrentCapacity(-1);
    }

    @Override
    public void destroy() {
        super.destroy();

        // TODO: The prisoners are released!
    }

    @Override
    public void captured(short playerId) {
        // TODO: Also the prisoners might get released...
    }

}
