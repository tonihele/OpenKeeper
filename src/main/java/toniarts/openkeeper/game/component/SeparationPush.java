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

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityComponent;

/**
 * The push a walking creature gets from the creatures it overlaps, worked out
 * once a game tick and added to its steering
 */
public final class SeparationPush implements EntityComponent {

    /**
     * The push on the map's x and z axes, as a share of the path following's
     * acceleration
     */
    public Vector2f push;

    public SeparationPush() {
        // For serialization
    }

    public SeparationPush(Vector2f push) {
        this.push = push;
    }

}
