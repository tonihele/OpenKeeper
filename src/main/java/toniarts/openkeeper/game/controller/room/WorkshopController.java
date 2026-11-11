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
import java.util.List;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.map.RoomPillars;
import toniarts.openkeeper.game.controller.room.storage.RoomCrateControl;
import toniarts.openkeeper.game.controller.room.storage.RoomManufacturerControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The workshop
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class WorkshopController extends NormalRoomController {

    private final IGameTimer gameTimer;

    public WorkshopController(EntityId entityId, EntityData entityData, IKwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,
            IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.MANUFACTURER);
        this.gameTimer = gameTimer;
    }

    @Override
    public void construct() {
        super.construct();

        // The creatures work at the furniture, the crates are stored on the floor
        addObjectControl(new RoomManufacturerControl(kwdFile, this, entityData, gameTimer) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFurnitureCount();
            }
        });
        addObjectControl(new RoomCrateControl(kwdFile, this, entityData, gameTimer) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }
        });
    }

    @Override
    protected List<EntityId> constructPillars() {
        List<EntityId> pillars = new java.util.ArrayList<>();
        for (RoomPillars.Placement placement : RoomPillars.getPlacements(roomInstance)) {
            pillars.add(objectsController.loadObject(placement.objectId(), roomInstance.getOwnerId(),
                    placement.tile().x, placement.tile().y, placement.rotation()));
        }
        return pillars;
    }

    @Override
    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ALLOW_DIAGONAL_NEIGHBOUR_ONLY;
    }

}
