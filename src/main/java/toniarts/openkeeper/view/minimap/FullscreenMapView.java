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
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;

/**
 * The full-screen map's picture in the {@code guiNode}: a flat backdrop over
 * the whole screen with the tile raster on top. Positions come from {@link
 * MapLayout}.
 *
 * <p>
 * m
 */
public final class FullscreenMapView {

    private final Node guiNode;
    private final Node rootNode = new Node("FullscreenMap");
    private final Geometry backdrop;
    private final Geometry raster;
    private final ByteBuffer rasterBuffer = ByteBuffer.allocateDirect(MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3);
    private final Texture2D rasterTexture;

    public FullscreenMapView(AssetManager assetManager, Node guiNode) {
        this.guiNode = guiNode;

        Material backdropMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        backdropMaterial.setColor("Color", new ColorRGBA(0.09f, 0.07f, 0.06f, 1f));
        backdrop = quad("FullscreenMapBackdrop", backdropMaterial, false);

        rasterTexture = new Texture2D(new Image(Image.Format.BGR8, MinimapRasteriser.RASTER_SIZE, MinimapRasteriser.RASTER_SIZE,
                rasterBuffer, ColorSpace.sRGB));
        rasterTexture.setMagFilter(MagFilter.Nearest);
        rasterTexture.setMinFilter(MinFilter.NearestNoMipMaps);
        Material rasterMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        rasterMaterial.setTexture("ColorMap", rasterTexture);
        raster = quad("FullscreenMapRaster", rasterMaterial, true); // flipped: raster row 0 (north) on top

        rootNode.setLocalTranslation(0, 0, 10); // above the other gui content
    }

    private Geometry quad(String name, Material material, boolean flipCoords) {
        Geometry geometry = new Geometry(name, new Quad(1, 1, flipCoords));
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Gui);
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
        backdrop.setLocalScale(layout.getScreenWidth(), layout.getScreenHeight(), 1);

        // The layout is top-left origin, jME bottom-left
        raster.setLocalScale(layout.getMapSize(), layout.getMapSize(), 1);
        raster.setLocalTranslation(layout.getMapX(), layout.getScreenHeight() - layout.getMapY() - layout.getMapSize(), 1);
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
