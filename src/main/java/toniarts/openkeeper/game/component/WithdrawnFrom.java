/*
 * Copyright (C) 2014-2017 OpenKeeper
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
 * Where a creature withdrew from a fight and when. Once it is well again it
 * goes back there, if it isn't too long ago (DK II 0x4611D0, 0x4773D0)
 */
public final class WithdrawnFrom implements EntityComponent {

    /**
     * The tile the creature withdrew from
     */
    public Point tile;

    /**
     * The game time it withdrew
     */
    public double time;

    public WithdrawnFrom() {
        // For serialization
    }

    public WithdrawnFrom(Point tile, double time) {
        this.tile = tile;
        this.time = time;
    }

}
