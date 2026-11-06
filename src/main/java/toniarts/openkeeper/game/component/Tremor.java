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
import toniarts.openkeeper.utils.Point;

/**
 * A tremor (from the Tremor keeper spell) shaking the ground around a tile
 */
public final class Tremor implements EntityComponent {

    public Point centre;
    public int radius;
    public double startTime;
    public double endTime;

    public Tremor() {
        // For serialization
    }

    /**
     * A new tremor
     *
     * @param centre the tile the tremor was cast on
     * @param radius the radius of the shaken area, in tiles
     * @param startTime the game time the tremor started at
     * @param endTime the game time the tremor stops at
     */
    public Tremor(Point centre, int radius, double startTime, double endTime) {
        this.centre = centre;
        this.radius = radius;
        this.startTime = startTime;
        this.endTime = endTime;
    }

}
