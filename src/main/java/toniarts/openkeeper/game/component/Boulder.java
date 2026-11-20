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
 * A rolling boulder: the way it rolls and the side it turns to at a wall. Its
 * health is what is left of it
 */
public final class Boulder implements EntityComponent {

    /**
     * The way it rolls, east and south, a unit vector until it turns at a
     * wall
     */
    public float directionX;
    public float directionY;

    /**
     * The side it takes where it can turn both ways: the way it first set
     * off, -1, 0 (either) or 1
     */
    public int turnX;
    public int turnY;

    public Boulder() {
        // For serialization
    }

    public Boulder(float directionX, float directionY, int turnX, int turnY) {
        this.directionX = directionX;
        this.directionY = directionY;
        this.turnX = turnX;
        this.turnY = turnY;
    }

}
