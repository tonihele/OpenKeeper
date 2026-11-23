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
import com.simsilica.es.EntityId;

/**
 * A shot on its way to its target
 */
public final class ShotFlight implements EntityComponent {

    public short shotId;
    public short ownerId;
    public EntityId shooter;
    public int shotData1;
    public int shotData2;
    public Vector3f velocity; // Tiles per turn
    public Vector3f start;
    public float maxRange; // Tiles, 0 for none
    public int health; // Turns left

    public ShotFlight() {
        // For serialization
    }

    public ShotFlight(short shotId, short ownerId, EntityId shooter, int shotData1, int shotData2, Vector3f velocity,
            Vector3f start, float maxRange, int health) {
        this.shotId = shotId;
        this.ownerId = ownerId;
        this.shooter = shooter;
        this.shotData1 = shotData1;
        this.shotData2 = shotData2;
        this.velocity = velocity;
        this.start = start;
        this.maxRange = maxRange;
        this.health = health;
    }

    public ShotFlight(ShotFlight flight, int health) {
        this(flight.shotId, flight.ownerId, flight.shooter, flight.shotData1, flight.shotData2, flight.velocity,
                flight.start, flight.maxRange, health);
    }

}
