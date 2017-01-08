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

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.action.ActionPoint;
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Send to action point task
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SendToActionPoint extends AbstractTileTask {

    protected final ActionPoint actionPoint;
    private boolean executed = false;

    public SendToActionPoint(WorldState worldState, ActionPoint actionPoint, short playerId) {
        super(worldState, (int) actionPoint.getCenter().x, (int) actionPoint.getCenter().y, playerId);

        this.actionPoint = actionPoint;
    }

    @Override
    public boolean isValid(CreatureControl creature) {
        if (!executed && creature != null) {

            // Check that the objectives are still the same
            return Thing.HeroParty.Objective.SEND_TO_ACTION_POINT.equals(creature.getObjective()) && actionPoint.equals(creature.getObjectiveTargetActionPoint());
        }
        return !executed;
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return new Vector2f(getTaskLocation().x + 0.5f, getTaskLocation().y + 0.5f);
    }

    @Override
    protected String getStringId() {
        return "2670";
    }

    @Override
    public void executeTask(CreatureControl creature) {
        // TODO: Wait delay
        executed = true;
        if (actionPoint.getNextWaypointId() != 0) {

            // Assign new objective
            creature.setObjectiveTargetActionPoint(worldState.getGameState().getActionPointState().getActionPoint(actionPoint.getNextWaypointId()));
        }
    }

    @Override
    public ArtResource getTaskAnimation(CreatureControl creature) {
        return null;
    }

    @Override
    public String getTaskIcon() {
        return null;
    }

}
