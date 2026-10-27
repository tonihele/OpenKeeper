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
package toniarts.openkeeper.view;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Lays the charred texture over a model's diffuse textures, used for dead
 * bodies. The material shader has no second texture layer to blend with, so
 * the overlay is baked into a copy of each diffuse texture (once per source
 * texture) and the geometry gets its own material pointing to it, leaving the
 * shared originals untouched.
 */
public final class CharredOverlay {

    private static final Logger logger = Logger.getLogger(CharredOverlay.class.getName());

    private static final String CHARRED_TEXTURE = "CharredTexture";
    private static final String USER_DATA_KEY_CHARRED = "charred";
    private static final String DIFFUSE_MAP = "DiffuseMap";

    // Baked textures by their source, only as long as the source lives
    private static final Map<Texture, Texture> BAKED_TEXTURES = new WeakHashMap<>();

    private CharredOverlay() {
        // Nope
    }

    /**
     * Applies the overlay to all the geometries under the spatial, only once
     * per spatial. Geometries that appear later (i.e. a changed animation)
     * need a new call after clearing the mark with {@link #clear(Spatial)}.
     *
     * @param spatial the spatial, usually a creature
     * @param assetManager asset manager instance
     */
    public static void apply(Spatial spatial, AssetManager assetManager) {
        if (Boolean.TRUE.equals(spatial.getUserData(USER_DATA_KEY_CHARRED))) {
            return;
        }
        spatial.setUserData(USER_DATA_KEY_CHARRED, true);

        TextureKey key = new TextureKey(AssetUtils.getCanonicalAssetKey(
                AssetsConverter.TEXTURES_FOLDER.concat("/").concat(CHARRED_TEXTURE).concat(".png")), false);
        Texture charred = assetManager.loadTexture(key);

        spatial.depthFirstTraversal(sp -> {
            if (sp instanceof Geometry geometry) {
                charr(geometry, charred);
            }
        });
    }

    /**
     * Forgets that the spatial was charred
     *
     * @param spatial the spatial
     */
    public static void clear(Spatial spatial) {
        spatial.setUserData(USER_DATA_KEY_CHARRED, null);
    }

    private static void charr(Geometry geometry, Texture charred) {
        Material material = geometry.getMaterial();
        if (material == null || material.getTextureParam(DIFFUSE_MAP) == null) {
            return;
        }
        Texture diffuse = material.getTextureParam(DIFFUSE_MAP).getTextureValue();
        Texture baked = bake(diffuse, charred);
        if (baked == null) {
            return;
        }
        Material charredMaterial = material.clone();
        charredMaterial.setTexture(DIFFUSE_MAP, baked);
        geometry.setMaterial(charredMaterial);
    }

    private static synchronized Texture bake(Texture diffuse, Texture charred) {
        Texture baked = BAKED_TEXTURES.get(diffuse);
        if (baked == null) {
            try {
                baked = createBakedTexture(diffuse, charred);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Failed to overlay the charred texture, unsupported texture format?", e);
            }
            BAKED_TEXTURES.put(diffuse, baked); // Also remember the failures
        }
        return baked;
    }

    private static Texture createBakedTexture(Texture diffuse, Texture charred) {
        Image source = diffuse.getImage();
        Image overlay = charred.getImage();
        if (source == null || overlay == null || source.getData().isEmpty() || overlay.getData().isEmpty()) {
            return null;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        Image result = new Image(Image.Format.RGBA8, width, height,
                BufferUtils.createByteBuffer(width * height * 4), source.getColorSpace());

        ImageRaster sourceRaster = ImageRaster.create(source);
        ImageRaster overlayRaster = ImageRaster.create(overlay);
        ImageRaster resultRaster = ImageRaster.create(result);
        ColorRGBA pixel = new ColorRGBA();
        for (int y = 0; y < height; y++) {
            int overlayY = y * overlay.getHeight() / height;
            for (int x = 0; x < width; x++) {
                sourceRaster.getPixel(x, y, pixel);
                ColorRGBA overlayPixel = overlayRaster.getPixel(x * overlay.getWidth() / width, overlayY);

                // Plain alpha blend of the overlay on top, the body's own alpha stays
                float a = overlayPixel.a;
                pixel.set(pixel.r * (1 - a) + overlayPixel.r * a,
                        pixel.g * (1 - a) + overlayPixel.g * a,
                        pixel.b * (1 - a) + overlayPixel.b * a,
                        pixel.a);
                resultRaster.setPixel(x, y, pixel);
            }
        }

        Texture2D texture = new Texture2D(result);
        texture.setWrap(Texture.WrapMode.Repeat);
        texture.setMinFilter(diffuse.getMinFilter());
        texture.setMagFilter(diffuse.getMagFilter());
        return texture;
    }
}
