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
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.WallSection.WallDirection;

import java.util.ArrayList;
import java.util.List;


/**
 * Torture chamber
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TortureChamberController extends NormalRoomController {

    private static final short OBJECT_TORTURE_WHEEL_ID = 8;
    private static final short OBJECT_TORTURE_COALS_ID = 62;
    private static final short OBJECT_TORTURE_WATER_ID = 63;
    private static final float WALL_WHEEL_DISTANCE = 0x7C0 / 4096f;
    private static final float WALL_WHEEL_HEIGHT = 0x800 / 4096f;
    private static final float TROUGH_DISTANCE = 0x480 / 4096f;

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

                // A trough shares its tile with the wheel above it
                return getCoordinates().size();
            }
        });

        // Extending the room builds the furniture again, the victims need the new devices
        control.refreshDevices();

        // Like in DK II, the torture wheels spin all the time, on the floor and on the walls
        List<EntityId> furnitures = new ArrayList<>(getFloorFurniture());
        furnitures.addAll(getWallFurniture());
        for (EntityId furniture : furnitures) {
            ObjectViewState viewState = entityData.getComponent(furniture, ObjectViewState.class);
            if (viewState != null && viewState.objectId == OBJECT_TORTURE_WHEEL_ID) {
                entityData.setComponent(furniture, new ObjectViewState(viewState.objectId,
                        GameObject.State.TORTURE_WHEEL_SPIN, viewState.animState, viewState.visible));
            }
        }
    }

    /**
     * Like DK II (MyRooms_RoomManager_cpp_4EDFC0, 0x4EDFC0), a wheel on the
     * wall hangs half a tile up, and a trough of coals or water stands under
     * a wheel
     */
    @Override
    protected List<EntityId> constructWallObject(short objectId, Point p, WallDirection direction) {
        return switch (objectId) {
            case OBJECT_TORTURE_WHEEL_ID ->
                List.of(loadWallObject(objectId, p, direction, WALL_WHEEL_DISTANCE, WALL_WHEEL_HEIGHT));
            case OBJECT_TORTURE_COALS_ID, OBJECT_TORTURE_WATER_ID ->
                List.of(loadWallObject(objectId, p, direction, TROUGH_DISTANCE, 0),
                        loadWallObject(OBJECT_TORTURE_WHEEL_ID, p, direction, WALL_WHEEL_DISTANCE, WALL_WHEEL_HEIGHT));
            default ->
                super.constructWallObject(objectId, p, direction);
        };
    }
}
