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
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * The job of the room the keeper dropped the creature into. Like DK II's
 * "dropped to do" job, the creature keeps going back to it until it can't do
 * it any more or is picked up again. Only the server uses this.
 */
public final class DroppedJob implements EntityComponent {

    public Creature.JobType jobType;
    public boolean justDropped;

    public DroppedJob() {
        // For serialization
    }

    /**
     * The job of the room the creature was dropped into
     *
     * @param jobType the job
     * @param justDropped whether the creature has not yet started the job
     * since it landed
     */
    public DroppedJob(Creature.JobType jobType, boolean justDropped) {
        this.jobType = jobType;
        this.justDropped = justDropped;
    }

}
