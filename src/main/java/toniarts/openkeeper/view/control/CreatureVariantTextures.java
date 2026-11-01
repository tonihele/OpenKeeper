/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

/** Applies DKII's optional {@code Unique_} textures to a reused model. */
final class CreatureVariantTextures {

    private static final String UNIQUE_PREFIX = "Unique_";

    private CreatureVariantTextures() {
    }

    static void apply(Spatial model, AssetManager assetManager) {
        model.depthFirstTraversal(spatial -> {
            if (!(spatial instanceof Geometry geometry)) {
                return;
            }
            Material material = geometry.getMaterial();
            if (material.getParam("DiffuseMap") == null) {
                return;
            }
            Texture texture = (Texture) material.getParam("DiffuseMap").getValue();
            if (texture == null || texture.getKey() == null) {
                return;
            }
            String name = texture.getKey().getName();
            int slash = name.lastIndexOf('/');
            if (name.regionMatches(slash + 1, UNIQUE_PREFIX, 0, UNIQUE_PREFIX.length())) {
                return;
            }
            String uniqueName = name.substring(0, slash + 1) + UNIQUE_PREFIX + name.substring(slash + 1);
            TextureKey key = new TextureKey(uniqueName, false);
            if (assetManager.locateAsset(key) != null) {
                Material uniqueMaterial = material.clone();
                uniqueMaterial.setTexture("DiffuseMap", assetManager.loadTexture(key));
                geometry.setMaterial(uniqueMaterial);
            }
        });
    }
}
