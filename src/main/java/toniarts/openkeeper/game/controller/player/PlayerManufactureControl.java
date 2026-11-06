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
package toniarts.openkeeper.game.controller.player;

/**
 * The keeper's manufacturing progress. All the creatures working in the
 * keeper's workshops add their manufacture points to the item currently in
 * production, like research points add to the current research.
 */
public final class PlayerManufactureControl {

    private int progress = 0;

    /**
     * Adds manufacture points to the item in production
     *
     * @param points the points to add
     * @param required the points the item needs ({@code manufToBuild})
     * @return true if the item is finished, the progress starts over then
     */
    public boolean manufacture(int points, int required) {
        progress += points;
        if (progress >= required) {
            progress = 0;
            return true;
        }
        return false;
    }

    /**
     * @return the manufacture points gathered for the item in production
     */
    public int getProgress() {
        return progress;
    }

}
