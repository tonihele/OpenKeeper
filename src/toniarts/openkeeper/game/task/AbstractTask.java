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
import java.util.logging.Logger;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Base class for all tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractTask implements Task {

    private final Date taskCreated;
    protected final WorldState worldState;
    private final Set<CreatureControl> assignees = new HashSet<>();
    private static final Logger logger = Logger.getLogger(AbstractTask.class.getName());

    public AbstractTask(final WorldState worldState) {
        this.taskCreated = new Date();
        this.worldState = worldState;
    }

    @Override
    public Date getTaskCreated() {
        return taskCreated;
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        return 1;
    }

    @Override
    public void assign(CreatureControl creature, boolean setToCreature) {
        if (assignees.size() == getMaxAllowedNumberOfAsignees()) {
            logger.warning("Task already has the maximum number of assignees!");
        }
        assignees.add(creature);
        if (setToCreature) {
            creature.setAssignedTask(this);
        }
    }

    @Override
    public void unassign(CreatureControl creature) {
        assignees.remove(creature);
    }

    @Override
    public int getAssigneeCount() {
        return assignees.size();
    }

    @Override
    public boolean canAssign(CreatureControl creature) {
        return (assignees.size() < getMaxAllowedNumberOfAsignees() && isValid(creature) && isReachable(creature));
    }

    @Override
    public int getPriority() {
        return 100;
    }

    public int compareTo(AbstractTask t) {
        return getTaskCreated().compareTo(t.getTaskCreated());
    }

    @Override
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
            if (worldState.isAccessible(worldState.getMapData().getTile(p), creature)) {
                hasAccessibleNeighbour = true;
                break; // At least one accessible point
            }
        }
        if (!hasAccessibleNeighbour) {
            return false;
        }

        // Path find
        return (worldState.findPath(WorldState.getTileCoordinates(new Vector3f(creature.getPosition().x, 0, creature.getPosition().y)), targetTile, creature) != null);
    }

    @Override
    public boolean isFaceTarget() {
        return false;
    }

    @Override
    public String getTooltip() {
        return Utils.getMainTextResourceBundle().getString(getStringId());
    }

    /**
     * The string ID for the dictionary
     *
     * @return string ID
     */
    protected abstract String getStringId();

}
