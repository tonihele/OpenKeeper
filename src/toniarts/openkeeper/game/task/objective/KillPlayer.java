/*
 * Copyright (C) 2014-2017 OpenKeeper
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
import toniarts.openkeeper.game.task.AbstractTileTask;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Kill player objective for those goodly heroes
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KillPlayer extends AbstractTileTask {

    protected final short targetPlayerId;

    public KillPlayer(WorldState worldState, short targetPlayerId, short playerId) {
        super(worldState, worldState.getGameState().getPlayer(targetPlayerId).getRoomControl().getDungeonHeart().getRoomInstance().getCoordinates().get(0).x, worldState.getGameState().getPlayer(targetPlayerId).getRoomControl().getDungeonHeart().getRoomInstance().getCoordinates().get(0).y, playerId);

        this.targetPlayerId = targetPlayerId;
    }

    @Override
    public boolean isValid(CreatureControl creature) {
        if (!isPlayerDestroyed() && creature != null) {

            // Check that the objectives are still the same
            return Thing.HeroParty.Objective.KILL_PLAYER.equals(creature.getObjective()) && Short.valueOf(targetPlayerId).equals(creature.getObjectiveTargetPlayerId());
        }
        return !isPlayerDestroyed();
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return new Vector2f(getTaskLocation().x + 0.5f, getTaskLocation().y + 0.5f);
    }

    @Override
    protected String getStringId() {
        return "2645";
    }

    @Override
    public void executeTask(CreatureControl creature) {

    }

    @Override
    public ArtResource getTaskAnimation(CreatureControl creature) {
        return null;
    }

    @Override
    public String getTaskIcon() {
        return null;
    }

    private boolean isPlayerDestroyed() {
        return worldState.getGameState().getPlayer(targetPlayerId).isDestroyed();
    }

}
