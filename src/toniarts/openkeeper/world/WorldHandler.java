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

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.scene.Node;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.view.selection.SelectionArea;

/**
 * Handles the handling of game world, physics & visual wise
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class WorldHandler {

    private final MapLoader mapLoader;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final Node worldNode;

    public WorldHandler(final AssetManager assetManager, final KwdFile kwdFile, final BulletAppState bulletAppState) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;

        // World node
        worldNode = new Node("World");

        // Create the actual map
        this.mapLoader = new MapLoader(assetManager, kwdFile) {
            @Override
            protected void updateProgress(int progress, int max) {
                WorldHandler.this.updateProgress(progress, max);
            }
        };
        worldNode.attachChild(mapLoader.load(assetManager, kwdFile));

        // Things
        worldNode.attachChild(new ThingLoader().load(bulletAppState, assetManager, kwdFile));
    }

    /**
     * If you want to monitor the map loading progress, use this method
     *
     * @param progress current progress
     * @param max max progress
     */
    protected abstract void updateProgress(int progress, int max);

    /**
     * Get the world node
     *
     * @return world node
     */
    public Node getWorld() {
        return worldNode;
    }

    /**
     * Set some tiles selected/undelected
     *
     * @param selectionArea the selection area
     * @param select select or unselect
     */
    public void selectTiles(SelectionArea selectionArea, boolean select) {
        mapLoader.selectTiles(selectionArea, select);
    }

    /**
     * Determine if a tile at x & y is selected or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selected
     */
    public boolean isSelected(int x, int y) {
        return mapLoader.isSelected(x, y);
    }

    /**
     * Determine if a tile at x & y is selectable or not
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return is the tile selectable
     */
    public boolean isTaggable(int x, int y) {
        return mapLoader.isTaggable(x, y);
    }

    /**
     * Determine if a tile at x & y is buildable by the player
     *
     * @param x x coordinate
     * @param y y coordinate
     * @param player the player
     * @param room the room to be build
     * @return is the tile buildable
     */
    public boolean isBuildable(int x, int y, Player player, Room room) {
        return mapLoader.isBuildable(x, y, player, room);
    }
}
