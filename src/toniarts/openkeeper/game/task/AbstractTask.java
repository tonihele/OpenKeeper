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
package toniarts.openkeeper.game.task;

import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.awt.Point;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Base class for all tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractTask implements Comparable<AbstractTask> {

    private final Date taskCreated;
    protected final WorldState worldState;
    private final Set<CreatureControl> assignees = new HashSet<>();

    public AbstractTask(final WorldState worldState) {
        this.taskCreated = new Date();
        this.worldState = worldState;
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
     * Unassing a creature from the job. A place for doing some cleanup
     *
     * @param creature
     */
    public void unassign(CreatureControl creature) {
        assignees.remove(creature);
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
        return (assignees.size() < getMaxAllowedNumberOfAsignees() && isValid() && isReachable(creature));
    }

    /**
     * Task priority, added to distance when evaluating tasks to give out. The
     * bigger the number, the less urgent the task is
     *
     * @return task priority
     */
    public int getPriority() {
        return 100;
    }

    @Override
    public int compareTo(AbstractTask t) {
        return getTaskCreated().compareTo(t.getTaskCreated());
    }

    /**
     * Is the task reachable by the given creature. Ask this last if determining
     * validity etc. As the method might be heavy
     *
     * @param creature the creature trying to reach this
     * @return is the task reachable
     */
    public boolean isReachable(CreatureControl creature) {
        Vector2f target = getTarget(creature);
        if (target != null) {
            return isReachable(creature, target);
        }
        return false;
    }

    /**
     * Is the task reachable by the given creature. Ask this last if determining
     * validity etc. As the method might be heavy
     *
     * @param creature the creature trying to reach this
     * @param target the target location
     * @return is the task reachable
     */
    protected boolean isReachable(CreatureControl creature, Vector2f target) {
        Point targetTile = new Point((int) Math.floor(target.x), (int) Math.floor(target.y));
        boolean hasAccessibleNeighbour = false;
        for (Point p : worldState.getMapLoader().getSurroundingTiles(targetTile, false)) {
            if (worldState.isAccessible(worldState.getMapData().getTile(p), creature.getCreature())) {
                hasAccessibleNeighbour = true;
                break; // At least one accessible point
            }
        }
        if (!hasAccessibleNeighbour) {
            return false;
        }

        // Path find
        return (worldState.findPath(worldState.getTileCoordinates(new Vector3f(creature.getPosition().x, 0, creature.getPosition().y)), targetTile, creature.getCreature()) != null);
    }

    /**
     * Get the task tooltip
     *
     * @return the task tooltip
     */
    public String getTooltip() {
        return Utils.getMainTextResourceBundle().getString(getStringId());
    }

    /**
     * The string ID for the dictionary
     *
     * @return string ID
     */
    protected abstract String getStringId();

    /**
     * Execute task!
     *
     * @param creature creature executing the task
     */
    public abstract void executeTask(CreatureControl creature);

    /**
     * Get the animation used for the task. Might be null if no animation is
     * tied to the task, sufficient to have the creature visit the location
     *
     * @param creature executing the task
     * @return the animation
     */
    public abstract ArtResource getTaskAnimation(CreatureControl creature);

    /**
     * The task icon for unit flowers
     *
     * @return the path t the icon
     */
    public abstract String getTaskIcon();

}
