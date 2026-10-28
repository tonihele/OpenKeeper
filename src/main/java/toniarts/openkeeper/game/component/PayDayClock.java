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
 * What the keeper's HUD shows of the next pay day, the time left to it and
 * what it costs
 */
public final class PayDayClock implements EntityComponent {

    /**
     * Whole seconds to the next pay day, 0 while the clock hasn't started
     */
    public int timeLeft;
    /**
     * Seconds from the last pay day (or the start of the clock) to the next
     */
    public int period;
    public int cost;

    public PayDayClock() {
        // For serialization
    }

    public PayDayClock(int timeLeft, int period, int cost) {
        this.timeLeft = timeLeft;
        this.period = period;
        this.cost = cost;
    }

    /**
     * How much of the time between the pay days is still left
     *
     * @return from 1 right after a pay day down to 0 on the next one
     */
    public float getTimeLeftFraction() {
        return period > 0 ? Math.clamp((float) timeLeft / period, 0f, 1f) : 0f;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PayDayClock other && timeLeft == other.timeLeft && period == other.period
                && cost == other.cost;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * timeLeft + period) + cost;
    }

}
