/*
 * Copyright (C) 2014-2016 OpenKeeper
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

/**
 * Signifies that a room is an entrance for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ICreatureEntrance extends IEntityGenerator {

    /** Successful Portal arrivals since capture; Dungeon Hearts do not use this counter. */
    default int getGeneratedCount() {
        return 0;
    }

    /** Signal an eligible Portal's build-up, three seconds before its generation check. */
    default void onPrepareSpawn(double preparationTime) {
    }

    /** Advance a Portal's generation clock, even when attraction cannot produce a creature. */
    default void onGenerationCycle(double time) {
        // Dungeon Hearts have their own free-Imp clock.
    }
}
