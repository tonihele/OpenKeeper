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
package toniarts.openkeeper.game.state;

import toniarts.openkeeper.tools.convert.map.Room;

/**
 * Decides which HUD room icons the keeper can afford. The ones it can't are
 * shown darkened, like in DK II
 */
public final class HudAffordability {

    private HudAffordability() {
        // Nope
    }

    /**
     * A room can be afforded while the gold covers at least one tile of it
     *
     * @param room the room
     * @param gold the gold the keeper has
     * @return true if the keeper can build the room
     */
    public static boolean canAffordRoom(Room room, int gold) {
        return room.getCost() <= gold;
    }

}
