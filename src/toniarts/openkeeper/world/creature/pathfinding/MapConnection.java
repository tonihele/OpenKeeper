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
package toniarts.openkeeper.world.creature.pathfinding;

import com.badlogic.gdx.ai.pfa.DefaultConnection;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.TileData;

/**
 * Or map connection. Gives water & lava a cost. I think it is shared amongst
 * the creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapConnection extends DefaultConnection<TileData> {

    private static final float WATER_LAVA_PENALTY = 0.4f;

    public MapConnection(TileData fromNode, TileData toNode) {
        super(fromNode, toNode);
    }

    @Override
    public float getCost() {
        float cost = 1f;
        cost = calculatePenalty(fromNode, cost);
        cost = calculatePenalty(toNode, cost);
        return cost;
    }

    private float calculatePenalty(TileData node, float cost) {
        if (node.getTerrain().getFlags().contains(Terrain.TerrainFlag.LAVA) || node.getTerrain().getFlags().contains(Terrain.TerrainFlag.WATER)) {
            cost -= WATER_LAVA_PENALTY;
        }
        return cost;
    }

}
