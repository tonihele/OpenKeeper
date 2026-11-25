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
package toniarts.openkeeper.utils;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Highlighting a creature must skip the materials that have nothing to tint,
 * like particle effects
 */
class AssetUtilsHighlightTest {

    @Test
    void particleEffectIsLeftAloneWithoutWarnings() {
        DesktopAssetManager assetManager = new DesktopAssetManager(true);
        Material particle = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        Node model = model(particle);

        boolean[] warned = {false};
        Logger logger = Logger.getLogger(AssetUtils.class.getName());
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                warned[0] = true;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            AssetUtils.setModelHighlight(model, ColorRGBA.Red, true);
        } finally {
            logger.removeHandler(handler);
        }

        assertFalse(warned[0]);
        assertNull(particle.getParam("Color"));
    }

    @Test
    void unshadedMaterialStillGetsTheColor() {
        DesktopAssetManager assetManager = new DesktopAssetManager(true);
        Material unshaded = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Node model = model(unshaded);

        AssetUtils.setModelHighlight(model, ColorRGBA.Red, true);

        assertEquals(ColorRGBA.Red, unshaded.getParamValue("Color"));
    }

    /**
     * The creature structure: root, a node and the geometry in it
     */
    private static Node model(Material material) {
        Geometry geometry = new Geometry("geometry", new Quad(1, 1));
        geometry.setMaterial(material);
        Node inner = new Node();
        inner.attachChild(geometry);
        Node root = new Node();
        root.attachChild(inner);
        return root;
    }
}
