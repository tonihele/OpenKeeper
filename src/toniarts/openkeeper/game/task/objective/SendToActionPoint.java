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

import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.ai.ICreatureController;

/**
 * Send to action point task
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SendToActionPoint extends GoToTask {

    protected final ActionPoint actionPoint;
    private boolean executed = false;

    public SendToActionPoint(final IGameWorldController gameWorldController, final IMapController mapController, ActionPoint actionPoint, short playerId) {
        super(gameWorldController, mapController, (int) actionPoint.getCenter().x, (int) actionPoint.getCenter().y, playerId);

        this.actionPoint = actionPoint;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (!executed && creature != null) {

            // Check that the objectives are still the same
            //return Thing.HeroParty.Objective.SEND_TO_ACTION_POINT.equals(creature.getObjective()) && actionPoint.equals(creature.getObjectiveTargetActionPoint());
        }
        return !executed;
    }

    @Override
    public void executeTask(ICreatureController creature) {

        // TODO: Wait delay
        executed = true;
        if (actionPoint.getNextWaypointId() != 0) {

            // Assign new objective
            //creature.setObjectiveTargetActionPoint(worldState.getGameState().getActionPointState().getActionPoint(actionPoint.getNextWaypointId()));
        }
    }

}
