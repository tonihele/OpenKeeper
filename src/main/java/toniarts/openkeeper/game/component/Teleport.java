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

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityComponent;

/**
 * An Imp teleporting to its job with its own spell: it jumps out, lands on the
 * job and jumps in
 */
public final class Teleport implements EntityComponent {

    /**
     * Game time when the Imp started to jump out, or landed
     */
    public double startTime;
    /**
     * Where the Imp lands, null once it has landed
     */
    public Vector3f destination;

    public Teleport() {
        // For serialization
    }

    public Teleport(double startTime, Vector3f destination) {
        this.startTime = startTime;
        this.destination = destination;
    }

}
