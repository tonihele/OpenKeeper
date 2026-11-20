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
 * A Horned Reaper summoned by a keeper spell. He either stays for a set time
 * or for as long as his keeper can pay for him with mana.
 */
public final class SummonedReaper implements EntityComponent {

    /**
     * Marks the times that haven't happened yet
     */
    public static final double NOT_YET = -1;

    public double summonTime;

    /**
     * How long the Reaper stays, in seconds. 0 if he stays as long as the
     * keeper pays.
     */
    public double lifetime;

    /**
     * Mana the keeper pays per second, once the upkeep has started
     */
    public int manaDrain;

    /**
     * When the mana drain was last checked, {@link #NOT_YET} if the keeper
     * isn't paying yet
     */
    public double manaCheckTime;

    /**
     * When the Reaper started to leave, {@link #NOT_YET} if he hasn't
     */
    public double leaveTime;

    public SummonedReaper() {
        // For serialization
    }

    public SummonedReaper(double summonTime, double lifetime, int manaDrain) {
        this(summonTime, lifetime, manaDrain, NOT_YET, NOT_YET);
    }

    public SummonedReaper(double summonTime, double lifetime, int manaDrain, double manaCheckTime, double leaveTime) {
        this.summonTime = summonTime;
        this.lifetime = lifetime;
        this.manaDrain = manaDrain;
        this.manaCheckTime = manaCheckTime;
        this.leaveTime = leaveTime;
    }

    /**
     * Is the Reaper on his way out
     *
     * @return {@code true} if he has started to leave
     */
    public boolean isLeaving() {
        return leaveTime != NOT_YET;
    }

    /**
     * Is the keeper paying mana for the Reaper
     *
     * @return {@code true} if the mana drain has started
     */
    public boolean isPaying() {
        return manaCheckTime != NOT_YET;
    }

}
