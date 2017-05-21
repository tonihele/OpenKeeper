/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.awt.Point;
import java.io.IOException;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * This is a holder for the map data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapData implements Savable {

    private int width;
    private int height;
    private MapTile[][] tiles;

    public MapData() {
        // For serialization
    }

    public MapData(KwdFile kwdFile) {
        width = kwdFile.getMap().getWidth();
        height = kwdFile.getMap().getHeight();

        // Duplicate the map
        this.tiles = new MapTile[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = kwdFile.getMap().getTile(x, y);
                tiles[x][y] = new MapTile(kwdFile, tile, kwdFile.getTerrain(tile.getTerrainId()), x, y, y * width + x);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Get the tile data at x & y
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return the tile data
     */
    public MapTile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        return this.tiles[x][y];
    }

    /**
     * Get the tile data at point
     *
     * @param p Point
     * @return the tile data
     */
    public MapTile getTile(Point p) {
        if (p != null) {
            return getTile(p.x, p.y);
        }

        return null;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(tiles, "tiles", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        Savable[][] savables = in.readSavableArray2D("tiles", new MapTile[0][0]);
        tiles = new MapTile[savables.length][savables[0].length];
        for (int row = 0; row < savables.length; row++) {
            for (int column = 0; column < savables[0].length; column++) {
                tiles[row][column] = (MapTile) savables[row][column];
                //tiles[row][column].setIndex(row * savables.length + column);
                //tiles[row][column].setPoint(new Point(row, column));
            }
        }
        width = tiles.length;
        height = tiles[0].length;
    }
}
