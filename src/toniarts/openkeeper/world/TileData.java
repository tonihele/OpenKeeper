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
package toniarts.openkeeper.world;

import java.awt.Point;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * Wrapper for a map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TileData extends Tile {

    private boolean selected = false;
    private boolean flashed = false;
    private short selectedByPlayerId = 0;
    private Integer randomTextureIndex;
    private Terrain terrain;
    private int health;
    private int gold;
    private final Point p;
    private final int index;
    private final KwdFile kwdFile;
    private final static ResourceBundle bundle = Main.getResourceBundle("Interface/Texts/Text");

    protected TileData(KwdFile kwdFile, Tile tile, Terrain terrain, int x, int y, int index) {
        this.p = new Point(x, y);
        this.index = index;
        this.kwdFile = kwdFile;
        this.terrain = terrain;
        this.setFlag(tile.getFlag());
        this.setPlayerId(tile.getPlayerId());
        this.setTerrainId(tile.getTerrainId());
        this.setUnknown(tile.getUnknown());

        // The water/lava under the bridge is set only when there is an actual bridge, but we might as well set it here, it doesn't change
        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            this.setFlag(BridgeTerrainType.LAVA);
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            this.setFlag(BridgeTerrainType.WATER);
        }

        // Set attributes
        setAttributesFromTerrain();
    }

    public boolean isSelected() {
        return selected;
    }

    protected void setSelected(boolean selected, short playerId) {
        this.selected = selected;
        selectedByPlayerId = playerId;
    }

    public boolean isFlashed() {
        return flashed;
    }

    public void setFlashed(boolean flashed) {
        this.flashed = flashed;
    }

    public short getSelectedByPlayerId() {
        return selectedByPlayerId;
    }

    public boolean isSelectedByPlayerId(short playerId) {
        return (selected && selectedByPlayerId == playerId);
    }

    @Override
    protected void setPlayerId(short playerId) {
        super.setPlayerId(playerId);
    }

    @Override
    protected void setTerrainId(short terrainId) {
        super.setTerrainId(terrainId);
        if (terrain.getTerrainId() != terrainId) {

            // A change
            terrain = kwdFile.getTerrain(getTerrainId());
            setAttributesFromTerrain();

            // If the terrain is not taggable anymore, reset the tagging data
            if (!terrain.getFlags().contains(Terrain.TerrainFlag.TAGGABLE)) {
                setSelected(false, (short) 0);
            }
            // FIXME realy need?
            setFlashed(false);
        }
    }

    public Integer getRandomTextureIndex() {
        return randomTextureIndex;
    }

    protected void setRandomTextureIndex(Integer randomTextureIndex) {
        this.randomTextureIndex = randomTextureIndex;
    }

    public int getX() {
        return p.x;
    }

    public int getY() {
        return p.y;
    }

    public Point getLocation() {
        return p;
    }

    public int getIndex() {
        return index;
    }

    /**
     * Get the terrain type of this terrain
     *
     * @return the terrain
     */
    public Terrain getTerrain() {
        return terrain;
    }

    private void setAttributesFromTerrain() {
        health = terrain.getStartingHealth();
        gold = terrain.getGoldValue();
    }

    public int getHealth() {
        return health;
    }

    public int getGold() {
        return gold;
    }

    public String getTooltip() {
        return bundle.getString(Integer.toString(getTerrain().getTooltipStringId()))
                .replaceAll("%37", Integer.toString(getHealthPercent()))
                .replaceAll("%66", Integer.toString(terrain.getManaGain()))
                .replaceAll("%67", Integer.toString(gold));
    }

    protected Integer getHealthPercent() {
        return Math.round((float) health / terrain.getMaxHealth() * 100);
    }

    /**
     * Apply damage to the tile
     *
     * @param damage amount of damage
     * @return true if the tile is "dead"
     */
    public boolean applyDamage(int damage) {
        health -= damage;
        return (health <= 0);
    }

    /**
     * Apply healing to the tile
     *
     * @param healing amount of healing
     * @return true if the tile is at max
     */
    public boolean applyHealing(int healing) {
        health = Math.min(getTerrain().getMaxHealth(), health + healing);
        return (health == getTerrain().getMaxHealth());
    }

    /**
     * Mine for gold
     *
     * @param damage the amount of gold requested
     * @return the amount of gold got
     */
    public int mineGold(int damage) {
        int minedAmount = Math.min(damage, gold);
        gold -= minedAmount;
        return minedAmount;
    }

    /**
     * Is tile at full health
     *
     * @return true if full health
     */
    public boolean isAtFullHealth() {
        return (health == getTerrain().getMaxHealth());
    }

}
