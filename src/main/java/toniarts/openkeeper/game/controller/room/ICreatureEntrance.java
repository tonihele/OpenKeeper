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

import com.simsilica.es.EntityId;

/**
 * Signifies that a room is an entrance for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ICreatureEntrance extends IEntityGenerator {

    /**
     * Get the number of creatures that came in through this entrance since it
     * was built or claimed. The original lengthens the Portal's generation
     * interval with it.
     *
     * @return the number of creatures that came in
     */
    default int getCreaturesEntered() {
        return 0;
    }

    /**
     * Notifies that a creature came in through this entrance outside its own
     * generation, like one a level script generates. The creature counts,
     * but the generation clock is not restarted.
     *
     * @param enteredEntityId the creature that came in
     */
    default void onCreatureEntered(EntityId enteredEntityId) {
    }

    /**
     * Notifies that the entrance's generation time came but no creature came
     * in, because attraction is closed, the portal limit is reached or no
     * creature is attracted. The original restarts the clock anyway.
     *
     * @param time the time of the generation check
     */
    default void onGenerationSkipped(double time) {
    }

}
