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

/**
 * The firing state of an armed trap. A trap without this has never fired and
 * is ready to.
 */
public final class TrapFiring implements EntityComponent {

    /**
     * Charging time when the trap is not charging
     */
    public static final double NOT_CHARGING = -1;

    /**
     * Game time the trap is recharged and can fire again
     */
    public double rechargedTime;

    /**
     * Game time the trap first saw its current victim, {@link #NOT_CHARGING}
     * when it sees none
     */
    public double chargeStartTime;

    public TrapFiring() {
        // For serialization
    }

    public TrapFiring(double rechargedTime, double chargeStartTime) {
        this.rechargedTime = rechargedTime;
        this.chargeStartTime = chargeStartTime;
    }

}
