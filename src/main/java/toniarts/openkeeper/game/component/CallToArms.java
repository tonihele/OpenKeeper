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
 * A raised Call to Arms banner
 */
public final class CallToArms implements EntityComponent {

    /**
     * Game time when the mana upkeep starts, the casting cost pays for the
     * time before it
     */
    public double upkeepTime;

    /**
     * Game time when the banner calls the creatures next
     */
    public double callTime;

    public CallToArms() {
        // For serialization
    }

    public CallToArms(double upkeepTime, double callTime) {
        this.upkeepTime = upkeepTime;
        this.callTime = callTime;
    }

}
