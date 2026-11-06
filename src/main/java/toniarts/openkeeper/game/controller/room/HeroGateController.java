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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * The in-game hero gates, the final one (Level20) and the 3 x 1 one. Unlike
 * the front end version, they have no gem holder, banners or candles. The room
 * data lists no objects for them, and the portal gem on the final gate is
 * placed by the level itself.
 */
public final class HeroGateController extends NormalRoomController {

    public HeroGateController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController);
    }

    @Override
    protected void constructObjects() {
        // No furniture
    }
}
