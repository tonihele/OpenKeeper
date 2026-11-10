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
package toniarts.openkeeper.view.effect.procedural;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Random;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Creates the procedural meshes, the art resources that the original
 * generates in code rather than loads. The art resource only carries an ID.
 * <p>
 * The IDs found in the data are 2 (the keeper's Thunderbolt), 7 (the
 * creature and trap lightning, from the caster to the target) and 8 (an
 * effect called Lightning Bolt). Only the Thunderbolt is done.
 */
public final class ProceduralMeshFactory {

    private static final Logger logger = System.getLogger(ProceduralMeshFactory.class.getName());

    /**
     * The keeper's Thunderbolt, a bolt from above striking down at the origin
     */
    public static final int THUNDERBOLT_ID = 2;

    private static final int THUNDERBOLT_SEGMENTS = 12;
    private static final float THUNDERBOLT_HEIGHT = 3 * WorldUtils.TILE_HEIGHT;
    private static final float THUNDERBOLT_JITTER = 0.2f * WorldUtils.TILE_WIDTH;

    private ProceduralMeshFactory() {
        // Nope
    }

    /**
     * Creates the procedural mesh
     *
     * @param resource the procedural mesh art resource
     * @param assetManager the asset manager
     * @return the mesh, an empty node for the ones not done yet
     */
    public static Node create(ArtResource resource, AssetManager assetManager) {
        Node node = new Node(resource.getName());
        Number id = resource.getData(ArtResource.KEY_ID);
        if (id != null && id.intValue() == THUNDERBOLT_ID) {
            node.attachChild(createThunderbolt(assetManager));
        } else {
            logger.log(Level.DEBUG, "Procedural mesh {0} ({1}) is not implemented", id, resource.getName());
        }

        return node;
    }

    private static Geometry createThunderbolt(AssetManager assetManager) {
        LightningBoltMesh mesh = new LightningBoltMesh(THUNDERBOLT_SEGMENTS, THUNDERBOLT_HEIGHT, THUNDERBOLT_JITTER);
        Geometry geometry = new Geometry("Thunderbolt", mesh);
        geometry.setMaterial(createGlowMaterial(assetManager));
        geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
        geometry.setShadowMode(RenderQueue.ShadowMode.Off);
        geometry.addControl(new LightningBoltControl(mesh, new Random()));

        return geometry;
    }

    private static Material createGlowMaterial(AssetManager assetManager) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setBoolean("VertexColor", true);
        RenderState renderState = material.getAdditionalRenderState();
        renderState.setBlendMode(RenderState.BlendMode.AlphaAdditive);
        renderState.setDepthWrite(false);
        renderState.setFaceCullMode(RenderState.FaceCullMode.Off);

        return material;
    }

}
