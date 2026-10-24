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
package toniarts.openkeeper.game.controller.creature;

import java.util.EnumSet;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

/**
 * Tells when a hero with the DESTROY_ROOMS flag should look at the room under
 * him. The heroes don't go looking for rooms, they wreck the ones on their way:
 * the check is made when the hero steps on a new tile while he is free to take
 * on work, not fighting, fleeing or carried
 */
final class RoomDestructionTrigger {

    private static final Set<CreatureState> FREE_STATES = EnumSet.of(CreatureState.IDLE, CreatureState.WORK, CreatureState.FOLLOW);

    private Point lastTile;

    /**
     * Note the tile the creature is on
     *
     * @param tile the tile the creature is on
     * @param state the creature state
     * @param destroysRooms has the creature the DESTROY_ROOMS flag
     * @param ownerId the owner of the creature
     * @return the tile, if the hero has just stepped on it and should see if
     * there is a room to wreck, {@code null} otherwise
     */
    Point onTheWay(Point tile, CreatureState state, boolean destroysRooms, short ownerId) {
        boolean steppedOn = lastTile != null && !lastTile.equals(tile);
        lastTile = tile;
        if (steppedOn && destroysRooms && ownerId == Player.GOOD_PLAYER_ID && FREE_STATES.contains(state)) {
            return tile;
        }
        return null;
    }

}
