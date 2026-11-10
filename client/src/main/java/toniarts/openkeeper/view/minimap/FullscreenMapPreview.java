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

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * The map's live 3D preview: a second camera looking straight down at the
 * tile under the cursor, rendering the normal world into a texture. Nothing
 * is pre-rendered, so animation and lighting are live.
 */
public final class FullscreenMapPreview {

    /**
     * Render texture size, twice the size of the 128x192 reference window
     */
    private static final int WIDTH = 256;
    private static final int HEIGHT = 384;

    /**
     * Camera height above the floor, in tiles. With the field of view below
     * this shows about 4.3 x 6.4 tiles.
     */
    private static final float CAMERA_HEIGHT = 8f;

    /**
     * Vertical field of view: DK2's focal length of 240 px in a 192 px tall window
     */
    private static final float FIELD_OF_VIEW = 2f * FastMath.atan(96f / 240f) * FastMath.RAD_TO_DEG;
    private static final float DRAW_DISTANCE = 10f;

    private final RenderManager renderManager;
    private final Camera camera = new Camera(WIDTH, HEIGHT);
    private final Texture2D texture = new Texture2D(WIDTH, HEIGHT, Image.Format.RGBA8);
    private final ViewPort viewPort;
    private float aspect = WIDTH / (float) HEIGHT;

    public FullscreenMapPreview(RenderManager renderManager, Node scene) {
        this.renderManager = renderManager;

        camera.setFrustumPerspective(FIELD_OF_VIEW, aspect, 0.1f, DRAW_DISTANCE);

        FrameBuffer frameBuffer = new FrameBuffer(WIDTH, HEIGHT, 1);
        frameBuffer.setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth));
        frameBuffer.addColorTarget(FrameBufferTarget.newTarget(texture));

        viewPort = renderManager.createPreView("FullscreenMapPreview", camera);
        viewPort.setClearFlags(true, true, true);
        viewPort.setBackgroundColor(ColorRGBA.Black);
        viewPort.setOutputFrameBuffer(frameBuffer);
        viewPort.attachScene(scene);
        viewPort.setEnabled(false);
    }

    /**
     * The texture the preview is rendered into
     */
    public Texture2D getTexture() {
        return texture;
    }

    /**
     * Sets the shape of the window the picture is shown in, so it is not
     * distorted when the window is stretched to the screen
     */
    public void setAspect(float aspect) {
        if (aspect != this.aspect) {
            this.aspect = aspect;
            camera.setFrustumPerspective(FIELD_OF_VIEW, aspect, 0.1f, DRAW_DISTANCE);
        }
    }

    public void stop() {
        viewPort.setEnabled(false);
    }

    /**
     * Points the camera at a spot of the map and renders it, or stops
     * rendering if there is none
     *
     * @param tile fractional tile coordinates, or {@code null}
     */
    public void lookAt(Vector2f tile, int mapWidth, int mapHeight) {
        viewPort.setEnabled(tile != null);
        if (tile == null) {
            return;
        }

        placeCamera(camera, tile, mapWidth, mapHeight);
    }

    /**
     * Puts the camera above the tile, looking straight down with north (smaller
     * tile y) up. It stays inside the playable area, so the window never shows
     * the void around the map.
     */
    static void placeCamera(Camera camera, Vector2f tile, int mapWidth, int mapHeight) {
        float x = FastMath.clamp(tile.x, 1, Math.max(1, mapWidth - 2));
        float y = FastMath.clamp(tile.y, 1, Math.max(1, mapHeight - 2));
        camera.setLocation(new Vector3f(x, CAMERA_HEIGHT, y));
        camera.lookAtDirection(Vector3f.UNIT_Y.negate(), new Vector3f(0, 0, -1));
    }

    public void dispose() {
        stop();
        renderManager.removePreView(viewPort);
    }
}
