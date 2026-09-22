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
package toniarts.openkeeper.game.map;

/**
 * The minimap design (minimap_design.md §2.4) assumes a {@code
 * playerNumber(playerId)} translation from level-file player ids into a
 * 1..7 palette slot. In this codebase raw {@code short} owner/player ids
 * (see {@link toniarts.openkeeper.tools.convert.map.Player}) already are
 * those slot numbers
 */
public final class PlayerNumbers {

    private PlayerNumbers() {
    }

    public static short playerNumber(short playerId) {
        return playerId;
    }

}
