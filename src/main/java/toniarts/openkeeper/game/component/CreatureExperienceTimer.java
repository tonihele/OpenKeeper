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
 * Time a creature has worked, fought or trained since it last gained
 * experience. It gains experience for every full second, so this stays under
 * a second.
 */
public final class CreatureExperienceTimer implements EntityComponent {

    public double timeWorking;

    public CreatureExperienceTimer() {
        // For serialization
    }

    public CreatureExperienceTimer(double timeWorking) {
        this.timeWorking = timeWorking;
    }

}
