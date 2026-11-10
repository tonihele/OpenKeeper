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
package toniarts.openkeeper.tools.convert.map;

/**
 * Container class for the levelnameMap.kld
 *
 *
 * @author Wizand Petteri Loisko petteri.loisko@gmail.com
 * @author ArchDemon
 *
 * Thank you https://github.com/werkt
 */
public final class GameMap {

    private Tile[][] tiles;
    private int width;
    private int height;
    
    private Terrain water;
    private Terrain lava;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
    }

    protected void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    protected void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    protected void setTiles(Tile[][] tiles) {
        this.tiles = tiles;
    }

    protected void setTile(int x, int y, Tile tile) {
        this.tiles[x][y] = tile;
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public Tile getTile(int x, int y) {
        return this.tiles[x][y];
    }
    
    protected void setLava(Terrain lava) {
        this.lava = lava;
    }
    
    /**
     * Get the lava terrain tile
     *
     * @return lava
     */
    public Terrain getLava() {
        return lava;
    }
    
    protected void setWater(Terrain water) {
        this.water = water;
    }

    /**
     * Get the water terrain tile
     *
     * @return water
     */
    public Terrain getWater() {
        return water;
    }
}
