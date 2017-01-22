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
package toniarts.openkeeper.game.task.worker;

import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Claim a wall task
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClaimWallTileTask extends DigTileTask {

    public ClaimWallTileTask(WorldState worldState, int x, int y, short playerId) {
        super(worldState, x, y, playerId);
    }

    @Override
    public boolean isValid(CreatureControl creature) {
        TileData tile = worldState.getMapData().getTile(getTaskLocation());
        return worldState.isClaimableWall(getTaskLocation().x, getTaskLocation().y, playerId) && !tile.isSelectedByPlayerId(playerId);
    }

    @Override
    public int getMaxAllowedNumberOfAsignees() {
        // TODO: I think it is 1 per accessible side
        return 1;
    }

    @Override
    public int getPriority() {
        return 176;
    }

    @Override
    public String toString() {
        return "Claim wall at " + getTaskLocation();
    }

    @Override
    protected String getStringId() {
        return "2603";
    }

    @Override
    public void executeTask(CreatureControl creature) {
        worldState.applyClaimTile(getTaskLocation(), playerId);
    }

    @Override
    public ArtResource getTaskAnimation(CreatureControl creature) {
        return creature.getCreature().getAnimSleepResource();
    }

    @Override
    public String getTaskIcon() {
        return "Textures/GUI/moods/SJ-Reinforce.png";
    }

}
