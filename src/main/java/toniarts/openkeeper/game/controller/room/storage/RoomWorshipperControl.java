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
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

/**
 * The creatures praying in a Temple. Like in DK II, one creature prays on each
 * floor tile, the pool in the middle is not for standing on
 */
public abstract class RoomWorshipperControl extends AbstractRoomObjectControl<EntityId> {

    protected RoomWorshipperControl(IKwdFile kwdFile, IRoomController parent, EntityData entityData, IGameTimer gameTimer) {
        super(kwdFile, parent, entityData, gameTimer, ObjectType.WORSHIPPER);
    }

    @Override
    protected int getObjectsPerTile() {
        return 1;
    }

    @Override
    public EntityId addItem(EntityId worshipper, Point p) {
        setRoomStorageToItem(worshipper, false);
        addCurrentCapacity(1);

        return worshipper;
    }

    @Override
    public void removeItem(EntityId object) {
        super.removeItem(object);

        addCurrentCapacity(-1);
    }

    @Override
    public void captured(short playerId) {

    }

}
