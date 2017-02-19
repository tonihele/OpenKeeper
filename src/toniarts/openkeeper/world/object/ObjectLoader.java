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
package toniarts.openkeeper.world.object;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import toniarts.openkeeper.game.player.PlayerSpell;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Object;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.TileData;
import toniarts.openkeeper.world.WorldState;

/**
 * Loads up object
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectLoader implements ILoader<Thing.Object> {

    private final KwdFile kwdFile;
    private final WorldState worldState;
    private static final Logger logger = Logger.getLogger(ObjectLoader.class.getName());

    public ObjectLoader(KwdFile kwdFile, WorldState worldState) {
        this.kwdFile = kwdFile;
        this.worldState = worldState;
    }

    @Override
    public Spatial load(AssetManager assetManager, Thing.Object object) {
        return load(assetManager, worldState.getMapData().getTile(object.getPosX(), object.getPosY()), object.getPosX() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f, object.getPosY() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f, object.getKeeperSpellId(), object.getMoneyAmount(), object.getTriggerId(), object.getObjectId(), object.getPlayerId(), (int) worldState.getGameState().getLevelVariable(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY));
    }

    public Spatial load(AssetManager assetManager, int posX, int posY, short objectId, short playerId) {
        return load(assetManager, worldState.getMapData().getTile(posX, posY), posX * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f, posY * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f, 0, 0, 0, objectId, playerId, 0);
    }

    public Spatial load(AssetManager assetManager, int posX, int posY, int keeperSpellId, int moneyAmount, int triggerId, short objectId, short playerId, int maxMoney) {
        return load(assetManager, worldState.getMapData().getTile(posX, posY), posX * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f, posY * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f, keeperSpellId, moneyAmount, triggerId, objectId, playerId, maxMoney);
    }

    public Spatial load(AssetManager assetManager, TileData tile, float posX, float posY, int keeperSpellId, int moneyAmount, int triggerId, short objectId, short playerId, int maxMoney) {
        PlayerSpell playerSpell = null;
        if (keeperSpellId > 0) {
            KeeperSpell keeperSpell = kwdFile.getKeeperSpellById(keeperSpellId);

            // Create a wrapper for it
            playerSpell = new PlayerSpell(keeperSpell, true);
        }
        return load(assetManager, tile, posX, posY, playerSpell, moneyAmount, triggerId, objectId, playerId, maxMoney);
    }

    public Spatial load(AssetManager assetManager, TileData tile, float posX, float posY, PlayerSpell playerSpell, int moneyAmount, int triggerId, short objectId, short playerId, int maxMoney) {
        toniarts.openkeeper.tools.convert.map.Object obj = kwdFile.getObject(objectId);

        // Load
        ObjectControl objectControl = getControl(tile, obj, moneyAmount, maxMoney, playerSpell);
        Node nodeObject = (Node) AssetUtils.loadModel(assetManager, objectControl.getResource().getName(), false);
        nodeObject.addControl(objectControl);

        // Move to the center of the tile
        AssetUtils.resetSpatial(nodeObject);
        nodeObject.setLocalTranslation(
                posX,
                0 * MapLoader.TILE_HEIGHT,
                posY);

        // Orientation
        nodeObject.setLocalRotation(nodeObject.getLocalRotation().fromAngles(0, -objectControl.getOrientation(), 0));

        return nodeObject;
    }

    private ObjectControl getControl(TileData tile, Object obj, int moneyAmount, int maxMoney, PlayerSpell playerSpell) {
        if (obj.getFlags().contains(Object.ObjectFlag.OBJECT_TYPE_GOLD)) {
            return new GoldObjectControl(tile, obj, worldState, moneyAmount, maxMoney);
        } else if (obj.getFlags().contains(Object.ObjectFlag.OBJECT_TYPE_SPELL_BOOK)) {
            return new SpellBookObjectControl(tile, obj, worldState, playerSpell);
        }
        return new ObjectControl(tile, obj, worldState);
    }
}
