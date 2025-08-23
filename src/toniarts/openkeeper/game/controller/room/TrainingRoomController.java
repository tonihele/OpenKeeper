/*
 * Copyright (C) 2014-2023 OpenKeeper
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
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.storage.RoomTraineeControl;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The training room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TrainingRoomController extends NormalRoomController {

    private final IGameTimer gameTimer;

    public TrainingRoomController(EntityId entityId, EntityData entityData, IKwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,
            IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, ObjectType.TRAINEE);

        this.gameTimer = gameTimer;
    }

    @Override
    public void construct() {
        super.construct();

        addObjectControl(new RoomTraineeControl(kwdFile, this, entityData, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFurnitureCount();
            }
        });
    }
}
