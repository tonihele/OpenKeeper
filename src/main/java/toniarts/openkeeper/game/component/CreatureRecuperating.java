/*
 * Copyright (C) 2014-2018 OpenKeeper
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
 * Tags creature as recuperating from wounds
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureRecuperating implements EntityComponent {

    public double startTime;
    public double healthCheckTime;
    public double moodCheckTime;

    public CreatureRecuperating() {
        // For serialization
    }

    /**
     * Starts or resumes rest with independent health and mood recovery clocks.
     *
     * @param startTime beginning of recuperation, in game seconds
     * @param healthCheckTime last global second checked for health recovery
     * @param moodCheckTime last global second checked for mood recovery
     */
    public CreatureRecuperating(double startTime, double healthCheckTime, double moodCheckTime) {
        this.startTime = startTime;
        this.healthCheckTime = healthCheckTime;
        this.moodCheckTime = moodCheckTime;
    }

}
