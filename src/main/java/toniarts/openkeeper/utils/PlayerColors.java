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
package toniarts.openkeeper.utils;

/**
 * The keepers' colours. DK II has one fixed table of them at 0x006E5058,
 * filled at 0x004C04C0. Player-coloured lights, effects and flowers, the lobby
 * list, the chat and the loading bars all read it by player number.
 */
public final class PlayerColors {

    /**
     * The 8 slots, shade 0. The slot is the player number: 0 unowned, 1 good
     * (also the fallback), 2 neutral, 3..7 the keepers. It is code in the
     * original, not data.
     */
    private static final int[][] COLORS = {
        {0, 0, 0},
        {191, 213, 224},
        {255, 255, 255},
        {230, 50, 50},
        {50, 78, 230},
        {50, 230, 51},
        {226, 230, 50},
        {230, 50, 212}
    };
    private static final int FALLBACK_SLOT = 1;

    private PlayerColors() {
    }

    /**
     * Get a player's colour. An absent or out-of-range player falls back to
     * slot 1 (pale blue-grey), not to black.
     *
     * @param playerId the player number, {@code null} if none
     * @return {r, g, b}, 0..255
     */
    public static int[] getRgb(Short playerId) {
        int slot = playerId == null || playerId < 0 || playerId >= COLORS.length
                ? FALLBACK_SLOT : playerId;
        return COLORS[slot].clone();
    }
}
