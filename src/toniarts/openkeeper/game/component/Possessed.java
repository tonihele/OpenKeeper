/*
 * Copyright (C) 2014-2024 OpenKeeper
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
 * Simple tagging component for entity being possessed
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Possessed implements EntityComponent {

    /**
     * We mark the mana drain here so that we can easily get the information.
     * Mana component should be used in other calculations
     */
    public int manaDrain;
    public double manaCheckTime;

    public Possessed() {
        // For serialization
    }

    public Possessed(int manaDrain, double manaCheckTime) {
        this.manaDrain = manaDrain;
        this.manaCheckTime = manaCheckTime;
    }

}
