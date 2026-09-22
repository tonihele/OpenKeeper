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
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;

/**
 * Renders the minimap disc, and its fit-mode frustum overlay, directly
 * into the {@code guiNode} (see this class's own history/javadoc on why:
 * the design doc's recommended off-screen {@code FrameBuffer} +
 * {@code RenderImageJme} approach doesn't work against this project's
 * actual Nifty batch-render backend). Both are children of a shared
 * {@link #overlayNode}, repositioned/rescaled together every frame
 * ({@link #updateLayout}) to match wherever the Nifty panel element
 * currently sits on screen.
 */
public final class MinimapView {

    private final Node guiNode;
    private final Node overlayNode;
    private final Texture2D rasterTexture;
    private final ByteBuffer rasterBuffer;
    private final Mesh discMesh;
    private final Geometry discGeometry;
    private final Mesh frustumMesh;
    private final Geometry frustumGeometry;

    public MinimapView(AssetManager assetManager, Node guiNode) {
        this.guiNode = guiNode;
        overlayNode = new Node("MinimapOverlay");

        int size = MinimapRasteriser.RASTER_SIZE;

        rasterBuffer = ByteBuffer.allocateDirect(size * size * 3);
        Image rasterImage = new Image(Image.Format.BGR8, size, size, rasterBuffer, ColorSpace.sRGB);
        rasterTexture = new Texture2D(rasterImage);
        rasterTexture.setMagFilter(MagFilter.Nearest);
        rasterTexture.setMinFilter(MinFilter.NearestNoMipMaps);
        rasterTexture.setWrap(WrapMode.EdgeClamp);

        Material rasterMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        rasterMaterial.setTexture("ColorMap", rasterTexture);
        rasterMaterial.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.8f));
        rasterMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Unit disc (0..1 in both position and UV, since design's
        // identity-UV formula already gives that range).
        discMesh = MinimapDisc.build(0.5f, 0.5f, 0.5f);
        discGeometry = new Geometry("MinimapDisc", discMesh);
        discGeometry.setMaterial(rasterMaterial);
        discGeometry.setQueueBucket(RenderQueue.Bucket.Gui);
        overlayNode.attachChild(discGeometry);

        // Camera box
        Material frustumMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        frustumMaterial.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.75f));
        frustumMaterial.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        frustumMaterial.getAdditionalRenderState().setLineWidth(2f);

        frustumMesh = buildFrustumQuad();
        frustumGeometry = new Geometry("MinimapFrustum", frustumMesh);
        frustumGeometry.setMaterial(frustumMaterial);
        frustumGeometry.setQueueBucket(RenderQueue.Bucket.Gui);
        frustumGeometry.setCullHint(Spatial.CullHint.Always); // shown only by updateFrustum, fit mode only
        overlayNode.attachChild(frustumGeometry);

        overlayNode.setCullHint(Spatial.CullHint.Always); // hidden until updateLayout() first runs
    }

    private static Mesh buildFrustumQuad() {
        // An outline, not a filled quad - LineLoop connects
        // the 4 corners 0-1-2-3-0 directly, no index buffer needed.
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, new float[4 * 3]);
        mesh.setMode(Mesh.Mode.LineLoop);
        mesh.updateBound();
        mesh.updateCounts();
        return mesh;
    }

    public void attach() {
        guiNode.attachChild(overlayNode);
    }

    public void detach() {
        overlayNode.removeFromParent();
    }

    /**
     * Repositions/rescales the overlay to match the Nifty panel element's
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
        overlayNode.setLocalTranslation(jmeX, jmeY, 0f);
        overlayNode.setLocalScale(niftyWidth, niftyHeight, 1f);
        overlayNode.setCullHint(Spatial.CullHint.Inherit);
    }

    /**
     * Uploads a freshly-rasterised 128x128x3 B,G,R buffer (see
     * {@link MinimapRasteriser#rebuildFitMode}) to the source texture the
     * disc samples.
     */
    public void updateRaster(byte[] rasterBgr) {
        rasterBuffer.clear();
        rasterBuffer.put(rasterBgr);
        rasterBuffer.flip();
        rasterTexture.getImage().setUpdateNeeded();
    }

    /**
     * Rewrites the disc's texture coordinates for the current camera
     * yaw. Called every rendered frame, independent of the
     * raster rebuild cadence, since the camera can turn between rebuilds.
     */
    public void updateYaw(float yawRadians) {
        MinimapDisc.updateUv(discMesh, yawRadians);
    }

    /**
     * Hides the frustum overlay. Used in zoomed mode and whenever the
     * camera is in whatever mode
     */
    public void hideFrustum() {
        frustumGeometry.setCullHint(Spatial.CullHint.Always);
    }

    /**
     * Positions the frustum quad from the camera's 4 ground-plane frustum
     * corners: each corner is converted to tile space, then fit-mode pixel
     * space (the same mapping the raster itself uses -
     * {@link MinimapRasteriser.FitGeometry}), then rotated
     * around the raster centre to track the disc's own content rotation
     * before being normalised into this view's 0..1 local unit space.
     *
     * @param groundCorners from {@link MinimapFrustum#groundCorners} -
     * hides the overlay if any entry is {@code null} (a corner's ray missed
     * the ground plane)
     */
    public void updateFrustum(Vector3f[] groundCorners, int mapWidth, int mapHeight, float yawRadians) {
        for (Vector3f corner : groundCorners) {
            if (corner == null) {
                hideFrustum();
                return;
            }
        }

        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(mapWidth, mapHeight);
        int rasterSize = MinimapRasteriser.RASTER_SIZE;
        float cos = FastMath.cos(-yawRadians);
        float sin = FastMath.sin(-yawRadians);

        float[] positions = new float[groundCorners.length * 3];
        for (int i = 0; i < groundCorners.length; i++) {
            Vector2f tile = MinimapCoordinates.worldToTile(groundCorners[i]);
            float px = fit.pixelX(tile.x);
            float py = fit.pixelY(tile.y);

            // Raster pixel space -> this view's local unit space (0..1),
            // matching the disc's own confirmed position<->UV
            // relationship at yaw 0 (V flipped, U not).
            float localX = px / rasterSize;
            float localY = 1f - py / rasterSize;

            float dx = localX - 0.5f;
            float dy = localY - 0.5f;
            positions[i * 3] = 0.5f + dx * cos - dy * sin;
            positions[i * 3 + 1] = 0.5f + dx * sin + dy * cos;
            positions[i * 3 + 2] = 0f;
        }

        frustumMesh.setBuffer(Type.Position, 3, positions);
        frustumMesh.updateBound();
        frustumGeometry.setCullHint(Spatial.CullHint.Inherit);
    }

}
