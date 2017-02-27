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
package toniarts.openkeeper.tools.modelviewer;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.ILoader;

/**
 * Loads up terrain
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TerrainLoader implements ILoader<Terrain> {

    @Override
    public Spatial load(AssetManager assetManager, Terrain object) {

        //Create a root
        Node root = new Node(object.getName());

        if (object.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {

        } else if (object.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {

        } else {
            // Add the top
            if (object.getTopResource() != null) {
                Spatial s = AssetUtils.loadModel(assetManager, object.getTopResource().getName());

                root.attachChild(s);
            }

            if (object.getCompleteResource() != null) {
                Spatial s = AssetUtils.loadModel(assetManager, object.getCompleteResource().getName());

                root.attachChild(s);
            }

            if (object.getTaggedTopResource() != null) {
                Spatial s = AssetUtils.loadModel(assetManager, object.getTaggedTopResource().getName());

                root.attachChild(s);
            }

            // The sides
            if (object.getSideResource() != null) {
                Spatial s = AssetUtils.loadModel(assetManager, object.getSideResource().getName());

                root.attachChild(s);
            }
        }

        return root;
    }
}
