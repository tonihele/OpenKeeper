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
 * The velocity of a creature that stands but is moved by its pilot, pushed by
 * the creatures it overlaps or sliding along the Dungeon Heart. DK II's pilot
 * runs for a fighting creature too (0x488930, 0x4D4760).
 */
public final class StandingVelocity implements EntityComponent {

    /**
     * The velocity on the map's x and z axes, tiles a second
     */
    public Vector2f velocity;

    public StandingVelocity() {
        // For serialization
    }

    public StandingVelocity(Vector2f velocity) {
        this.velocity = velocity;
    }

}
