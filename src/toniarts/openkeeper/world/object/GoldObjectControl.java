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
package toniarts.openkeeper.world.object;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.gui.CursorFactory;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Object;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;

/**
 * Handles gold type objects in the game world
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GoldObjectControl extends ObjectControl {

    private int gold = 0;

    /**
     * Max gold doesn't really tell anything, it is used to display the
     * different stages of gold piles. In rooms it is the max, but the outside
     * rooms the piles can be way over this
     */
    private int maxGold;
    private int currentResourceIndex = 0;
    private final String tooltipLooseGold;
    private final String tooltipGold;

    public GoldObjectControl(TileData tile, Object object, WorldState worldState, int initialGoldAmount, int maxGold) {
        super(tile, object, worldState);

        this.gold = initialGoldAmount;
        this.maxGold = maxGold;

        // Tooltips
        tooltipLooseGold = bundle.getString("2544");
        tooltipGold = bundle.getString("2543");
    }

    @Override
    public String getTooltip(short playerId) {
        return (getState() == ObjectState.STORED_IN_ROOM ? tooltipGold : tooltipLooseGold).replace("%73", Integer.toString(gold));
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
        refreshResource();
    }

    public void setMaxGold(int maxGold) {
        this.maxGold = maxGold;
        refreshResource();
    }

    public int getMaxGold() {
        return maxGold;
    }

    @Override
    protected ArtResource getResource() {
        if (isAdditionalResources()) {

            // For gold it is the amount of gold
            currentResourceIndex = getResourceIndex();
            if (currentResourceIndex >= object.getAdditionalResources().size()) {
                return object.getMeshResource();
            }
            return object.getAdditionalResources().get(currentResourceIndex);
        }
        return object.getMeshResource();
    }

    private int getResourceIndex() {
        return Math.max((int) Math.ceil((gold / (float) maxGold) * 100 / (100f / getResourceCount())) - 1, 0);
    }

    private void refreshResource() {
        if (isAdditionalResources() && currentResourceIndex != getResourceIndex()) {
            Node nodeObject = (Node) AssetUtils.loadModel(worldState.getAssetManager(), AssetsConverter.MODELS_FOLDER + "/" + getResource().getName() + ".j3o", false);
            ((Node) getSpatial()).detachAllChildren();
            for (Spatial spat : nodeObject.getChildren()) {
                ((Node) getSpatial()).attachChild(spat);
            }
        }
    }

    @Override
    public CursorFactory.CursorType getInHandCursor() {
        return CursorFactory.CursorType.HOLD_GOLD;
    }

}
