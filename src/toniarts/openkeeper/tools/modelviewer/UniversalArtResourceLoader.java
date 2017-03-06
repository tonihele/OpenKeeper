/*
 * Copyright (C) 2014-2017 OpenKeeper
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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.effect.EffectGeometry;

/**
 *
 * @author archdemon
 */
public class UniversalArtResourceLoader {

    public static Spatial load(final AssetManager assetManager, ArtResource resource) {

        if (resource == null) {
            return null;
        }

        Spatial result = null;

        switch (resource.getType()) {
            case MESH:
            case ANIMATING_MESH:
                result = AssetUtils.loadAsset(assetManager, resource.getName());
                break;

            case PROCEDURAL_MESH:
                result = AssetUtils.createProceduralMesh(resource);
                break;

            case ALPHA:
            case ADDITIVE_ALPHA:
            case SPRITE:
                result = new EffectGeometry("effect");
                ((EffectGeometry) result).setFrames(Math.max(1, resource.getData("frames")));

                Material material = AssetUtils.createParticleMaterial(resource, assetManager);
                result.setMaterial(material);
                break;

            default:
                System.err.println("Not supported effect type" + resource.getType());
        }

        return result;
    }
}
