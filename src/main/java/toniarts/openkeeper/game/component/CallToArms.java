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
 * A Call to Arms banner
 */
public final class CallToArms implements EntityComponent {

    public static final double UP = -1;

    /**
     * Game time when the banner has risen from the ground and starts waving
     */
    public double waveTime;

    /**
     * Game time when the mana upkeep starts, the casting cost pays for the
     * time before it
     */
    public double upkeepTime;

    /**
     * Game time when the banner calls the creatures next
     */
    public double callTime;

    /**
     * How many times the banner has called the creatures
     */
    public int calls;

    /**
     * Game time when the banner that is going down is gone, {@link #UP} while
     * it is up
     */
    public double removeTime = UP;

    public CallToArms() {
        // For serialization
    }

    public CallToArms(double waveTime, double upkeepTime, double callTime, int calls, double removeTime) {
        this.waveTime = waveTime;
        this.upkeepTime = upkeepTime;
        this.callTime = callTime;
        this.calls = calls;
        this.removeTime = removeTime;
    }

    /**
     * @return true when the banner is up, not sinking or slapped away
     */
    public boolean isUp() {
        return removeTime == UP;
    }

}
