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
package toniarts.openkeeper.utils;

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;

/**
 * Contains transforms from tile indexes and world coordinates
 *
 * @author archdemon
 */
public final class WorldUtils {

    public static final float TILE_HEIGHT = 1;
    public static final float TILE_WIDTH = 1;
    public static final float UNDERFLOOR_HEIGHT = 0 * TILE_HEIGHT;
    public static final float FLOOR_HEIGHT = 1 * TILE_HEIGHT;
    public static final float WATER_LEVEL = FLOOR_HEIGHT - 0.07F;
    public static final float TOP_HEIGHT = 2 * TILE_HEIGHT;
    public static final float DROP_HEIGHT = FLOOR_HEIGHT + 0.5f * TILE_HEIGHT;

    private WorldUtils() {
        // Nope
    }

    /**
     * Get a standard camera position vector on given map point
     *
     * @param x tile x coordinate
     * @param y tile y coordinate
     * @return position on 3D world with y = 0
     */
    public static Vector3f pointToVector3f(final int x, final int y) {
        return new Vector3f(x * TILE_WIDTH, 0, y * TILE_WIDTH);
    }

    /**
     * calculates position from center ActionPoint
     *
     * @param ap
     * @return position on 3D world with y = 0
     */
    public static Vector3f ActionPointToVector3f(final ActionPoint ap) {

        return new Vector3f(
                (ap.getStart().x + ap.getEnd().x) / 2.0f * TILE_WIDTH,
                0,
                (ap.getStart().y + ap.getEnd().y) / 2.0f * TILE_WIDTH);
    }

    /**
     * calculates position from center ActionPoint
     *
     * @param ap
     * @return position on 2D world
     */
    public static Vector2f ActionPointToVector2f(final ActionPoint ap) {

        return new Vector2f(
                (ap.getStart().x + ap.getEnd().x) / 2.0f * TILE_WIDTH,
                (ap.getStart().y + ap.getEnd().y) / 2.0f * TILE_WIDTH);
    }

    /**
     *
     * @param p
     * @return position on 3D world with y = 0
     */
    public static Vector3f pointToVector3f(final Point p) {
        return pointToVector3f(p.x, p.y);
    }

    public static Vector2f pointToVector2f(final int x, final int y) {
        return new Vector2f(x * TILE_WIDTH, y * TILE_WIDTH);
    }

    public static Vector2f pointToVector2f(final Point p) {
        return pointToVector2f(p.x, p.y);
    }

    /**
     * Whether there are no solid tiles between two points on the map. The
     * tiles the points are on don't count. Like DK II it goes along the line
     * from tile to tile, so every tile the line passes through counts, and
     * where it passes through a corner, both tiles beside the corner count
     * (0x4512F0).
     *
     * @param mapInformation the map
     * @param from the start of the line
     * @param to the end of the line
     * @return true if nothing solid is in the way
     */
    public static boolean hasLineOfSight(IMapInformation<?> mapInformation, Vector3f from, Vector3f to) {
        Point tile = vectorToPoint(from);
        Point endTile = vectorToPoint(to);
        float dx = (to.x - from.x) / TILE_WIDTH;
        float dz = (to.z - from.z) / TILE_WIDTH;
        int stepX = endTile.x > tile.x ? 1 : -1;
        int stepZ = endTile.y > tile.y ? 1 : -1;

        // How far along the line the next tile edge on each axis is, a tile spans half a tile either side of its centre
        float edgeX = dx == 0 ? Float.MAX_VALUE : (tile.x + stepX * 0.5f - from.x / TILE_WIDTH) / dx;
        float edgeZ = dz == 0 ? Float.MAX_VALUE : (tile.y + stepZ * 0.5f - from.z / TILE_WIDTH) / dz;
        float stepAlongX = dx == 0 ? Float.MAX_VALUE : Math.abs(1 / dx);
        float stepAlongZ = dz == 0 ? Float.MAX_VALUE : Math.abs(1 / dz);
        int tilesLeft = Math.abs(endTile.x - tile.x) + Math.abs(endTile.y - tile.y);
        while (tilesLeft > 0) {
            if (edgeX < edgeZ) {
                tile = new Point(tile.x + stepX, tile.y);
                edgeX += stepAlongX;
                tilesLeft--;
            } else if (edgeZ < edgeX) {
                tile = new Point(tile.x, tile.y + stepZ);
                edgeZ += stepAlongZ;
                tilesLeft--;
            } else {
                if (mapInformation.isSolid(new Point(tile.x + stepX, tile.y)) || mapInformation.isSolid(new Point(tile.x, tile.y + stepZ))) {
                    return false;
                }
                tile = new Point(tile.x + stepX, tile.y + stepZ);
                edgeX += stepAlongX;
                edgeZ += stepAlongZ;
                tilesLeft -= 2;
            }
            if (tilesLeft > 0 && mapInformation.isSolid(tile)) {
                return false;
            }
        }

        return true;
    }

