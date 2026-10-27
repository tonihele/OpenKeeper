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
 * Movement intent of a possessed creature, as steered by its keeper
 */
public final class PossessedMovement implements EntityComponent {

    public static final byte SPEED_WALK = 0;
    public static final byte SPEED_RUN = 1;
    public static final byte SPEED_CREEP = 2;

    /**
     * Normalized movement direction on the map plane (x, z), zero when standing
     * still
     */
    public Vector2f direction;
    public float rotation;
    public float speed;

    public PossessedMovement() {
        // For serialization
    }

    public PossessedMovement(Vector2f direction, float rotation, float speed) {
        this.direction = direction;
        this.rotation = rotation;
        this.speed = speed;
    }

}
