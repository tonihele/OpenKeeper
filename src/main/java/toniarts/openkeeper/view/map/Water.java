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
package toniarts.openkeeper.view.map;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.control.LiquidSurfaceControl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import toniarts.openkeeper.common.EntityInstance;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Don't let the name fool you, this bad boy also handles lava construction. The
 * name just comes from the construction type
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Water {

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
        return construct(assetManager, entityInstances, true);
    }

    /**
     * Constructs a liquid surface, optionally keeping it Classic regardless of graphics settings.
     * Temple pools use Classic water to conceal their floor and submerged sacrifices, as in DKII.
     *
     * @param allowEnhanced whether the graphics setting may enable Enhanced rendering
     */
    public static Spatial construct(AssetManager assetManager, List<EntityInstance<Terrain>> entityInstances,
            boolean allowEnhanced) {
        boolean water = entityInstances.get(0).getEntity().getFlags().contains(Terrain.TerrainFlag.WATER);
        Mesh mesh = createMesh(entityInstances);

        // Create the geometry
        Geometry geo = new Geometry((water ? "Water" : "Lava"), mesh);


        // The material
        Terrain terrain = entityInstances.get(0).getEntity();
        Material mat;
        if (water) {
            mat = AssetUtils.createLightningSpriteMaterial(terrain.getTopResource(), assetManager);
        } else {
            mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            TextureKey key = new TextureKey(AssetUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER
                    + "/" + terrain.getTopResource().getName() + ".png"), false);
            mat.setTexture("DiffuseMap", assetManager.loadTexture(key));
        }
        MapViewController.setTerrainMaterialLighting(mat, terrain);
        mat = AssetUtils.createDkSurfaceMaterial(assetManager, mat);

        // Set it all
        geo.setMaterial(mat);
        geo.setBatchHint(Spatial.BatchHint.Never); // Wave vertices change every frame, including Temple pools.
        mat.setFloat("EnhancedOpacity", water ? .75f : 1f);
        geo.addControl(allowEnhanced ? new LiquidSurfaceControl(water, assetManager)
                : new LiquidSurfaceControl(water, assetManager, () -> false));
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
     * @return returns a mesh
     */
    static Mesh createMesh(List<EntityInstance<Terrain>> entityInstances) {
        List<Vector3f> vertices = new ArrayList<>();
        List<Vector2f> uv = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (EntityInstance<Terrain> instance : entityInstances) for (Point tile : instance.getCoordinates()) {
            int base = vertices.size();
            for (int z = 0; z <= 2; z++) for (int x = 0; x <= 2; x++) {
                vertices.add(new Vector3f((tile.x - .5f + x * .5f) * WorldUtils.TILE_WIDTH,
                        LiquidWaves.LEVEL, (tile.y - .5f + z * .5f) * WorldUtils.TILE_WIDTH));
                uv.add(new Vector2f(x * .5f, z * .5f));
                normals.add(Vector3f.UNIT_Y.clone());
            }
            for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) {
                int a = base + z * 3 + x, b = a + 1, c = a + 3, d = c + 1;
                indices.addAll(Arrays.asList(c, d, b, b, a, c));
            }
        }
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices.toArray(Vector3f[]::new)));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(uv.toArray(Vector2f[]::new)));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals.toArray(Vector3f[]::new)));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indices.stream().mapToInt(Integer::intValue).toArray()));
        mesh.updateBound();
        return mesh;
    }
}
