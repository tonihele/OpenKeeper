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
package toniarts.openkeeper.view.minimap;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;

/**
 * Renders the minimap octagon directly into the {@code guiNode}
 */
public final class MinimapView {

    private final Node guiNode;
    private final Texture2D rasterTexture;
    private final ByteBuffer rasterBuffer;
    private final Geometry octagonGeometry;

    public MinimapView(AssetManager assetManager, Node guiNode) {
        this.guiNode = guiNode;
        int size = MinimapRasteriser.RASTER_SIZE;

        rasterBuffer = ByteBuffer.allocateDirect(size * size * 3);
        Image rasterImage = new Image(Image.Format.BGR8, size, size, rasterBuffer, ColorSpace.sRGB);
        rasterTexture = new Texture2D(rasterImage);
        rasterTexture.setMagFilter(MagFilter.Nearest);
        rasterTexture.setMinFilter(MinFilter.NearestNoMipMaps);
        rasterTexture.setWrap(WrapMode.EdgeClamp);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", rasterTexture);
        material.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.8f));
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Unit octagon (0..1 in both position and UV, since design's
        // identity-UV formula already gives that range) - scaled/positioned
        // every frame in updateLayout() to match the Nifty panel's rect.
        Mesh octagonMesh = MinimapOctagon.build(0.5f, 0.5f, 0.5f);
        octagonGeometry = new Geometry("MinimapOctagon", octagonMesh);
        octagonGeometry.setMaterial(material);
        octagonGeometry.setQueueBucket(RenderQueue.Bucket.Gui);
        octagonGeometry.setCullHint(Spatial.CullHint.Always); // hidden until updateLayout() first runs
    }

    public void attach() {
        guiNode.attachChild(octagonGeometry);
    }

    public void detach() {
        octagonGeometry.removeFromParent();
    }

    /**
     * Repositions/rescales the octagon to match the Nifty panel element's
     * current on-screen rectangle. Nifty's own coordinates are top-left
     * origin, Y down (like most desktop UI toolkits); {@code guiNode}'s
     * {@code Bucket.Gui} content is bottom-left origin, Y up (standard
     * OpenGL convention) - converted here.
     *
     * @param screenHeight the window/back-buffer height in pixels
     */
    public void updateLayout(int niftyX, int niftyY, int niftyWidth, int niftyHeight, int screenHeight) {
        float jmeX = niftyX;
        float jmeY = screenHeight - niftyY - niftyHeight;
        octagonGeometry.setLocalTranslation(jmeX, jmeY, 0f);
        octagonGeometry.setLocalScale(niftyWidth, niftyHeight, 1f);
        octagonGeometry.setCullHint(Spatial.CullHint.Inherit);
    }

    /**
     * Uploads a freshly-rasterised 128x128x3 B,G,R buffer (see
     * {@link MinimapRasteriser#rebuildFitMode}) to the source texture the
     * octagon samples.
     */
    public void updateRaster(byte[] rasterBgr) {
        rasterBuffer.clear();
        rasterBuffer.put(rasterBgr);
        rasterBuffer.flip();
        rasterTexture.getImage().setUpdateNeeded();
    }

}
