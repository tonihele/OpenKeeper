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
package toniarts.openkeeper.view.control;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import jme3tools.optimize.TextureAtlas;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 *
 * @author ArchDemon
 */
public final class TorchControl extends BillboardControl {

    private static final Logger logger = System.getLogger(TorchControl.class.getName());

    private static final int FRAMES = 20;
    private Material material;
    private Node torch;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private static final AssetKey<Spatial> ASSET_KEY = new AssetKey<>("TorchFlame");

    public TorchControl(KwdFile kwdFile, AssetManager assetManager, float angle) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        setAlignment(Alignment.AxialY);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        torch = (Node) spatial;

        if (torch != null) {
            this.spatial = createFlame();
            if (this.spatial != null) {
                torch.attachChild(this.spatial);
            }
        }
    }

    private Spatial createFlame() {
        Spatial result = assetManager.getFromCache(ASSET_KEY);

        if (result == null) {
            try {
                material = createMaterial();
                material.setTexture("DiffuseMap", createTexture());

                result = new Geometry("torch flame", createMesh(0.5f, 0.5f));
                result.setMaterial(material);
                result.setQueueBucket(RenderQueue.Bucket.Translucent);
                result.move(0.14f, 0.2f, 0);
                result.setShadowMode(RenderQueue.ShadowMode.Off);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Can't create torch flame", e);
            }

            assetManager.addToCache(ASSET_KEY, result);
        }

        if (result != null) {
            return result.clone();
        }

        return null;
    }

    /**
     * Creates a quad, just that this one is centered on x-axis and on y-axis
     * lifted up by the unit height
     *
     * @param width width
     * @param height height
     * @return the mesh
     */
    private Mesh createMesh(float width, float height) {
        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{
            -width / 2f, 0, 0,
            width / 2f, 0, 0,
            width / 2f, height, 0,
            -width / 2f, height, 0
        });
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, new byte[]{0, 0,  1, 0,  1, 1,  0, 1});
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new byte[]{0, 1, 2,  0, 2, 3});
        mesh.updateBound();
        return mesh;
    }

    private Texture createTexture() throws IOException {
        String name = "ktorch";

        var frameTexture = assetManager.loadTexture("Textures/" + name + "0.png");
        var frameImage = frameTexture.getImage();
        var atlas = new TextureAtlas(frameImage.getWidth() * FRAMES, frameImage.getHeight());
        boolean added = atlas.addTexture(frameTexture, "DiffuseMap");
        if (!added)
            logger.log(Level.ERROR, "Failed to add frame to atlas");

        for (int i = 1; i < FRAMES; ++i) {
            frameTexture = assetManager.loadTexture("Textures/" + name + i + ".png");
            added = atlas.addTexture(frameTexture, "DiffuseMap");
            if (!added)
                logger.log(Level.ERROR, "Failed to add frame {0} to atlas", i);
        }

        var atlasTexture = atlas.getAtlasTexture("DiffuseMap");
        atlasTexture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        atlasTexture.setMagFilter(Texture.MagFilter.Nearest);
        atlasTexture.setWrap(Texture.WrapMode.EdgeClamp);
        return atlasTexture;
    }

    private Material createMaterial() {
        Material result = new Material(assetManager, "MatDefs/LightingSprite.j3md");

        result.setInt("NumberOfTiles", FRAMES);
        result.setInt("Speed", FRAMES); // FIXME: correct value

        result.setTransparent(true);
        result.setFloat("AlphaDiscardThreshold", 0.1f);

        result.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        result.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        //result.getAdditionalRenderState().setDepthTest(false);

        return result;
    }
}
