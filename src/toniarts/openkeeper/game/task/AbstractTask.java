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
import java.awt.Point;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Base class for all tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractTask implements Task {

    private final Date taskCreated;
    protected final INavigationService navigationService;
    protected final IMapController mapController;
    private final Map<ICreatureController, Float> assignees = new HashMap<>();
    private static final Logger LOGGER = Logger.getLogger(AbstractTask.class.getName());

    public AbstractTask(final INavigationService navigationService, final IMapController mapController) {
        this.taskCreated = new Date();
        this.navigationService = navigationService;
        this.mapController = mapController;
    }

    /**
     * Get the non-stop execution duration of a creature
     *
     * @param creature the executing creature
     * @return the execution duration
     */
    protected float getExecutionDuration(ICreatureController creature) {
        return assignees.get(creature);
    }

    /**
     * Get the non-stop execution duration of a creature
     *
     * @param creature the executing creature
     * @param duration the execution duration to store
     */
    protected void setExecutionDuration(ICreatureController creature, float duration) {
        assignees.put(creature, duration);
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
    public void assign(ICreatureController creature, boolean setToCreature) {
        if (assignees.size() == getMaxAllowedNumberOfAsignees()) {
            LOGGER.warning("Task already has the maximum number of assignees!");
        }
        assignees.put(creature, 0.0f);
        if (setToCreature) {
            creature.setAssignedTask(this);
        }
    }

    @Override
    public void unassign(ICreatureController creature) {
        assignees.remove(creature);
    }

    @Override
    public int getAssigneeCount() {
        return assignees.size();
    }

    @Override
    public boolean canAssign(ICreatureController creature) {
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
    public boolean isReachable(ICreatureController creature) {
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
    protected boolean isReachable(ICreatureController creature, Vector2f target) {
        Point targetTile = WorldUtils.vectorToPoint(target);
        boolean hasAccessibleNeighbour = false;
        for (Point p : WorldUtils.getSurroundingTiles(mapController.getMapData(), targetTile, false)) {
            if (navigationService.isAccessible(mapController.getMapData().getTile(p), mapController.getMapData().getTile(targetTile), creature)) {
                hasAccessibleNeighbour = true;
                break; // At least one accessible point
            }
        }
        if (!hasAccessibleNeighbour) {
            return false;
        }

        // Path find
        return (navigationService.findPath(WorldUtils.vectorToPoint(creature.getPosition()), targetTile, creature) != null);
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

    @Override
    public boolean isRemovable() {
        return !isValid(null);
    }

}
