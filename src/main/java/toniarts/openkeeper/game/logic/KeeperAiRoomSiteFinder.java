/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.logic;

import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.AI;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.Point;

/**
 * Finds the computer player a place to dig out for a new room, the way DK II
 * does. From the starting point it walks in one direction to the edge of the
 * rooms there, leaves the wanted gap and then needs a square of rock, or of
 * its own free land, centred on that line.
 */
public final class KeeperAiRoomSiteFinder {

    /**
     * A place for a room
     *
     * @param entrance the tile of the site the search line hits, the path to
     * the room is dug to here
     * @param corner the tile of the site to start from
     * @param oppositeCorner the last tile of the site
     */
    public record RoomSite(Point entrance, Point corner, Point oppositeCorner) {

    }

    /**
     * North, west, south and east. For each the step along the direction and
     * the steps the tiles of the site are walked in
     */
    private static final int[][] DIRECTIONS = {{0, -1, 1, -1}, {-1, 0, -1, 1}, {0, 1, -1, 1}, {1, 0, 1, -1}};

    private final IMapInformation<?> mapInformation;
    private final short playerId;
    private final AI ai;

    public KeeperAiRoomSiteFinder(IMapInformation<?> mapInformation, short playerId, AI ai) {
        this.mapInformation = mapInformation;
        this.playerId = playerId;
        this.ai = ai;
    }

    /**
     * Looks for a room site in one direction
     *
     * @param start where to start from, a tile of an own room
     * @param direction 0 north, 1 west, 2 south, 3 east
     * @param size the wanted size of the room in tiles
     * @return the site, or {@code null} if there is no room for it
     */
    public RoomSite find(Point start, int direction, int size) {
        int side = (int) Math.sqrt(size);
        int dx = DIRECTIONS[direction][0];
        int dy = DIRECTIONS[direction][1];
        int stepX = DIRECTIONS[direction][2];
        int stepY = DIRECTIONS[direction][3];

        // Out of the rooms, the room we start from has to end soon enough
        int x = start.x;
        int y = start.y;
        int steps = side * 2;
        while (steps != 0 && (isTagged(x, y) || isRoom(x, y))) {
            steps--;
            x += dx;
            y += dy;
        }
        if (steps == 0 || isBlocked(x, y)) {
            return null;
        }

        // The gap between the rooms
        for (int i = 0; i < ai.getTilesLeftBetweenRooms(); i++) {
            x += dx;
            y += dy;
            if (isBlocked(x, y)) {
                return null;
            }
        }
        Point entrance = new Point(x, y);

        // The square
        x += dy * (side / 2);
        y += dx * (side / 2);
        for (int row = 0; row < side; row++) {
            for (int column = 0; column < side; column++) {
                if (!isFree(x + column * stepX, y + row * stepY)) {
                    return null;
                }
            }
        }
        if (isTooOpen(countOpenBorder(x, y, stepX, stepY, side), side * 4 + 4)) {
            return null;
        }

        // A size that is not square gets one more row at the far end
        int width = side;
        int height = side;
        if (size > side * side) {
            int open = 0;
            for (int i = 0; i < side; i++) {
                int rowX = dx == 0 ? x + i * stepX : x + side * stepX;
                int rowY = dx == 0 ? y + side * stepY : y + i * stepY;
                if (isTagged(rowX, rowY) || !isTaggable(rowX, rowY)) {
                    return null;
                }
                if (isOpen(rowX + (dx == 0 ? 0 : stepX), rowY + (dx == 0 ? stepY : 0))) {
                    open++;
                }
            }
            if (isTooOpen(open, side)) {
                return null;
            }

            // DK II only adds the row when a gap is left between the rooms
            if (ai.getTilesLeftBetweenRooms() > 0) {
                if (dx == 0) {
                    height++;
                } else {
                    width++;
                }
            }
        }

        // The whole site, and the tile past its far corner, must be in the build area
        if (!isInBuildArea(x, y) || !isInBuildArea(x + stepX * width, y + stepY * height)) {
            return null;
        }

        return new RoomSite(entrance, new Point(x, y), new Point(x + stepX * (width - 1), y + stepY * (height - 1)));
    }

    /**
     * Counts the tiles around the square that are already open or tagged
     */
    private int countOpenBorder(int x, int y, int stepX, int stepY, int side) {
        int open = 0;
        for (int i = 0; i < side; i++) {
            if (isOpen(x - stepX, y + i * stepY)) {
                open++;
            }
            if (isOpen(x + side * stepX, y + i * stepY)) {
                open++;
            }
        }
        for (int i = -1; i <= side; i++) {
            if (isOpen(x + i * stepX, y - stepY)) {
                open++;
            }
            if (isOpen(x + i * stepX, y + side * stepY)) {
                open++;
            }
        }

        return open;
    }

    /**
     * The openness is how many percent of the tiles around a room may be
     * open. It only counts when a gap is left between the rooms
     */
    private boolean isTooOpen(int open, int border) {
        return ai.getTilesLeftBetweenRooms() > 0 && open * 100 / border > ai.getOpenness();
    }

    private boolean isInBuildArea(int x, int y) {
        return x >= ai.getBuildAreaStartX() && x <= ai.getBuildAreaEndX()
                && y >= ai.getBuildAreaStartY() && y <= ai.getBuildAreaEndY();
    }

    private boolean isTagged(int x, int y) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(x, y);

        return tile != null && tile.isSelected(playerId);
    }

    private boolean isRoom(int x, int y) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(x, y);

        return tile != null && tile.getRoomId() != null;
    }

    private boolean isTaggable(int x, int y) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(x, y);

        return tile != null && mapInformation.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    /**
     * Off the map, or rock that can't be dug
     */
    private boolean isBlocked(int x, int y) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(x, y);
        if (tile == null) {
            return true;
        }
        Terrain terrain = mapInformation.getTerrain(tile);

        return terrain.getFlags().contains(Terrain.TerrainFlag.SOLID)
                && !terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE);
    }

    /**
     * Rock to dig out, or own land without a room
     */
    private boolean isFree(int x, int y) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(x, y);

        return tile != null && (isTaggable(x, y) || (tile.getOwnerId() == playerId && tile.getRoomId() == null));
    }

    private boolean isOpen(int x, int y) {
        IMapTileInformation tile = mapInformation.getMapData().getTile(x, y);

        return tile != null && (tile.isSelected(playerId)
                || !mapInformation.getTerrain(tile).getFlags().contains(Terrain.TerrainFlag.SOLID));
    }

}
