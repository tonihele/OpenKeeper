/*
 * Copyright (C) 2014-2019 OpenKeeper
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
 * Creature experience level stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureExperience implements EntityComponent {

    public int level;
    public int experience;
    public int experienceToNextLevel;
    public int experiencePerSecond;
    public int experiencePerTickTraining;

    public CreatureExperience() {
        // For serialization
    }

    public CreatureExperience(int level, int experience, int experienceToNextLevel, int experiencePerSecond, int experiencePerTickTraining) {
        this.level = level;
        this.experience = experience;
        this.experienceToNextLevel = experienceToNextLevel;
        this.experiencePerSecond = experiencePerSecond;
        this.experiencePerTickTraining = experiencePerTickTraining;
    }

    public CreatureExperience(CreatureExperience creatureExperience) {
        this.level = creatureExperience.level;
        this.experience = creatureExperience.experience;
        this.experienceToNextLevel = creatureExperience.experienceToNextLevel;
        this.experiencePerSecond = creatureExperience.experiencePerSecond;
        this.experiencePerTickTraining = creatureExperience.experiencePerTickTraining;
    }

}
