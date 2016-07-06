/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.MaterialKey;
import com.jme3.asset.ModelKey;
import com.jme3.asset.TextureKey;
import com.jme3.asset.cache.AssetCache;
import com.jme3.asset.cache.SimpleAssetCache;
import com.jme3.asset.cache.WeakRefAssetCache;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;

/**
 * Collection of asset related common functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AssetUtils {

    private final static AssetCache assetCache = new SimpleAssetCache();
    private final static AssetCache weakAssetCache = new WeakRefAssetCache();
    private final static Map<String, TextureKey> textureKeyMapCache = new HashMap<>();
    private final static Map<TextureKey, Boolean> textureMapCache = new HashMap<>();
    private static final Logger logger = Logger.getLogger(AssetUtils.class.getName());

    private AssetUtils() {
        // Nope
    }

    /**
     * Loads a model. The model is cached on the first call and loaded from
     * cache.
     *
     * @param assetManager the asset manager to use
     * @param resourceName the model name, the model name is checked and fixed
     * @param useWeakCache use weak cache, if not then permanently cache the
     * models. Use weak cache to load some models that are not often needed
     * (water bed etc.)
     * @return a cloned instance from the cache
     */
    public static Spatial loadModel(AssetManager assetManager, String resourceName, boolean useWeakCache) {
        ModelKey assetKey = new ModelKey(ConversionUtils.getCanonicalAssetKey(resourceName));

        // Set the correct asset cache
        final AssetCache cache;
        if (useWeakCache) {
            cache = weakAssetCache;
        } else {
            cache = assetCache;
        }

        // Get the model from cache
        Spatial model = cache.getFromCache(assetKey);
        if (model == null) {
            model = assetManager.loadModel(assetKey);

            // Assign maps
            assignMapsToMaterial(model, assetManager);

            cache.addToCache(assetKey, model);
        }
        return model.clone();
    }

    private static void assignMapsToMaterial(Spatial model, AssetManager assetManager) {
        model.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {
                    Material material = ((Geometry) spatial).getMaterial();
                    assignMapsToMaterial(assetManager, material);
                }
            }

        });
    }

    /**
     * Assign different kind of maps (Specular, Norma, etc.) to material, if
     * found
     *
     * @param assetManager   the asset manager
     * @param material     the material to apply to
     */
    public static void assignMapsToMaterial(AssetManager assetManager, Material material) {
        String diffuseTexture = ((Texture) material.getParam("DiffuseMap").getValue()).getKey().getName(); // Unharmed texture

        assignMapToMaterial(assetManager, material, "NormalMap", getNormalMapName(diffuseTexture));
        assignMapToMaterial(assetManager, material, "SpecularMap", getSpecularMapName(diffuseTexture));
    }

    private static void assignMapToMaterial(AssetManager assetManager, Material material, String paramName, String textureName) {

        // Try to locate the texture
        TextureKey textureKey = textureKeyMapCache.get(textureName);
        boolean found;
        if (textureKey == null) {
            textureKey = new TextureKey(textureName, false);
            textureKeyMapCache.put(textureName, textureKey);

            // See if it exists
            AssetInfo assetInfo = assetManager.locateAsset(textureKey);
            found = (assetInfo != null);
            textureMapCache.put(textureKey, found);
        } else {
            found = textureMapCache.get(textureKey);
        }

        // Set it
        if (found) {
            material.setTexture(paramName, assetManager.loadTexture(textureKey));
        } else {
            material.clearParam(paramName);
        }
    }

    private static String getNormalMapName(String texture) {
        return getCustomTextureMapName(texture, "n");
    }

    private static String getSpecularMapName(String texture) {
        return getCustomTextureMapName(texture, "s");
    }

    private static String getCustomTextureMapName(String texture, String suffix) {
        int extensionIndex = texture.lastIndexOf(".");
        return texture.substring(0, extensionIndex).concat("_").concat(suffix).concat(texture.substring(extensionIndex));
    }

    /**
     * Creates a material from an ArtResource
     *
     * @param resource the ArtResource
     * @param assetManager the asset manager
     * @return JME material
     */
    public static Material createLightningSpriteMaterial(ArtResource resource, AssetManager assetManager) {
        if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE)) {

            // Cache
            MaterialKey assetKey = new MaterialKey(resource.getName());
            Material mat = assetCache.getFromCache(assetKey);

            if (mat == null) {
                mat = new Material(assetManager,
                        "MatDefs/LightingSprite.j3md");
                int frames = ((ArtResource.Image) resource.getSettings()).getFrames();
                mat.setInt("NumberOfTiles", frames);
                mat.setInt("Speed", 8); // Just a guess work

                // Create the texture
                try {

                    if (resource.getSettings().getType() == ArtResource.Type.ALPHA) {
                        mat.setTransparent(true);
                        mat.setFloat("AlphaDiscardThreshold", 0.1f);
                        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                    }

                    Texture tex = createArtResourceTexture(resource, assetManager);

                    // Load the texture up
                    mat.setTexture("DiffuseMap", tex);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Can't create a texture out of " + resource + "!", e);
                }

                // Add to cache
                assetCache.addToCache(assetKey, mat);
            }
            return mat.clone();
        }
        return null;
    }

    /**
     * Create particle type material from ArtResource
     *
     * @param resource the ArtResource
     * @param assetManager the asset manager
     * @return JME material
     */
    public static Material createParticleMaterial(AssetManager assetManager, ArtResource resource) {

        // Cache
        MaterialKey assetKey = new MaterialKey(resource.getName());
        Material mat = assetCache.getFromCache(assetKey);

        if (mat == null) {
            mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
            try {
                mat.setTexture("Texture", createArtResourceTexture(resource, assetManager));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't create a texture out of " + resource + "!", e);
            }

            // Add to cache
            assetCache.addToCache(assetKey, mat);
        }
        return mat.clone();
    }

    private static Texture createArtResourceTexture(ArtResource resource, AssetManager assetManager) throws IOException {

        if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE)) {
            int frames = ((ArtResource.Image) resource.getSettings()).getFrames();
            RescaleOp rop = null;
            if (resource.getSettings().getType() == ArtResource.Type.ALPHA) {
                float[] scales = {1f, 1f, 1f, 0.75f};
                float[] offsets = new float[4];
                rop = new RescaleOp(scales, offsets, null);
            }

            // Get the first frame, the frames need to be same size
            BufferedImage img = ImageIO.read(assetManager.locateAsset(new AssetKey(ConversionUtils.getCanonicalAssetKey("Textures/" + resource.getName() + "0.png"))).openStream());

            // Create image big enough to fit all the frames
            BufferedImage text
                    = new BufferedImage(img.getWidth() * frames, img.getHeight(),
                            BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = text.createGraphics();
            g.drawImage(img, rop, 0, 0);
            for (int x = 1; x < frames; x++) {
                img = ImageIO.read(assetManager.locateAsset(new AssetKey(ConversionUtils.getCanonicalAssetKey("Textures/" + resource.getName() + x + ".png"))).openStream());
                g.drawImage(img, rop, img.getWidth() * x, 0);
            }
            g.dispose();

            // Convert the new image to a texture
            AWTLoader loader = new AWTLoader();
            Texture tex = new Texture2D(loader.load(text, false));
            return tex;
        } else {

            // A regular texture
            TextureKey key = new TextureKey(ConversionUtils.getCanonicalAssetKey("Textures/" + resource.getName() + ".png"), false);
            return assetManager.loadTexture(key);
        }
    }

}
