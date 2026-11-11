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
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.map.RoomPillars;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Constructs "normal" rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class NormalRoomController extends AbstractRoomController {

    public NormalRoomController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController) {
        this(entityId, entityData, kwdFile, roomInstance, objectsController, null);
    }

    public NormalRoomController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController, ObjectType defaultStorageType) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, defaultStorageType);
    }

    @Override
    protected List<EntityId> constructPillars() {
        List<EntityId> pillars = new ArrayList<>();
        for (RoomPillars.Placement placement : RoomPillars.getPlacements(roomInstance)) {
            pillars.add(objectsController.loadObject(placement.objectId(), (short) 0,
                    placement.tile().x, placement.tile().y, placement.rotation()));
        }
        return pillars;
    }
}
