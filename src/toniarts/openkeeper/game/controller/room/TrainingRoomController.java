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
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * The training room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TrainingRoomController extends NormalRoomController {

    public TrainingRoomController(EntityId entityId, EntityData entityData, KwdFile kwdFile, RoomInstance roomInstance, IObjectsController objectsController,
            IGameTimer gameTimer) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController);

        addObjectControl(new RoomTraineeControl(kwdFile, this, objectsController, gameTimer) {

            @Override
            protected int getNumberOfAccessibleTiles() {
                return getFurnitureCount();
            }
        });
    }

}
