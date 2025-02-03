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
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import static toniarts.openkeeper.game.navigation.pathfinding.INavigable.DEFAULT_COST;
import static toniarts.openkeeper.game.navigation.pathfinding.INavigable.WATER_COST;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Map representation for the path finding
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapIndexedGraph implements IndexedGraph<IMapTileInformation> {

    private final IMapController mapController;
    private final IEntityPositionLookup entityPositionLookup;
    private final int nodeCount;
    private INavigable pathFindable;

    public MapIndexedGraph(IMapController mapController, IEntityPositionLookup entityPositionLookup) {
        this.mapController = mapController;
        this.entityPositionLookup = entityPositionLookup;

        nodeCount = mapController.getMapData().getHeight() * mapController.getMapData().getWidth();
    }

    @Override
    public int getNodeCount() {
        return nodeCount;
    }

    @Override
    public int getIndex(IMapTileInformation n) {
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
    public Array<Connection<IMapTileInformation>> getConnections(IMapTileInformation tile) {

        // The connections depend on the creature type
        Array<Connection<IMapTileInformation>> connections = new Array<>(pathFindable.canMoveDiagonally() ? 8 : 4);
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

    private boolean addIfValidCoordinate(final IMapTileInformation startTile, final int x, final int y, final Array<Connection<IMapTileInformation>> connections) {

        // Valid coordinate
        IMapTileInformation tile = mapController.getMapData().getTile(x, y);
        if (tile != null) {
            Float cost = pathFindable.getCost(startTile, tile, mapController, entityPositionLookup);
            if (cost != null) {
                connections.add(new DefaultConnection<IMapTileInformation>(startTile, tile) {

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

    /**
     * Can the entity travel from A to B?
     *
     * @param navigable the navigable entity
     * @param from the tile we are traversing from, always the adjacent tile
     * which we know already being accessible
     * @param to the tile we are travelling to
     * @param mapController the map controller
     * @param entityPositionLookup entity position lookup
     * @see #DEFAULT_COST
     * @see #WATER_COST
     * @return {@code null} if the to tile is not accessible
     */
    protected static Float getCost(final INavigable navigable, final IMapTileInformation from, final IMapTileInformation to, final IMapController mapController,
            IEntityPositionLookup entityPositionLookup) {
        return getCost(navigable, from, to, mapController, entityPositionLookup, true);
    }

    private static Float getCost(final INavigable navigable, final IMapTileInformation from, final IMapTileInformation to, final IMapController mapController,
            IEntityPositionLookup entityPositionLookup, boolean checkDiagonal) {
        Terrain terrain = mapController.getTerrain(to);
        if (!terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)) {

            // We can never squeeze through obstacles, even if able to move diagonally
            if (checkDiagonal && from != null && from.getX() != to.getX() && from.getY() != to.getY()) {
                if (!navigable.canMoveDiagonally()) {
                    return null;
                }

                // Get the 2 neighbouring tiles (corners kinda)
                /*
                Consider cases like:
                [1,1][ h ]
                [ l ][2,2]
                   and
                [ h ][2,1]
                [1,2][ l ]
                 */
                boolean hasConnection = false;
                IMapTileInformation hiCorner = mapController.getMapData().getTile(from.getLocation().y < to.getLocation().y ? to.getLocation().x : from.getLocation().x, from.getLocation().y < to.getLocation().y ? from.getLocation().y : to.getLocation().y);
                IMapTileInformation loCorner = mapController.getMapData().getTile(from.getLocation().y > to.getLocation().y ? to.getLocation().x : from.getLocation().x, from.getLocation().y > to.getLocation().y ? from.getLocation().y : to.getLocation().y);
                if (hiCorner != null && getCost(navigable, from, hiCorner, mapController, entityPositionLookup, false) != null) {
                    hasConnection = true;
                } else if (loCorner != null && getCost(navigable, from, loCorner, mapController, entityPositionLookup, false) != null) {
                    hasConnection = true;
                }

                if (!hasConnection) {
                    return null;
                }
            }

            // Check for doors etc.
            if (entityPositionLookup.isTileBlocked(to, navigable.getOwnerId())) {
                return null;
            }

            // Check terrain
            if (terrain.getFlags().contains(Terrain.TerrainFlag.ROOM)) {

                // Get room obstacles
                RoomInstance roomInstance = mapController.getRoomInstanceByCoordinates(to.getLocation());
                IRoomController room = mapController.getRoomController(roomInstance);
                return room.isTileAccessible(from != null ? from.getLocation() : null, to.getLocation()) ? DEFAULT_COST : null;
            } else if (navigable.canFly()) {
                return DEFAULT_COST;
            } else if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA) && !navigable.canWalkOnLava()) {
                return null;
            } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
                if (navigable.canWalkOnWater()) {
                    return WATER_COST;
                }
                return null;
            }
            return DEFAULT_COST;
        }
        return null;
    }

}
