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
package toniarts.openkeeper.game.task.objective;

import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Send to action point task
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SendToActionPoint extends GoToTask {

    protected final ActionPoint actionPoint;
    private boolean executed = false;

    public SendToActionPoint(final INavigationService navigationService, final IMapController mapController, ActionPoint actionPoint, short playerId) {
        super(navigationService, mapController, (int) actionPoint.getCenter().x, (int) actionPoint.getCenter().y, playerId);

        this.actionPoint = actionPoint;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (!executed && creature != null) {

            // Check that the objectives are still the same
            return Thing.HeroParty.Objective.SEND_TO_ACTION_POINT.equals(creature.getObjective()) && actionPoint.getId() == creature.getObjectiveTargetActionPointId();
        }
        return !executed;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {

        // TODO: Wait delay
        executed = true;
        if (actionPoint.getNextWaypointId() != 0) {

            // Assign new objective
            creature.setObjectiveTargetActionPointId(actionPoint.getNextWaypointId());
        }
    }

}
