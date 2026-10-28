/*
 * Copyright (C) 2014-2020 OpenKeeper
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
 * Regenerates health (when on own grounds)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Regeneration implements EntityComponent {

    public int ownLandHealthIncrease;
    public Double timeOnOwnLand;

    public Regeneration() {
        // For serialization
    }

    public Regeneration(int ownLandHealthIncrease, Double timeOnOwnLand) {
        this.ownLandHealthIncrease = ownLandHealthIncrease;
        this.timeOnOwnLand = timeOnOwnLand;
    }

    public Regeneration(Regeneration regeneration) {
        this.ownLandHealthIncrease = regeneration.ownLandHealthIncrease;
        this.timeOnOwnLand = regeneration.timeOnOwnLand;
    }

}
