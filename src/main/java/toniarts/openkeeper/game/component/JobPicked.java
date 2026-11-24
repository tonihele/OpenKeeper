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
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.utils.Point;

/**
 * What the creature's search for work remembers. A creature that got a job by
 * picking from its job list doesn't pick again for a while. A worker carries on
 * with the digging or claiming it got last, and after a drop its first search
 * takes the closest work around the spot where it landed
 */
public final class JobPicked implements EntityComponent {

    /**
     * Game time of the pick
     */
    public double time;

    /**
     * The work a worker got from its last search for work, {@code null} for
     * none
     */
    public TaskType work;

    /**
     * Where the keeper dropped a worker, until its first search for work after
     * the drop
     */
    public Point dropArea;

    public JobPicked() {
        // For serialization
    }

    public JobPicked(double time) {
        this.time = time;
    }

    public JobPicked(TaskType work, Point dropArea) {
        this.work = work;
        this.dropArea = dropArea;
    }

}
