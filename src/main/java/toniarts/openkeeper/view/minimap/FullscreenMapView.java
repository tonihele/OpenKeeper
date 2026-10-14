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
import com.jme3.math.ColorRGBA;
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
 * The full-screen map's picture in the {@code guiNode}: a flat backdrop over
 * the whole screen, the tile raster, and the preview window with a frame.
 * Positions come from {@link MapLayout}.
 *
 * <p>
 * TODO the DK2 backdrop and frame art is not available, the backdrop and
 * frame are flat colours.
 */
public final class FullscreenMapView {

    private static final String ROCK_TEXTURE_PATH = "Textures/GUI/Map/Map-BG.png";

    private final Node guiNode;
    private final Node rootNode = new Node("FullscreenMap");
    private final Geometry backdrop;
    private final Geometry raster;
    private final Geometry previewFrame;
    private final Geometry previewRock;
    private final Geometry preview;
    private final ByteBuffer rasterBuffer = ByteBuffer.allocateDirect(MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3);
    private final Texture2D rasterTexture;

    /**
     * @param previewTexture the texture the 3D preview is rendered into
     */
    public FullscreenMapView(AssetManager assetManager, Node guiNode, Texture2D previewTexture) {
        this.guiNode = guiNode;

        backdrop = quad("Backdrop", solid(assetManager, new ColorRGBA(0.09f, 0.07f, 0.06f, 1f)), false);

        rasterTexture = new Texture2D(new Image(Image.Format.BGR8, MinimapRasteriser.RASTER_SIZE, MinimapRasteriser.RASTER_SIZE,
                rasterBuffer, ColorSpace.sRGB));
        rasterTexture.setMagFilter(MagFilter.Nearest);
        rasterTexture.setMinFilter(MinFilter.NearestNoMipMaps);
        raster = quad("Raster", textured(assetManager, rasterTexture), true); // flipped: raster row 0 (north) on top

        previewFrame = quad("PreviewFrame", solid(assetManager, new ColorRGBA(0.32f, 0.26f, 0.2f, 1f)), false);
        previewRock = quad("PreviewRock", textured(assetManager, assetManager.loadTexture(ROCK_TEXTURE_PATH)), false);
        preview = quad("Preview", textured(assetManager, previewTexture), false);

        rootNode.setLocalTranslation(0, 0, 10); // above the other gui content
    }

    private static Material solid(AssetManager assetManager, ColorRGBA colour) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", colour);
        return material;
    }

    private static Material textured(AssetManager assetManager, Texture texture) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", texture);
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
        backdrop.setLocalScale(layout.getScreenWidth(), screenHeight, 1);
        place(raster, layout.getMap(), screenHeight);

        MapLayout.Rect window = layout.getPreview();
        float border = window.w() / 32f;
        place(previewFrame, new MapLayout.Rect(window.x() - border, window.y() - border,
                window.w() + 2 * border, window.h() + 2 * border), screenHeight);
        place(previewRock, window, screenHeight);
        place(preview, window, screenHeight);
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
     * Shows the live 3D preview, or the rock instead of it (e.g. over
     * unexplored ground, so the map can't be used to scout)
     */
    public void setPreviewVisible(boolean visible) {
        preview.setCullHint(visible ? CullHint.Inherit : CullHint.Always);
    }

    /**
     * Uploads a 128x128x3 B,G,R raster
     */
    public void updateRaster(byte[] rasterBgr) {
        rasterBuffer.clear();
        rasterBuffer.put(rasterBgr);
        rasterBuffer.flip();
        rasterTexture.getImage().setUpdateNeeded();
    }
}
