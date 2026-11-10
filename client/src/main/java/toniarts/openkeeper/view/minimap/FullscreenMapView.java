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
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;

/**
 * The full-screen map's picture in the {@code guiNode}, back to front: the
 * backdrop over the whole screen, the rock under the map, the tile raster,
 * the preview window (the 3D render, or rock) and the preview's frame
 * overlay. Positions come from {@link MapLayout}.
 */
public final class FullscreenMapView {

    private static final String BACKDROP_PATH = "Textures/MapBack.PNG";
    private static final String ROCK_PATH = "Textures/MapRock.png";
    private static final String PREVIEW_FRAME_PATH = "Textures/MapOver.png";

    /**
     * Raster pixels of this colour are not painted, the rock shows through.
     * Give {@link MinimapRasteriser#rebuildFitMode} this as the rock texture.
     */
    private static final byte[] ROCK_KEY_BGR = new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];

    static {
        for (int i = 0; i < ROCK_KEY_BGR.length; i += 3) {
            ROCK_KEY_BGR[i] = 1;
            ROCK_KEY_BGR[i + 1] = 2;
            ROCK_KEY_BGR[i + 2] = 3;
        }
    }

    /**
     * A fresh copy of the rock key texture. Callers that need it repeatedly
     * should keep the copy rather than call this each time.
     */
    static byte[] rockKeyBgr() {
        return ROCK_KEY_BGR.clone();
    }

    private final Node guiNode;
    private final Node rootNode = new Node("FullscreenMap");
    private final Geometry backdrop;
    private final Geometry mapRock;
    private final Geometry raster;
    private final Geometry previewRock;
    private final Geometry preview;
    private final Geometry previewFrame;
    private final Node overlayNode = new Node("Overlay");
    private final ByteBuffer rasterBuffer = ByteBuffer.allocateDirect(MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 4);
    private final Texture2D rasterTexture;

    /**
     * @param previewTexture the texture the 3D preview is rendered into
     * @throws AssetNotFoundException if one of the images is missing
     */
    public FullscreenMapView(AssetManager assetManager, Node guiNode, Texture2D previewTexture) {
        this.guiNode = guiNode;

        backdrop = quad("Backdrop", textured(assetManager, assetManager.loadTexture(BACKDROP_PATH), false), false);
        mapRock = quad("MapRock", textured(assetManager, assetManager.loadTexture(ROCK_PATH), false), false);

        rasterTexture = new Texture2D(new Image(Image.Format.RGBA8, MinimapRasteriser.RASTER_SIZE, MinimapRasteriser.RASTER_SIZE,
                rasterBuffer, ColorSpace.sRGB));
        rasterTexture.setMagFilter(MagFilter.Nearest);
        rasterTexture.setMinFilter(MinFilter.NearestNoMipMaps);
        raster = quad("Raster", textured(assetManager, rasterTexture, true), true); // flipped: raster row 0 (north) on top

        previewRock = quad("PreviewRock", textured(assetManager, assetManager.loadTexture(ROCK_PATH), false), false);
        preview = quad("Preview", textured(assetManager, previewTexture, false), false);
        previewFrame = quad("PreviewFrame", textured(assetManager, assetManager.loadTexture(PREVIEW_FRAME_PATH), true), false);

        overlayNode.setLocalTranslation(0, 0, rootNode.getQuantity()); // above all the quads
        rootNode.attachChild(overlayNode);
        rootNode.setLocalTranslation(0, 0, 10); // above the other gui content
    }

    /**
     * Where the overlays (units, room icon, legend) go
     */
    public Node getOverlayNode() {
        return overlayNode;
    }

    private static Material textured(AssetManager assetManager, Texture texture, boolean transparent) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", texture);
        if (transparent) {
            material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        }
        return material;
    }

    /**
     * A unit quad, scaled and moved into place by {@link #updateLayout}.
     * Quads are stacked in the order they are made.
     */
    private Geometry quad(String name, Material material, boolean flipCoords) {
        Geometry geometry = new Geometry(name, new Quad(1, 1, flipCoords));
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Gui);
        geometry.setLocalTranslation(0, 0, rootNode.getQuantity());
        rootNode.attachChild(geometry);
        return geometry;
    }

    public void attach() {
        guiNode.attachChild(rootNode);
    }

    public void detach() {
        rootNode.removeFromParent();
    }

    public void updateLayout(MapLayout layout) {
        int screenHeight = layout.getScreenHeight();
        place(backdrop, layout.getBackdrop(), screenHeight);
        place(mapRock, layout.getMap(), screenHeight);
        place(raster, layout.getMap(), screenHeight);
        place(previewRock, layout.getPreview(), screenHeight);
        place(preview, layout.getPreview(), screenHeight);
        place(previewFrame, layout.getPreview(), screenHeight);
    }

    /**
     * Fits a quad on a screen rectangle, keeping its stacking depth (the
     * layout is top-left origin, jME bottom-left)
     */
    private static void place(Geometry quad, MapLayout.Rect rect, int screenHeight) {
        quad.setLocalScale(rect.w(), rect.h(), 1);
        quad.setLocalTranslation(rect.x(), screenHeight - rect.y() - rect.h(), quad.getLocalTranslation().z);
    }

    /**
     * Shows the live 3D preview, or the rock instead of it (when the cursor
     * is not over the map)
     */
    public void setPreviewVisible(boolean visible) {
        preview.setCullHint(visible ? CullHint.Inherit : CullHint.Always);
    }

    /**
     * Uploads a 128x128x3 B,G,R raster. Pixels of the {@link #ROCK_KEY_BGR}
     * colour become transparent.
     */
    public void updateRaster(byte[] rasterBgr) {
        rasterBuffer.clear();
        for (int i = 0; i < rasterBgr.length; i += 3) {
            boolean rock = rasterBgr[i] == 1 && rasterBgr[i + 1] == 2 && rasterBgr[i + 2] == 3;
            rasterBuffer.put(rasterBgr[i + 2]).put(rasterBgr[i + 1]).put(rasterBgr[i]).put((byte) (rock ? 0 : 255));
        }
        rasterBuffer.flip();
        rasterTexture.getImage().setUpdateNeeded();
    }
}
