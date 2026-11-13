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
 * Gains health back every second, wherever the entity is and even right after
 * a hit, like a trap does (DKII.EXE's trap tick 0x4A84E0)
 */
public final class HealthGain implements EntityComponent {

    public int healthGain;
    public Double nextGainTime;

    public HealthGain() {
        // For serialization
    }

    public HealthGain(int healthGain, Double nextGainTime) {
        this.healthGain = healthGain;
        this.nextGainTime = nextGainTime;
    }

}
