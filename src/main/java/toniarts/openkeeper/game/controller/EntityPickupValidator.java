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
package toniarts.openkeeper.game.controller;

import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Pickup rules that are shared by the authoritative game controller and the
 * client view.
 *
 * @author OpenKeeper team
 */
public final class EntityPickupValidator {

    private EntityPickupValidator() {
    }

    /**
     * Checks object-specific restrictions for the entity's current location.
     * Loose gold follows the Dungeon Keeper II own-land rule. Other entity
     * types do not currently have a location restriction.
     *
     * @param object object metadata, or {@code null} for a non-object entity
     * @param position entity position
     * @param playerId player attempting the pickup
     * @param mapData current map data
     * @return {@code true} when the location permits pickup
     */
    public static boolean isValidLocation(ObjectComponent object, Position position, short playerId,
            IMapDataInformation<? extends IMapTileInformation> mapData) {
        if (object == null || object.objectId != ObjectsController.OBJECT_GOLD_ID) {
            return true;
        }
        if (position == null || mapData == null) {
            return false;
        }

        IMapTileInformation tile = mapData.getTile(WorldUtils.vectorToPoint(position.position));
        return tile != null && tile.getOwnerId() == playerId;
    }
}
