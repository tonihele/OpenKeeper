/*
 * Copyright (C) 2014-2017 OpenKeeper
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
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomTortureeControl;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;


/**
 * Torture chamber
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TortureChamberController extends NormalRoomController {

    private static final short TORTURE_WHEEL_OBJECT_ID = 8;

    private final IGameTimer gameTimer;

    public TortureChamberController(EntityId entityId, EntityData entityData, IKwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,             IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.TORTUREE);

        this.gameTimer = gameTimer;
    }

    @Override
    public void construct() {
        super.construct();

        RoomTortureeControl control = addObjectControl(new RoomTortureeControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFurnitureCount();
            }
        });

        // Extending the room builds the furniture again, the victims need the new devices
        control.refreshDevices();

        // Like in DK II, the torture wheels spin all the time
        for (EntityId furniture : getFloorFurniture()) {
            ObjectViewState viewState = entityData.getComponent(furniture, ObjectViewState.class);
            if (viewState != null && viewState.objectId == TORTURE_WHEEL_OBJECT_ID) {
                entityData.setComponent(furniture, new ObjectViewState(viewState.objectId,
                        GameObject.State.TORTURE_WHEEL_SPIN, viewState.animState, viewState.visible));
            }
        }
    }
}
