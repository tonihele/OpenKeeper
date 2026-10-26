/*
 * Copyright (C) 2014-2018 OpenKeeper
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
 * Creature sleeping component. Marks the need for sleep overall.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class CreatureSleep implements EntityComponent {

    private static final int RUNTIME_SLEEP_SCALE = 4;

    public EntityId lairObjectId;
    /** Last fatigue latch or sleeping update; the awake interval starts after rest. */
    public double lastSleepTime;
    public double sleepStartTime;
    /** Remaining native sleep updates. */
    public int sleepNeed;

    public CreatureSleep() {
        // For serialization
    }

    /**
     * Captures bed ownership and the independent fatigue and rest clocks.
     *
     * @param lairObjectId assigned bed, or {@code null} if none exists
     * @param lastSleepTime last fatigue latch or sleeping update, in game seconds
     * @param sleepStartTime beginning of the current rest, in game seconds
     * @param sleepNeed remaining native sleep updates; zero still permits healing
     */
    public CreatureSleep(EntityId lairObjectId, double lastSleepTime, double sleepStartTime, int sleepNeed) {
        this.lairObjectId = lairObjectId;
        this.lastSleepTime = lastSleepTime;
        this.sleepStartTime = sleepStartTime;
        this.sleepNeed = sleepNeed;
    }

    /**
     * Converts authored sleep duration to debt consumed once per native update.
     *
     * @param authoredTimeSleep the creature definition's sleep duration
     * @return remaining updates at the native four-ticks-per-second rate
     */
    public static int toRuntimeSleepNeed(int authoredTimeSleep) {
        return authoredTimeSleep * RUNTIME_SLEEP_SCALE;
    }

}
