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
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * Claim a tile task, for workers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ClaimTileTask extends AbstractTileTask {

    public ClaimTileTask(WorldState worldState, int x, int y, short playerId) {
        super(worldState, x, y, playerId);
    }

    @Override
    public Vector2f getTarget(CreatureControl creature) {
        return new Vector2f(getTaskLocation().x + 0.5f, getTaskLocation().y + 0.5f);
    }

    @Override
    public boolean isValid() {
        return isValid(worldState, playerId, getTaskLocation().x, getTaskLocation().y);
    }

    private static boolean isConnectedToOwnedTiles(WorldState worldState, short playerId, Point location) {
        for (Point p : worldState.getMapLoader().getSurroundingTiles(location, false)) {
            TileData tile = worldState.getMapData().getTile(p);
            if (tile != null && tile.getPlayerId() == playerId && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValid(WorldState worldState, short playerId, int x, int y) {
        TileData tile = worldState.getMapData().getTile(x, y);
        return (tile.getPlayerId() != playerId && tile.getTerrain().getFlags().contains(Terrain.TerrainFlag.FILL_INABLE) && isConnectedToOwnedTiles(worldState, playerId, new Point(x, y)));
    }

    @Override
    public String toString() {
        return "Claim tile at " + getTaskLocation();
    }

}
