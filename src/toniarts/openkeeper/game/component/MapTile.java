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
package toniarts.openkeeper.game.component;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityComponent;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * Represents single map tile
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapTile implements EntityComponent {

    public final Map<Short, Boolean> selection = new HashMap<>();
    public final Map<Short, Boolean> flashing = new HashMap<>();
    public int randomTextureIndex;
    //public int health;
    //public int maxHealth;
    //public int gold;
    //public int manaGain;

    /* Refers to the room this tile holds */
    public boolean destroyed = false;

    public short terrainId;
    public Tile.BridgeTerrainType bridgeTerrainType;

    public Point p;
    public int index;

    public MapTile() {
        // For serialization
    }

    public MapTile(Tile tile, Terrain terrain, int x, int y, int index) {
        this.p = new Point(x, y);
        this.index = index;
        this.bridgeTerrainType = tile.getFlag();
        this.terrainId = tile.getTerrainId();
        //this.ownerId = tile.getPlayerId();

        // The water/lava under the bridge is set only when there is an actual bridge, but we might as well set it here, it doesn't change
        if (terrain.getFlags().contains(Terrain.TerrainFlag.LAVA)) {
            this.bridgeTerrainType = Tile.BridgeTerrainType.LAVA;
        } else if (terrain.getFlags().contains(Terrain.TerrainFlag.WATER)) {
            this.bridgeTerrainType = Tile.BridgeTerrainType.WATER;
        }

        // Set attributes
        setAttributesFromTerrain(this, terrain);
    }

    public static void setAttributesFromTerrain(MapTile tile, Terrain terrain) {
//        tile.health = terrain.getStartingHealth();
//        tile.maxHealth = terrain.getMaxHealth();
//        tile.gold = terrain.getGoldValue();
//        tile.manaGain = terrain.getManaGain();

        // Randomize the texture index, the terrain can change for sure but the changed types have no random textures
        // But for the principle, let it be here
        if (terrain.getFlags().contains(Terrain.TerrainFlag.RANDOM_TEXTURE)) {
            tile.randomTextureIndex = FastMath.nextRandomInt(0, terrain.getTextureFrames() - 1);
        }
    }

}
