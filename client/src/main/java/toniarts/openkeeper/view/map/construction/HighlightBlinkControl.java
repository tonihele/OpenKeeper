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
package toniarts.openkeeper.view.map.construction;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Makes a highlighted level blink between its normal texture and the blue
 * highlight (<i>_E</i>) texture for a fixed duration, after which it stays
 * solidly on the highlight texture.
 *
 * @author OpenKeeper
 */
public class HighlightBlinkControl extends AbstractControl {

    private static final Logger logger = Logger.getLogger(HighlightBlinkControl.class.getName());
    private static final float BLINK_DURATION = 8f;
    private static final float BLINK_SPEED = 2f;

    private final Spatial levelSpatial;
    private final AssetManager assetManager;
    private final String baseTexture;
    private final String highlightTexture;
    private float elapsed = 0f;
    private boolean settled = false;
    private String currentTexture = null;

    /**
     * @param levelSpatial the level spatial whose geometries get swapped
     * @param assetManager the asset manager instance
     * @param baseTexture the base (normal) diffuse texture key
     * @param highlightTexture the blue highlight (<i>_E</i>) diffuse texture key
     */
    public HighlightBlinkControl(Spatial levelSpatial, AssetManager assetManager,
                                 String baseTexture, String highlightTexture) {
        this.levelSpatial = levelSpatial;
        this.assetManager = assetManager;
        this.baseTexture = baseTexture;
        this.highlightTexture = highlightTexture;
    }

    @Override
    protected void controlUpdate(float tpf) {
        elapsed += tpf;
        if (elapsed >= BLINK_DURATION) {
            if (!settled) {
                settled = true;
                applyTexture(highlightTexture);
            }
            return;
        }

        // Blink between the normal and highlight texture
        float wave = (FastMath.sin(elapsed * BLINK_SPEED * FastMath.TWO_PI) + 1f) / 2f;
        applyTexture(wave >= 0.5f ? highlightTexture : baseTexture);
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

    private void applyTexture(final String textureKey) {
        if (textureKey.equals(currentTexture)) {
            return;
        }
        currentTexture = textureKey;
        levelSpatial.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (!(spatial instanceof Geometry)) {
                    return;
                }

                Material material = ((Geometry) spatial).getMaterial();
                try {
                    Texture texture = assetManager.loadTexture(new TextureKey(textureKey, false));
                    material.setTexture("DiffuseMap", texture);
                    AssetUtils.assignMapsToMaterial(assetManager, material);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to apply highlight texture " + textureKey + "!", e);
                }
            }
        });
    }
}
