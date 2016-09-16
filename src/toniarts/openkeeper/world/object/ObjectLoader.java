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
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Object;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.ILoader;
import toniarts.openkeeper.world.MapLoader;
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
        return load(assetManager, object.getPosX(), object.getPosY(), object.getKeeperSpellId(), object.getMoneyAmount(), object.getTriggerId(), object.getObjectId(), object.getPlayerId(), (int) worldState.getGameState().getLevelVariable(Variable.MiscVariable.MiscType.MAX_GOLD_PILE_OUTSIDE_TREASURY));
    }

    public Spatial load(AssetManager assetManager, int posX, int posY, short objectId, short playerId) {
        return load(assetManager, posX, posY, 0, 0, 0, objectId, playerId, 0);
    }

    public Spatial load(AssetManager assetManager, int posX, int posY, int keeperSpellId, int moneyAmount, int triggerId, short objectId, short playerId, int maxMoney) {
        toniarts.openkeeper.tools.convert.map.Object obj = kwdFile.getObject(objectId);
        KeeperSpell keeperSpell = null;
        if (keeperSpellId > 0) {
            keeperSpell = kwdFile.getKeeperSpellById(keeperSpellId);
        }

        // Load
        ObjectControl objectControl = getControl(playerId, obj, moneyAmount, maxMoney);
        Node nodeObject = (Node) AssetUtils.loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + objectControl.getResource().getName() + ".j3o", false);
        nodeObject.addControl(objectControl);

        // Move to the center of the tile
        nodeObject.setLocalTranslation(
                posX * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                0 * MapLoader.TILE_HEIGHT,
                posY * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        // Orientation
        nodeObject.setLocalRotation(nodeObject.getLocalRotation().fromAngles(0, -objectControl.getOrientation(), 0));

        return nodeObject;
    }

    private ObjectControl getControl(short playerId, Object obj, int moneyAmount, int maxMoney) {
        if (obj.getFlags().contains(Object.ObjectFlag.OBJECT_TYPE_GOLD)) {
            return new GoldObjectControl(playerId, obj, worldState, moneyAmount, maxMoney);
        }
        return new ObjectControl(playerId, obj, worldState);
    }
}
