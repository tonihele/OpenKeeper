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
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.loader.ILoader;
import toniarts.openkeeper.view.effect.EffectManagerState;

/**
 * Loads up terrain
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class TerrainsLoader implements ILoader<Terrain> {

    @Override
    public Spatial load(AssetManager assetManager, Terrain object) {

        //Create a root
        Node root = new Node(object.getName());

        if (object.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
            if (object.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                for (int p = 0; p <= 6; p++) {
                    for (int i = 0; i <= 4; i++) {
                        Spatial s = AssetUtils.loadAsset(assetManager,
                                object.getCompleteResource().getName() + p + "_" + i, object.getCompleteResource());
                        s.move(i / 2f, p, i / 2f);

                        root.attachChild(s);
                    }
                }
            } else {
                for (int i = 0; i < 4; i++) {
                        Spatial s = AssetUtils.loadAsset(assetManager,
                                object.getCompleteResource().getName() + i, object.getCompleteResource());

                        root.attachChild(s);
                    }
            }

        } else if (object.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {
            for (int i = 0; i <= 7; i++) {
                Spatial s = AssetUtils.loadAsset(assetManager,
                        object.getCompleteResource().getName() + i, object.getCompleteResource());
                s.move(0, i, 0);

                root.attachChild(s);
            }

        } else if (object.getFlags().contains(Terrain.TerrainFlag.ROOM)) {
            // FIXME

        } else {
            // Add the top
            if (object.getTopResource() != null) {
                Spatial s = AssetUtils.loadAsset(assetManager, object.getTopResource().getName(), object.getTopResource());

                root.attachChild(s);
            }

            if (object.getCompleteResource() != null) {
                Spatial s = AssetUtils.loadAsset(assetManager, object.getCompleteResource().getName(), object.getCompleteResource());

                root.attachChild(s);
            }

            if (object.getTaggedTopResource() != null) {
                Spatial s = AssetUtils.loadAsset(assetManager, object.getTaggedTopResource().getName(), object.getTaggedTopResource());

                root.attachChild(s);
            }

            // The sides
            if (object.getSideResource() != null) {
                Spatial s = AssetUtils.loadAsset(assetManager, object.getSideResource().getName(), object.getSideResource());

                root.attachChild(s);
            }
        }

        return root;
    }

    public Spatial load(AssetManager assetManager, EffectManagerState effectManagerState, Terrain object) {
        Spatial root = load(assetManager, object);

        List<Integer> effects = new ArrayList<>();
        effects.add(object.getDestroyedEffectId());
        effects.add(object.getMaxHealthEffectId());

        float height = 1;
        for (Integer effectId : effects) {
            if (effectId == 0) {
                continue;
            }

            effectManagerState.loadSingleEffect((Node) root, new Vector3f(0, height++, 0), effectId, true);
        }

        return root;
    }
}
