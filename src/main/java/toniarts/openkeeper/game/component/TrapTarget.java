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
 * The creature a sight trap aimed at, and when. DK II keeps it as the trap's
 * target on the next tick while it stays in sight (0x4A9400).
 */
public final class TrapTarget implements EntityComponent {

    public EntityId target;

    /**
     * Game time of the tick the trap aimed at it
     */
    public double time;

    public TrapTarget() {
        // For serialization
    }

    public TrapTarget(EntityId target, double time) {
        this.target = target;
        this.time = time;
    }

}
