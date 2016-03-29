/*
 * Copyright (C) 2014-2016 OpenKeeper
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
package toniarts.openkeeper.game.task.type;

import com.jme3.math.Vector2f;
import java.awt.Point;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Base class for all tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractTask implements Comparable<AbstractTask> {

    private final Date taskCreated;
    private final Set<CreatureControl> assignees = new HashSet<>();

    public AbstractTask() {
        this.taskCreated = new Date();
    }

    public Date getTaskCreated() {
        return taskCreated;
    }

    /**
     * Amount of assignees this task can be assigned on
     *
     * @return max number of assignees
     */
    public int getMaxAllowedNumberOfAsignees() {
        return 1;
    }

    /**
     * Assing an entity to the task
     *
     * @param creature entity to be assigned
     */
    public void assign(CreatureControl creature) {
        if (assignees.size() == getMaxAllowedNumberOfAsignees()) {
            throw new IllegalArgumentException("Task already has the maximum number of assignees!");
        }
        assignees.add(creature);
        creature.setAssignedTask(this);
    }

    /**
     * How many workers have already been assigned to this task
     *
     * @return number of assignees on duty
     */
    public int getAssigneeCount() {
        return assignees.size();
    }

    /**
     * Task location, the task it self not necessarily the target for navigating
     *
     * @return the task location
     */
    public abstract Point getTaskLocation();

    /**
     * Evaluates the task validity
     *
     * @return the task validity
     */
    public abstract boolean isValid();

    /**
     * Get the target coordinates to navigate to for accomplishing the task
     *
     * @param creature who wants to know?
     * @return the target coordinates
     */
    public abstract Vector2f getTarget(CreatureControl creature);

    /**
     * Can the entity be assigned to this task
     *
     * @param creature the tested entity
     * @return returns tru if the entity can be assigned to the task
     */
    public boolean canAssign(CreatureControl creature) {
        return (assignees.size() < getMaxAllowedNumberOfAsignees() && isValid());
    }

    @Override
    public int compareTo(AbstractTask t) {
        return getTaskCreated().compareTo(t.getTaskCreated());
    }

}
