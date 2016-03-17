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
package toniarts.openkeeper.world.terrain;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.util.BufferUtils;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.world.EntityInstance;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.WaterConstructor;

/**
 * Don't let the name fool you, this bad boy also handles lava construction. The
 * name just comes from the construction type
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Water {

    /**
     * TODO: Options loaded from elsewhere
     */
    public enum WaterType {

        /* Just for testing etc. Not to be really used, I just left it in */
        SIMPLE, CLASSIC;
    };
    private static final WaterType WATER_TYPE = WaterType.CLASSIC;
    private static final Logger logger = Logger.getLogger(Water.class.getName());

    private Water() {
        // Nope
    }

    /**
     * Construct water/lava
     *
     * @param assetManager asset manager instance
     * @param entityInstances list of entity instances <b>of the same type</b>.
     * Don't mix lava & water here
     * @return the visual representation of your entity instance
     */
    public static Spatial construct(AssetManager assetManager, List<EntityInstance<Terrain>> entityInstances) {
        boolean water = entityInstances.get(0).getEntity().getFlags().contains(Terrain.TerrainFlag.WATER);
        Mesh mesh = createMesh(entityInstances, (water && WATER_TYPE == WaterType.SIMPLE));

        // Create the geometry
        Geometry geo = new Geometry((water ? "Water" : "Lava"), mesh);

        // The material
        Material mat = null;
        if (water) {
            switch (WATER_TYPE) {
                case SIMPLE: {
                    mat = new Material(assetManager,
                            "Common/MatDefs/Misc/Unshaded.j3md");
                    mat.setColor("Color", new ColorRGBA(0, 0, 1, 0.2f));
                    mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                    mat.setTransparent(true);
                    break;
                }
                case CLASSIC: {

                    // TODO: MaterialHelper to convert package? Something
                    ArtResource resource = entityInstances.get(0).getEntity().getTopResource();

                    // Alpha resource, compile such
                    mat = createMaterial(resource, assetManager);
                    MapLoader.setTerrainMaterialLighting(mat, entityInstances.get(0).getEntity());
                    break;
                }
            }
        } else {

            // Lava
            mat = new Material(assetManager,
                    "Common/MatDefs/Light/Lighting.j3md");
            TextureKey textureKey = new TextureKey(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat("/").concat(entityInstances.get(0).getEntity().getTopResource().getName()).concat(".png")), false);
            Texture tex = assetManager.loadTexture(textureKey);
            mat.setTexture("DiffuseMap", tex);
            MapLoader.setTerrainMaterialLighting(mat, entityInstances.get(0).getEntity());
        }

        // Set it all
        geo.setMaterial(mat);
        if (mat.isTransparent()) {
            geo.setQueueBucket(RenderQueue.Bucket.Transparent);
            geo.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        } else {
            geo.setShadowMode(RenderQueue.ShadowMode.Receive);
        }

        return geo;
    }

    /**
     * Create the whole mesh
     *
     * @param entityInstances the instances
     * @param shareVertices no duplicate vertices, if no texture coordinates are
     * needed
     * @return returns a mesh
     */
    private static Mesh createMesh(List<EntityInstance<Terrain>> entityInstances, boolean shareVertices) {

        // Create new mesh
        Mesh mesh = new Mesh();
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector2f> textureCoordinates = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();

        // Handle each river/lake separately
        for (EntityInstance<Terrain> entityInstance : entityInstances) {
            HashMap<Vector3f, Integer> verticeHash = new HashMap<>(entityInstance.getCoordinates().size());
            for (Point tile : entityInstance.getCoordinates()) {

                // For each tile, create a quad, in a way
                // Texture coordinates
                Vector2f textureCoord1 = new Vector2f(0, 0);
                Vector2f textureCoord2 = new Vector2f(1, 0);
                Vector2f textureCoord3 = new Vector2f(0, 1);
                Vector2f textureCoord4 = new Vector2f(1, 1);

                // Vertices
                Vector3f vertice1 = new Vector3f(tile.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH, -WaterConstructor.WATER_LEVEL, tile.y * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH);
                Vector3f vertice2 = new Vector3f(tile.x * MapLoader.TILE_WIDTH, -WaterConstructor.WATER_LEVEL, tile.y * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH);
                Vector3f vertice3 = new Vector3f(tile.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH, -WaterConstructor.WATER_LEVEL, tile.y * MapLoader.TILE_WIDTH);
                Vector3f vertice4 = new Vector3f(tile.x * MapLoader.TILE_WIDTH, -WaterConstructor.WATER_LEVEL, tile.y * MapLoader.TILE_WIDTH);
                int vertice1Index = addVertice(verticeHash, vertice1, vertices, textureCoord1, textureCoordinates, normals, shareVertices);
                int vertice2Index = addVertice(verticeHash, vertice2, vertices, textureCoord2, textureCoordinates, normals, shareVertices);
                int vertice3Index = addVertice(verticeHash, vertice3, vertices, textureCoord3, textureCoordinates, normals, shareVertices);
                int vertice4Index = addVertice(verticeHash, vertice4, vertices, textureCoord4, textureCoordinates, normals, shareVertices);

                // Indexes
                indexes.addAll(Arrays.asList(vertice3Index, vertice4Index, vertice2Index, vertice2Index, vertice1Index, vertice3Index));
            }
        }
        // Assign the mesh data
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(new Vector3f[vertices.size()])));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(textureCoordinates.toArray(new Vector2f[textureCoordinates.size()])));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(toIntArray(indexes)));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(new Vector3f[normals.size()])));
        mesh.updateBound();
        return mesh;
    }

    /**
     * Adds a vertice to vertice list or returns the index of an existing
     * verctice in the same coordinates. No duplicate vertices
     *
     * @param verticeHash the vertice hash (vertice -> index)
     * @param vertice vertice to add/find
     * @param vertices all the vertices
     * @param textureCoord the texture coordinate at the given vertice
     * @param textureCoordinates the texture coordinates list
     * @param normals the normals
     * @param shareVertices no duplicate vertices, if no texture coordinates are
     * needed
     * @return index of the given vertice
     */
    private static int addVertice(final HashMap<Vector3f, Integer> verticeHash, final Vector3f vertice, final List<Vector3f> vertices, final Vector2f textureCoord, final List<Vector2f> textureCoordinates, final List<Vector3f> normals, final boolean shareVertices) {
        if (!shareVertices || (shareVertices && !verticeHash.containsKey(vertice))) {
            vertices.add(vertice);
            verticeHash.put(vertice, vertices.size() - 1);

            // Add the texture coordinate as well
            textureCoordinates.add(textureCoord);

            // And the normal, they are all facing upwards now
            normals.add(new Vector3f(0, 1, 0));

            return vertices.size() - 1;
        }
        return verticeHash.get(vertice);
    }

    /**
     * TODO: general, refactor out of here. And cache the created materials.<br>
     * Creates a material from an ArtResource
     *
     * @param resource the ArtResource
     * @param assetManager the asset manager
     * @return JME material
     */
    private static Material createMaterial(ArtResource resource, AssetManager assetManager) {
        if (resource.getSettings().getFlags().contains(ArtResource.ArtResourceFlag.ANIMATING_TEXTURE)) {
            Material mat = new Material(assetManager,
                    "MatDefs/LightingSprite.j3md");
            int frames = ((ArtResource.Image) resource.getSettings()).getFrames();
            mat.setInt("NumberOfTiles", frames);
            mat.setInt("Speed", 8); // Just a guess work

            // Create the texture
            try {

                RescaleOp rop = null;
                if (resource.getSettings().getType() == ArtResource.Type.ALPHA) {
                    float[] scales = {1f, 1f, 1f, 0.75f};
                    float[] offsets = new float[4];
                    rop = new RescaleOp(scales, offsets, null);
                    mat.setTransparent(true);
                    mat.setFloat("AlphaDiscardThreshold", 0.1f);
                    mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                }

                // Get the first frame, the frames need to be same size
                BufferedImage img = ImageIO.read(assetManager.locateAsset(new AssetKey("Textures/" + resource.getName() + "0.png")).openStream());

                // Create image big enough to fit all the frames
                BufferedImage text
                        = new BufferedImage(img.getWidth() * frames, img.getHeight(),
                                BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = text.createGraphics();
                g.drawImage(img, rop, 0, 0);
                for (int x = 1; x < frames; x++) {
                    img = ImageIO.read(assetManager.locateAsset(new AssetKey("Textures/" + resource.getName() + x + ".png")).openStream());
                    g.drawImage(img, rop, img.getWidth() * x, 0);
                }
                g.dispose();

                // Convert the new image to a texture
                AWTLoader loader = new AWTLoader();
                Texture tex = new Texture2D(loader.load(text, false));

                // Load the texture up
                mat.setTexture("DiffuseMap", tex);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Can't create a texture out of " + resource + "!", e);
            }

            return mat;
        }
        return null;
    }

    private static int[] toIntArray(final List<Integer> list) {
        int[] ret = new int[list.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = list.get(i);
        }
        return ret;
    }
}
