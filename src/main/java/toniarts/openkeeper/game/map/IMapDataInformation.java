/*
 * Copyright (C) 2014-2020 OpenKeeper
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
package toniarts.openkeeper.game.map;

import toniarts.openkeeper.utils.Point;
import java.util.Iterator;
import java.util.List;

/**
 * Holds the map data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @param <T> the map tile data type
 */
public interface IMapDataInformation<T extends IMapTileInformation> extends Iterable<T> {

    int getHeight();

    default int getSize() {
        return getWidth() * getHeight();
    }

    /**
     * Get the tile data at x & y
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return the tile data
     */
    T getTile(int x, int y);

    /**
     * Get the tile data at point
     *
     * @param p Point
     * @return the tile data
     */
    default T getTile(Point p) {
        if (p != null) {
            return getTile(p.x, p.y);
        }

        return null;
    }

    int getWidth();

    /**
     * Get the share of the map a player owns, the tiles it owns out of all the
     * tiles
     *
     * @param playerId the player
     * @return the percentage of the map the player owns, rounded down
     */
    default int getOwnedPercentage(short playerId) {
        int owned = 0;
        for (T tile : this) {
            if (tile.getOwnerId() == playerId) {
                owned++;
            }
        }
        return owned * 100 / getSize();
    }

    void setTiles(List<T> mapTiles);

    @Override
    default Iterator<T> iterator() {
        return new MapIterator<>(this);
    }

}
