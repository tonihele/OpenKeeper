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
import java.util.List;
import toniarts.openkeeper.tools.convert.KmfModelLoader;

/**
 * Stops the spinning numbers of the My Pet Dungeon level indicator on a level.
 * Its number texture animates through MPD1, two blurs, MPD2, two blurs... up to
 * MPD10; DKII.EXE stops it on the level's number (0x4A3910, 0x4A3A00).
 */
final class LevelIndicatorTextures {

    private LevelIndicatorTextures() {
    }

    static void apply(Spatial model, int level, AssetManager assetManager) {
        String number = "MPD" + level + ".png";
        model.depthFirstTraversal(spatial -> {
            List<String> textures = spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES);
            if (!(spatial instanceof Geometry geometry) || textures == null) {
                return;
            }
            for (String texture : textures) {
                if (texture.substring(texture.lastIndexOf('/') + 1).equalsIgnoreCase(number)) {
                    Material material = geometry.getMaterial();
                    material.setTexture("DiffuseMap", assetManager.loadTexture(new TextureKey(texture, false)));
                    material.setInt("NumberOfTiles", 1);
                    material.setInt("Speed", 0);
                    return;
                }
            }
        });
    }
}
