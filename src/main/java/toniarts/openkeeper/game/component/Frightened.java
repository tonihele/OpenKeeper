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
 * A creature a possessed Maiden of the Nest has frightened. Until the end time
 * its fear is at least the severity, so it runs away unless it is fearless.
 */
public final class Frightened implements EntityComponent {

    /**
     * The creature that frightened us
     */
    public EntityId entity;

    public int severity;

    /**
     * The game time the fear ends
     */
    public double endTime;

    public Frightened() {
        // For serialization
    }

    public Frightened(EntityId entity, int severity, double endTime) {
        this.entity = entity;
        this.severity = severity;
        this.endTime = endTime;
    }

}
