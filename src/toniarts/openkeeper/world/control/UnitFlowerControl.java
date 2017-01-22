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
package toniarts.openkeeper.world.control;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.BillboardControl;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.MapThumbnailGenerator;

/**
 * A base class for showing unit (creature, object...) flower
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class UnitFlowerControl extends BillboardControl {

    private static final float DISPLAY_SECONDS = 2.5f;
    private static final Logger logger = Logger.getLogger(UnitFlowerControl.class.getName());

    private float targetTimeVisible = DISPLAY_SECONDS;
    private float timeVisible = 0;
    private int currentHealthIndex = 0;
    private Node unitSpatial;
    private boolean updateRequired = false;
    private Material material;
    private final IUnitFlowerControl unitFlowerControl;
    private final AssetManager assetManager;

    public UnitFlowerControl(AssetManager assetManager, IUnitFlowerControl unitFlowerControl) {
        this.assetManager = assetManager;
        this.unitFlowerControl = unitFlowerControl;
        enabled = false;
        setAlignment(Alignment.Screen);
    }

    /**
     * Get the unit owner id, for the color
     *
     * @return the unit player id
     */
    protected short getOwnerId() {
        return unitFlowerControl.getOwnerId();
    }

    /**
     * Get unit max health
     *
     * @return max health
     */
    protected int getHealthMax() {
        return unitFlowerControl.getMaxHealth();
    }

    /**
     * Get current unit health
     *
     * @return unit current health
     */
    protected int getHealthCurrent() {
        return unitFlowerControl.getHealth();
    }

    /**
     * Get the center icon resource as a string resource path
     *
     * @return the center icon
     */
    protected String getCenterIcon() {
        return unitFlowerControl.getCenterIcon();
    }

    /**
     * Get unit height, to correctly position the flower
     *
     * @return the unit height
     */
    protected float getHeight() {
        return unitFlowerControl.getHeight();
    }

    /**
     * Get the objective icon as a string resource path, the flower around the
     * icon
     *
     * @return objective icon
     */
    protected String getObjectiveIcon() {
        return null;
    }

    /**
     * Prior to show the flower (note that the flower might already be showing)
     */
    protected void onShow() {

    }

    /**
     * The flower is hidden
     */
    protected void onHide() {

    }

    /**
     * The flower texture has been generated, but the graphics are still open
     * for modifying
     *
     * @param g the graphics for modifying
     */
    protected void onTextureGenerated(Graphics2D g) {

    }

    /**
     * On update loop, only when we are showing
     *
     * @param tpf the time since last update
     * @return true if a redraw of the icon is needed
     */
    protected boolean onUpdate(float tpf) {
        return false;
    }

    /**
     * Show the flower for a brief time
     */
    public final void show() {
        show(DISPLAY_SECONDS);
    }

    /**
     * Show the flower for a period of time
     *
     * @param period the time period the flower should be visible
     */
    public final void show(float period) {

        if (isEnabled() && targetTimeVisible - timeVisible < period) {

            // If already showing, just extend the time
            targetTimeVisible = timeVisible + period;
        } else if (!isEnabled()) {

            // Reset counter and create the graphics
            timeVisible = 0;
            targetTimeVisible = period;
            updateRequired = true;
            unitSpatial.attachChild(getFlower());

            // Enable
            setEnabled(true);

            onShow();
        }
    }

    /**
     * Hide the flower
     */
    public final void hide() {
        setEnabled(false);
        unitSpatial.detachChild(getFlower());

        onHide();
    }

    @Override
    protected final void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

        timeVisible += tpf;
        if (timeVisible >= targetTimeVisible) {

            // Remove us
            hide();

            return;
        }

        // Update health ring
        updateHealth();

        // See if we need to update
        if (onUpdate(tpf) || updateRequired) {
            generateTexture();
        }
    }

    protected Spatial getFlower() {
        if (spatial == null) {
            updateRequired = false;

            Mesh mesh = createMesh(0.5f, 0.5f);
            spatial = new Geometry("Health indicator", mesh);
            material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            Color c = MapThumbnailGenerator.getPlayerColor(getOwnerId());
            material.setColor("Color", new ColorRGBA(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f));
            spatial.setMaterial(material);
            material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
            material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            material.getAdditionalRenderState().setDepthTest(false);
            spatial.setQueueBucket(Bucket.Translucent);
            spatial.setUserData(AssetUtils.USER_DATA_KEY_REMOVABLE, false);

            generateTexture();
        }
        return spatial;
    }

    @Override
    public void setSpatial(Spatial spatial) {

        // Create the spatial
        unitSpatial = (Node) spatial;
        show();
    }

    private void updateHealth() {
        int healthIndex = 5 - (int) Math.ceil((float) getHealthCurrent() / getHealthMax() / (1.0 / 5));
        if (healthIndex != currentHealthIndex) {

            // Reload health ring
            currentHealthIndex = healthIndex;
            updateRequired = true;
        }
    }

    private void generateTexture() {
        if (material != null) {

            // The base image
            BufferedImage flower = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = flower.createGraphics();

            // Health ring
            drawImage(assetManager, g, flower.getWidth(), flower.getHeight(), "Textures/GUI/moods/H-0" + currentHealthIndex + ".png");

            // The rest
            drawImage(assetManager, g, flower.getWidth(), flower.getHeight(), getCenterIcon());
            drawImage(assetManager, g, flower.getWidth(), flower.getHeight(), getObjectiveIcon());

            onTextureGenerated(g);

            // Dispose
            g.dispose();

            // Convert the new image to a texture
            AWTLoader loader = new AWTLoader();
            Texture tex = new Texture2D(loader.load(flower, false));

            material.setTexture("ColorMap", tex);
        }
    }

    private static void drawImage(AssetManager assetManager, Graphics2D g, int width, int height, String image) {

        if (image != null) {
            try {
                // TODO: cache the images
                BufferedImage img = ImageIO.read(assetManager.locateAsset(new AssetKey(image)).openStream());
                g.drawImage(img, (width - img.getWidth()) / 2, (height - img.getHeight()) / 2, null);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Can't load the texture " + image + "!", ex);
            }
        }
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
        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{-width / 2f, -height / 2f + getHeight(), 0,
            width / 2f, -height / 2f + getHeight(), 0,
            width / 2f, height / 2f + getHeight(), 0,
            -width / 2f, height / 2f + getHeight(), 0
        });

        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 1,
            1, 1,
            1, 0,
            0, 0});
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, new float[]{0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1});
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2,
            0, 2, 3});

        mesh.updateBound();
        mesh.setStatic();
        return mesh;
    }

    /**
     * Show the unit flower
     *
     * @param control the control to own the flower control
     * @param seconds how many seconds to show, can be {@code null}
     */
    public static void showUnitFlower(AbstractControl control, Integer seconds) {
        UnitFlowerControl aufc = control.getSpatial().getControl(UnitFlowerControl.class);
        if (seconds != null) {
            aufc.show(seconds);
        } else {
            aufc.show();
        }
    }

}
