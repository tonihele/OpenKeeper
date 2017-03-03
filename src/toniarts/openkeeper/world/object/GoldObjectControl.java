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
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;
import toniarts.openkeeper.world.creature.CreatureControl;

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
    private ArtResource currentResource = null;
    private final String tooltipLooseGold;
    private final String tooltipGold;

    public GoldObjectControl(TileData tile, GameObject object, WorldState worldState, int initialGoldAmount, int maxGold) {
        super(tile, object, worldState);

        this.gold = initialGoldAmount;
        this.maxGold = maxGold;

        // Tooltips
        tooltipLooseGold = bundle.getString("2544");
        tooltipGold = bundle.getString("2543");
    }

    @Override
    public String getTooltip(short playerId) {
        return (getState() == ObjectState.STORED_IN_ROOM ? tooltipGold : tooltipLooseGold)
                .replace("%73", Integer.toString(gold));
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
        ArtResource result = object.getMeshResource();

        if (gold != maxGold && isAdditionalResources()) {
            float delta = (float) maxGold / object.getAdditionalResources().size();
            int index = (int) (gold / delta);
            result = object.getAdditionalResources().get(index);
        }

        return result;
    }

    private void refreshResource() {
        ArtResource temp = getResource();
        if (temp != currentResource) {
            currentResource = temp;
            Node nodeObject = (Node) AssetUtils.loadModel(worldState.getAssetManager(), currentResource.getName());
            nodeObject.move(0, MapLoader.FLOOR_HEIGHT, 0);
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

    @Override
    public void creaturePicksUp(CreatureControl creature) {
        super.creaturePicksUp(creature);

        // Gold just melts
        removeObject();
    }

}