    public static Vector2 pointToVector2(final int x, final int y) {
        return new Vector2(x * TILE_WIDTH, y * TILE_WIDTH);
    }

    public static Vector2 pointToVector2(final Point p) {
        return pointToVector2(p.x, p.y);
    }

    public static Point vectorToPoint(final Vector2f v) {
        return vectorToPoint(v.x, v.y);
    }

    public static Point vectorToPoint(final Vector2 v) {
        return vectorToPoint(v.x, v.y);
    }

    public static Point vectorToPoint(final Vector3f v) {
        return vectorToPoint(v.x, v.z);
    }

    public static Point vectorToPoint(final float x, final float y) {
        return new Point(Math.round(x / TILE_WIDTH), Math.round(y / TILE_WIDTH));
    }

    public static Vector2 vector3fToVector2(Vector3f v) {
        return new Vector2(v.x, v.z);
    }

    public static Vector3f vector2fToVector3(Vector2f v) {
        return new Vector3f(v.x, TILE_HEIGHT / 2f, v.y);
    }

    public static Vector2f vector3fToVector2f(Vector3f v) {
        return new Vector2f(v.x, v.z);
    }

    /**
     * Get surrounding tile coordinates
     *
     * @param mapData the map data
     * @param point starting coordinate whose surroundings you want
     * @param diagonal whether to also include diagonally attached tiles
     * @return surrounding tile coordinates
     */
    public static Point[] getSurroundingTiles(IMapDataInformation mapData, Point point, boolean diagonal) {

        // Get all surrounding tiles
        List<Point> tileCoords = new ArrayList<>(diagonal ? 9 : 5);
        tileCoords.add(point);

        addIfValidCoordinate(mapData, point.x, point.y - 1, tileCoords); // North
        addIfValidCoordinate(mapData, point.x + 1, point.y, tileCoords); // East
        addIfValidCoordinate(mapData, point.x, point.y + 1, tileCoords); // South
        addIfValidCoordinate(mapData, point.x - 1, point.y, tileCoords); // West
        if (diagonal) {
            addIfValidCoordinate(mapData, point.x - 1, point.y - 1, tileCoords); // NW
            addIfValidCoordinate(mapData, point.x + 1, point.y - 1, tileCoords); // NE
            addIfValidCoordinate(mapData, point.x - 1, point.y + 1, tileCoords); // SW
            addIfValidCoordinate(mapData, point.x + 1, point.y + 1, tileCoords); // SE
        }

        return tileCoords.toArray(Point[]::new);
    }

    private static void addIfValidCoordinate(IMapDataInformation mapData, final int x, final int y, List<Point> tileCoords) {
        IMapTileInformation tile = mapData.getTile(x, y);
        if (tile != null) {
            tileCoords.add(tile.getLocation());
        }
    }

    /**
     * Calculates manhattan distance between two points
     *
     * @param p1 point 1
     * @param p2 point 2
     * @return distance between the to points
     */
    public static int calculateDistance(Point p1, Point p2) {
        if (p1 == null || p2 == null) {
            return Short.MAX_VALUE; // With the points added, int max value would overflow
        }
        return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

}
