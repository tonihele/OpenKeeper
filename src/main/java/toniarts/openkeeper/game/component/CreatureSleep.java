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
    /** Time at which DKII last latched a new sleep debt. */
    public double lastSleepTime;
    public double sleepStartTime;
    /** Remaining native sleep updates. */
    public int sleepNeed;

    public CreatureSleep() {
        // For serialization
    }

    public CreatureSleep(EntityId lairObjectId, double lastSleepTime, double sleepStartTime, int sleepNeed) {
        this.lairObjectId = lairObjectId;
        this.lastSleepTime = lastSleepTime;
        this.sleepStartTime = sleepStartTime;
        this.sleepNeed = sleepNeed;
    }

    public static int toRuntimeSleepNeed(int authoredTimeSleep) {
        return authoredTimeSleep * RUNTIME_SLEEP_SCALE;
    }

}
