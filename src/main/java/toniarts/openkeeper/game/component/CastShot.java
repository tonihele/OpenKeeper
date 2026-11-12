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
import com.simsilica.es.EntityId;

/**
 * A creature's spell shot that leaves the creature once the cast has reached
 * the spell's shot delay
 */
public final class CastShot implements EntityComponent {

    public short creatureSpellId;
    public EntityId target;
    public float range;
    public double releaseTime;

    public CastShot() {
        // For serialization
    }

    public CastShot(short creatureSpellId, EntityId target, float range, double releaseTime) {
        this.creatureSpellId = creatureSpellId;
        this.target = target;
        this.range = range;
        this.releaseTime = releaseTime;
    }

}
