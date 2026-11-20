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
package toniarts.openkeeper.view.map;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.control.LiquidSurfaceControl;

/** Builds the shared map and Temple water/lava surface path. */
public final class Water {

    private static final int NATIVE_WAVE_SUBDIVISIONS = 2;

    private Water() {
    }

    /** Construct water/lava without a terrain deformer. */
    public static Spatial construct(AssetManager assetManager, List<EntityInstance<Terrain>> entityInstances) {
        return construct(assetManager, entityInstances, null, true);
    }

    /**
     * Construct water/lava and bend its shore vertices with the terrain.
     *
     * @param assetManager asset manager instance
     * @param entityInstances list of instances of one liquid type
     * @param deformer deforms the horizontal shore position, or {@code null}
     * @return the visual representation
     */
    public static Spatial construct(AssetManager assetManager, List<EntityInstance<Terrain>> entityInstances,
            TerrainDeformer deformer) {
        return construct(assetManager, entityInstances, deformer, true);
    }

    /** Construct water/lava with the enhanced mode explicitly enabled or disabled. */
    public static Spatial construct(AssetManager assetManager, List<EntityInstance<Terrain>> entityInstances,
            boolean allowEnhanced) {
        return construct(assetManager, entityInstances, null, allowEnhanced);
    }

    /**
     * Construct water/lava with optional terrain deformation and enhanced rendering.
     * Temple pools pass {@code false} to preserve the original Classic pool surface.
     */
    public static Spatial construct(AssetManager assetManager, List<EntityInstance<Terrain>> entityInstances,
            TerrainDeformer deformer, boolean allowEnhanced) {
        if (entityInstances.isEmpty()) {
            throw new IllegalArgumentException("A liquid surface needs at least one terrain instance.");
        }
        Terrain terrain = entityInstances.get(0).getEntity();
        boolean water = terrain.getFlags().contains(Terrain.TerrainFlag.WATER);
        Mesh mesh = createMesh(entityInstances, !water, deformer);
        Mesh enhancedWaterMesh = water && allowEnhanced
                ? createMesh(entityInstances, true, deformer) : null;

        Geometry geometry = new Geometry(water ? "Water" : "Lava", mesh);
        geometry.setBatchHint(Spatial.BatchHint.Never);

        Material original;
        if (water) {
            ArtResource resource = terrain.getTopResource();
            original = AssetUtils.createLightningSpriteMaterial(resource, assetManager);
        } else {
            original = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            String texture = AssetUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                    + "/" + terrain.getTopResource().getName() + ".png");
            original.setTexture("DiffuseMap", assetManager.loadTexture(new TextureKey(texture, false)));
        }
        MapViewController.setTerrainMaterialLighting(original, terrain);
        Material material = AssetUtils.createDkSurfaceMaterial(assetManager, original);
        material.setFloat("LiquidWaveLevel", WorldUtils.WATER_LEVEL);
        material.setFloat("LiquidWaveFrequency", LiquidWaves.frequency(water));
        if (!water) {
            material.setBoolean("LiquidWavesEnabled", true);
        }
        geometry.setMaterial(material);
        geometry.addControl(new LiquidSurfaceControl(water, allowEnhanced, assetManager, enhancedWaterMesh));

        if (material.isTransparent()) {
            geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
            geometry.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        } else {
            geometry.setShadowMode(RenderQueue.ShadowMode.Receive);
        }
        return geometry;
    }

    /**
     * Build one quad per Classic water tile and a 3-by-3 sample grid for any
     * liquid surface that waves. The map coordinates in TexCoord2 keep phase
     * continuous when duplicated tile-edge vertices are displaced on the GPU.
     */
    static Mesh createMesh(List<EntityInstance<Terrain>> entityInstances, boolean animatedWaves,
            TerrainDeformer deformer) {
        Mesh mesh = new Mesh();
        int subdivisions = animatedWaves ? NATIVE_WAVE_SUBDIVISIONS : 1;
        int verticesPerTile = (subdivisions + 1) * (subdivisions + 1);
        int tiles = entityInstances.stream().mapToInt(instance -> instance.getCoordinates().size()).sum();
        ArrayList<Vector3f> vertices = new ArrayList<>(tiles * verticesPerTile);
        ArrayList<Vector2f> textureCoordinates = new ArrayList<>(tiles * verticesPerTile);
        ArrayList<Vector2f> waveCoordinates = animatedWaves ? new ArrayList<>(tiles * verticesPerTile) : null;
        ArrayList<Vector3f> normals = new ArrayList<>(tiles * verticesPerTile);
        ArrayList<Integer> indexes = new ArrayList<>(tiles * subdivisions * subdivisions * 6);

        for (EntityInstance<Terrain> instance : entityInstances) {
            for (Point tile : instance.getCoordinates()) {
                int base = vertices.size();
                for (int z = 0; z <= subdivisions; z++) {
                    float v = z / (float) subdivisions;
                    for (int x = 0; x <= subdivisions; x++) {
                        float u = x / (float) subdivisions;
                        float worldX = (tile.x - 0.5f + u) * WorldUtils.TILE_WIDTH;
                        float worldZ = (tile.y - 0.5f + v) * WorldUtils.TILE_WIDTH;
                        vertices.add(new Vector3f(worldX, WorldUtils.WATER_LEVEL, worldZ));
                        textureCoordinates.add(new Vector2f(u, v));
                        normals.add(Vector3f.UNIT_Y.clone());
                        if (waveCoordinates != null) {
                            // DKII samples the wave field on tile-corner coordinates.
                            waveCoordinates.add(new Vector2f(tile.x + u, tile.y + v));
                        }
                    }
                }
                for (int z = 0; z < subdivisions; z++) {
                    for (int x = 0; x < subdivisions; x++) {
                        int a = base + z * (subdivisions + 1) + x;
                        int b = a + 1;
                        int c = a + subdivisions + 1;
                        int d = c + 1;
                        indexes.add(c);
                        indexes.add(d);
                        indexes.add(b);
                        indexes.add(b);
                        indexes.add(a);
                        indexes.add(c);
                    }
                }
            }
        }

        // Keep the liquid edge horizontally attached to the deformed bank.
        // Its y remains at water level; the shader applies waves after this.
        if (deformer != null) {
            Vector3f bank = new Vector3f();
            for (Vector3f vertex : vertices) {
                bank.set(vertex.x, WorldUtils.FLOOR_HEIGHT, vertex.z);
                deformer.deform(bank, bank, null);
                vertex.x = bank.x;
                vertex.z = bank.z;
            }
        }

        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(Vector3f[]::new)));
        mesh.setBuffer(Type.TexCoord, 2,
                BufferUtils.createFloatBuffer(textureCoordinates.toArray(Vector2f[]::new)));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indexes.stream().mapToInt(Integer::intValue).toArray()));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(Vector3f[]::new)));
        if (waveCoordinates != null) {
            mesh.setBuffer(Type.TexCoord2, 2, BufferUtils.createFloatBuffer(waveCoordinates.toArray(Vector2f[]::new)));
        }
        mesh.updateBound();
        if (animatedWaves && mesh.getBound() instanceof BoundingBox bound) {
            mesh.setBound(new BoundingBox(bound.getCenter().clone(), bound.getXExtent(),
                    bound.getYExtent() + LiquidWaves.AMPLITUDE * 2f, bound.getZExtent()));
        }
        return mesh;
    }
}
