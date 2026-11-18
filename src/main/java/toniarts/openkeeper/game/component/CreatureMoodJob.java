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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;

/**
 * When the creature last took a job out of discontent. Like in DK II, an
 * unhappy creature gets a new job from its unhappy list only every now and
 * then, and so does an angry creature from its angry list
 */
public final class CreatureMoodJob implements EntityComponent {

    public double unhappyJobTime;
    public double angryJobTime;

    public CreatureMoodJob() {
        // For serialization
    }

    /**
     * @param unhappyJobTime game time the creature last got a job from its
     * unhappy list, in seconds
     * @param angryJobTime game time the creature last got a job from its angry
     * list, in seconds
     */
    public CreatureMoodJob(double unhappyJobTime, double angryJobTime) {
        this.unhappyJobTime = unhappyJobTime;
        this.angryJobTime = angryJobTime;
    }

}
