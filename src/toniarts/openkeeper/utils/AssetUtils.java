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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * Collection of asset related common functions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AssetUtils {

    private static volatile boolean preWarmedAssets = false;
    private final static Object assetLock = new Object();
    private final static AssetCache assetCache = new SimpleAssetCache();
    private final static AssetCache weakAssetCache = new WeakRefAssetCache();
    private final static Map<String, Boolean> textureMapCache = new HashMap<>();
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
     * @param assetManager the asset manager
     * @param material the material to apply to
     */
    public static void assignMapsToMaterial(AssetManager assetManager, Material material) {
        String diffuseTexture = ((Texture) material.getParam("DiffuseMap").getValue()).getKey().getName(); // Unharmed texture

        assignMapToMaterial(assetManager, material, "NormalMap", getNormalMapName(diffuseTexture));
        assignMapToMaterial(assetManager, material, "SpecularMap", getSpecularMapName(diffuseTexture));
    }

    private static void assignMapToMaterial(AssetManager assetManager, Material material, String paramName, String textureName) {

        // Try to locate the texture
        Boolean found = textureMapCache.get(textureName);
        if (found == null) {
            TextureKey textureKey = new TextureKey(textureName, false);

            // See if it exists
            AssetInfo assetInfo = assetManager.locateAsset(textureKey);
            found = (assetInfo != null);
            textureMapCache.put(textureName, found);
        }

        // Set it
        if (found) {
            TextureKey textureKey = new TextureKey(textureName, false);
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

    /**
     * Preloads all assets, to memory and to the GPU. May take some time, but
     * everything works smoothly after. Enqueues the actual GPU loading to the
     * main render loop.
     *
     * @param kwdFile the KWD file to scan for the loadable assets
     * @param assetManager the asset manager
     * @param app the app
     */
    public static void prewarmAssets(KwdFile kwdFile, AssetManager assetManager, Main app) {
        if (!preWarmedAssets) {
            synchronized (assetLock) {
                if (!preWarmedAssets) {
                    try {

                        // Objects
                        prewarmArtResources(new ArrayList<>(kwdFile.getObjectList()), assetManager, app);

                        // Creatures
                        prewarmArtResources(new ArrayList<>(kwdFile.getCreatureList()), assetManager, app);

                        // Doors
                        prewarmArtResources(kwdFile.getDoors(), assetManager, app);

                        // Traps
                        prewarmArtResources(kwdFile.getTraps(), assetManager, app);

                        // Terrain
                        prewarmArtResources(new ArrayList<>(kwdFile.getTerrainList()), assetManager, app);

                        // Rooms
                        prewarmArtResources(kwdFile.getRooms(), assetManager, app);
                    } catch (Exception e) {
                        Logger.getLogger(AssetUtils.class.getName()).log(Level.SEVERE, "Failed to prewarm assets!", e);
                    } finally {
                        preWarmedAssets = true;
                    }
                }
            }
        }
    }

    private static void prewarmArtResources(List<?> objects, AssetManager assetManager, Main app) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, Exception {

        // Get the fields that house a possible ArtResource
        Class clazz = objects.get(0).getClass();
        Method[] methods = clazz.getMethods();
        List<Method> methodsToScan = new ArrayList<>();
        for (Method method : methods) {
            if (method.getReturnType().equals(ArtResource.class)) {
                methodsToScan.add(method);
            }
        }

        // Scan every object
        List<Spatial> models = new ArrayList<>(methodsToScan.size() * objects.size());
        if (!methodsToScan.isEmpty()) {
            for (Object obj : objects) {
                for (Method method : methodsToScan) {
                    ArtResource artResource = (ArtResource) method.invoke(obj, (Object[]) null);
                    if (artResource != null && artResource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.PRELOAD)) {

                        try {

                            // TODO: if possible, we should have here a general loadAsset(ArtResource) stuff
                            if (artResource.getSettings().getType() == ArtResource.Type.MESH || artResource.getSettings().getType() == ArtResource.Type.ANIMATING_MESH || artResource.getSettings().getType() == ArtResource.Type.MESH_COLLECTION || artResource.getSettings().getType() == ArtResource.Type.PROCEDURAL_MESH) {
                                models.add(loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + artResource.getName() + ".j3o", false));
                            } else if (artResource.getSettings().getType() == ArtResource.Type.TERRAIN_MESH && obj instanceof Terrain) {

                                // With terrains, we need to see the contruction type
                                Terrain terrain = (Terrain) obj;
                                if (method.getName().startsWith("getTaggedTopResource") || method.getName().startsWith("getSideResource")) {
                                    models.add(loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + artResource.getName() + ".j3o", false));
                                } else if (terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_QUAD)) {
                                    for (int i = 0; i < 5; i++) {
                                        if (terrain.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                                            for (int y = 0; y < 7; y++) {
                                                models.add(loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + artResource.getName() + y + "_" + i + ".j3o", false));
                                            }
                                        } else {
                                            models.add(loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + artResource.getName() + i + ".j3o", false));
                                        }
                                    }
                                } // TODO: No water... it is done in Water.java, need to tweak somehow
                                else if (!terrain.getFlags().contains(Terrain.TerrainFlag.CONSTRUCTION_TYPE_WATER)) {
                                    models.add(loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + artResource.getName() + ".j3o", false));
                                }
                            } else if (artResource.getSettings().getType() == ArtResource.Type.TERRAIN_MESH && obj instanceof Room) {

                                // With terrains, we need to see the contruction type
                                Room room = (Room) obj;
                                int count = 0;
                                int start = 0;
                                switch (room.getTileConstruction()) {
                                    case NORMAL: {
                                        count = 10;
                                        break;
                                    }
                                    case QUAD: {
                                        count = 4;
                                        break;
                                    }
                                    case DOUBLE_QUAD: {
                                        count = 15; // Hmm not perfect, see Prison
                                        break;
                                    }
                                    case _3_BY_3_ROTATED:
                                    case _3_BY_3: {
                                        count = 9;
                                        break;
                                    }
                                    case HERO_GATE_2_BY_2:
                                    case _5_BY_5_ROTATED: {
                                        count = 4;
                                        break;
                                    }
                                    case HERO_GATE_FRONT_END: {
                                        count = 17;
                                        start = 1;
                                        break;
                                    }
                                }
                                for (int i = start; i < count; i++) {
                                    models.add(loadModel(assetManager, AssetsConverter.MODELS_FOLDER + "/" + artResource.getName() + i + ".j3o", false));
                                }
                            }

                        } catch (Exception e) {
                            throw new Exception("Failed to load " + artResource + " on object " + obj + "!", e);
                        }
                    }
                }
            }
        }

        // Enque the warming up, we need GL context
        if (!models.isEmpty()) {
            logger.log(Level.INFO, "Prewarming {0} objects!", models.size());
            app.enqueue(() -> {

                for (Spatial spatial : models) {
                    app.getRenderManager().preloadScene(spatial);
                }

                return null;
            });
        }
    }

}
