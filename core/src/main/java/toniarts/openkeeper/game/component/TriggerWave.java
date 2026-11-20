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
import java.util.Set;

/**
 * The spreading set off of a Trigger Trap
 */
public final class TriggerWave implements EntityComponent {

    /**
     * The Trigger Trap
     */
    public EntityId triggerTrap;
    /**
     * The owner, whose traps the wave sets off
     */
    public short ownerId;
    /**
     * Where the Trigger Trap is
     */
    public Vector3f origin;
    /**
     * The creature that set off the Trigger Trap
     */
    public EntityId target;
    /**
     * Game time the Trigger Trap fired
     */
    public double startTime;
    /**
     * The traps the wave has already reached
     */
    public Set<EntityId> reachedTraps;

    public TriggerWave() {
        // For serialization
    }

    public TriggerWave(EntityId triggerTrap, short ownerId, Vector3f origin, EntityId target, double startTime,
            Set<EntityId> reachedTraps) {
        this.triggerTrap = triggerTrap;
        this.ownerId = ownerId;
        this.origin = origin;
        this.target = target;
        this.startTime = startTime;
        this.reachedTraps = reachedTraps;
    }

}
