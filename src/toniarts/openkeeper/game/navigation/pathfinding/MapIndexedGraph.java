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
package toniarts.openkeeper.game.navigation.pathfinding;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.DefaultConnection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedGraph;
import com.badlogic.gdx.utils.Array;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.MapTile;

/**
 * Map representation for the path finding
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapIndexedGraph implements IndexedGraph<MapTile> {

    private final IMapController mapController;
    private final IGameWorldController gameWorldController;
    private final int nodeCount;
    private INavigable pathFindable;

    public MapIndexedGraph(IGameWorldController gameWorldController, IMapController mapController) {
        this.mapController = mapController;
        this.gameWorldController = gameWorldController;
        nodeCount = mapController.getMapData().getHeight() * mapController.getMapData().getWidth();
    }

    @Override
    public int getNodeCount() {
        return nodeCount;
    }

    @Override
    public int getIndex(MapTile n) {
        return n.getIndex();
    }

    /**
     * Set this prior to finding the path to search the path for certain path
     * findable type
     *
     * @param pathFindable the path findable
     */
    public void setPathFindable(INavigable pathFindable) {
        this.pathFindable = pathFindable;
    }

    @Override
    public Array<Connection<MapTile>> getConnections(MapTile tile) {

        // The connections depend on the creature type
        Array<Connection<MapTile>> connections = new Array<>(pathFindable.canMoveDiagonally() ? 8 : 4);
        boolean valids[] = new boolean[4];

        valids[0] = addIfValidCoordinate(tile, tile.getX(), tile.getY() - 1, connections); // North
        valids[1] = addIfValidCoordinate(tile, tile.getX() + 1, tile.getY(), connections); // East
        valids[2] = addIfValidCoordinate(tile, tile.getX(), tile.getY() + 1, connections); // South
        valids[3] = addIfValidCoordinate(tile, tile.getX() - 1, tile.getY(), connections); // West

        if (pathFindable.canMoveDiagonally()) {
            if (valids[0] && valids[1]) { // North-East
                addIfValidCoordinate(tile, tile.getX() + 1, tile.getY() - 1, connections);
            }
            if (valids[0] && valids[3]) { // North-West
                addIfValidCoordinate(tile, tile.getX() - 1, tile.getY() - 1, connections);
            }
            if (valids[2] && valids[1]) { // South-East
                addIfValidCoordinate(tile, tile.getX() + 1, tile.getY() + 1, connections);
            }
            if (valids[2] && valids[3]) { // South-West
                addIfValidCoordinate(tile, tile.getX() - 1, tile.getY() + 1, connections);
            }
        }

        return connections;
    }

    private boolean addIfValidCoordinate(final MapTile startTile, final int x, final int y, final Array<Connection<MapTile>> connections) {

        // Valid coordinate
        MapTile tile = mapController.getMapData().getTile(x, y);
        if (tile != null) {
            Float cost = pathFindable.getCost(startTile, tile, gameWorldController, mapController);
            if (cost != null) {
                connections.add(new DefaultConnection<MapTile>(startTile, tile) {

                    @Override
                    public float getCost() {
                        return cost;
                    }

                });
                return true;
            }
        }
        return false;
    }

}
