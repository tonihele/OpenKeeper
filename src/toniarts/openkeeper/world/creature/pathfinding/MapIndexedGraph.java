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

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.indexed.DefaultIndexedGraph;
import com.badlogic.gdx.utils.Array;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.TileData;

/**
 * Map representation for the path finding
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapIndexedGraph extends DefaultIndexedGraph<TileData> {

    private final MapLoader mapLoader;
    private final KwdFile kwdFile;
    private final int nodeCount;
    private Creature creature;

    public MapIndexedGraph(MapLoader mapLoader, KwdFile kwdFile) {
        this.mapLoader = mapLoader;
        this.kwdFile = kwdFile;
        nodeCount = kwdFile.getMap().getHeight() * kwdFile.getMap().getWidth();
    }

    @Override
    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Set this prior to finding the path to search the path for certain
     * creature type
     *
     * @param creature the creature
     */
    public void setCreature(Creature creature) {
        this.creature = creature;
    }

    @Override
    public Array<Connection<TileData>> getConnections(TileData tile) {

        // The connections depend on the creature type
        // No diagonal movement
        Array<Connection<TileData>> connections = new Array<>(4);
        addIfValidCoordinate(tile, tile.getX(), tile.getY() - 1, connections); // North
        addIfValidCoordinate(tile, tile.getX() + 1, tile.getY(), connections); // East
        addIfValidCoordinate(tile, tile.getX(), tile.getY() + 1, connections); // South
        addIfValidCoordinate(tile, tile.getX() - 1, tile.getY(), connections); // West

        return connections;
    }

    private void addIfValidCoordinate(final TileData startTile, final int x, final int y, final Array<Connection<TileData>> connections) {
        if ((x >= 0 && x < kwdFile.getMap().getWidth() && y >= 0 && y < kwdFile.getMap().getHeight())) {

            // Valid coordinate
            TileData tile = mapLoader.getTile(x, y);
            Terrain terrain = tile.getTerrain();
            if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {

                // TODO: Rooms, obstacles and what not, should create an universal isAccessible(Creature) to map loader / world handler maybe
                if (creature != null) {

                    if (creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY)) {
                        connections.add(new DefaultConnection<>(startTile, tile)); // No cost
                        return;
                    } else if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA) && !creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_LAVA)) {
                        return;
                    } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER) && !creature.getFlags().contains(Creature.CreatureFlag.CAN_WALK_ON_WATER)) {
                        return;
                    }
                }
                connections.add(new MapConnection(startTile, tile));
            }
        }
    }

}
