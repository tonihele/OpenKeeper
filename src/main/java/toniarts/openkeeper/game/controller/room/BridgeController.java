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
 * Controls the bridges: the Wooden Bridge, the Stone Bridge and the Hero
 * Stone Bridge. They are the rooms built tile by tile over water or lava,
 * and the only ones using the quad tile construction.<br>
 * A bridge has no walls, pillars or furniture and stores nothing. The
 * placement rules are in {@code RoomPlacementValidator}. Of the bridges only
 * the Wooden Bridge burns away on lava, see {@code WoodenBridgeDecaySystem};
 * the Stone Bridge and the Hero Stone Bridge last until they are sold or
 * destroyed.
 */
public final class BridgeController extends AbstractRoomController {

    public BridgeController(EntityId entityId, EntityData entityData, IKwdFile kwdFile,
            RoomInstance roomInstance, IObjectsController objectsController) {
        super(entityId, entityData, kwdFile, roomInstance, objectsController, null);
    }

    /**
     * Bridges have no furniture, so there is nothing to place.
     */
    @Override
    protected void constructObjects() {
        // Nothing on a bridge
    }
}
