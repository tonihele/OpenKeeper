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
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
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

        toniarts.openkeeper.tools.convert.map.Object obj = kwdFile.getObject(object.getObjectId());

        // Load
        Node nodeObject = (Node) AssetUtils.loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + obj.getMeshResource().getName() + ".j3o", false);
        ObjectControl objectControl = new ObjectControl(object, obj, worldState);
        nodeObject.addControl(objectControl);

        // Move to the center of the tile
        nodeObject.setLocalTranslation(
                object.getPosX() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f,
                0 * MapLoader.TILE_HEIGHT,
                object.getPosY() * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2f);

        return nodeObject;
    }
}
