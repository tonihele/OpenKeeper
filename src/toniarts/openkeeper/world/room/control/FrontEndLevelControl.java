/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.openkeeper.world.room.control;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * Controls level graphics
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class FrontEndLevelControl extends AbstractControl {

    private final int level;
    private final String variation;
    private final AssetManager assetManager;
    private volatile boolean active = false;
    private volatile boolean moved = true;
    private Vector3f baseLocation;
    private float baseProgess;
    private Long lastTime;
    private static final Vector3f MOVE_VECTOR = new Vector3f(0, 0.025f, 0);
    private static final int ACTIVATE_ANIMATION_LENGTH = 250;
    private static final int DEACTIVATE_ANIMATION_LENGTH = 1500;

    public FrontEndLevelControl(int level, String variation, AssetManager assetManager) {
        this.level = level;
        this.variation = variation;
        this.assetManager = assetManager;
    }

    @Override
    public void setSpatial(Spatial spatial) {
        super.setSpatial(spatial);

        // Set the base location
        baseLocation = new Vector3f(spatial.getLocalTranslation());
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (moved == false) {
            if (lastTime != null) {
                long elapsedTime = System.currentTimeMillis() - lastTime;
                if (active) {

                    // If the spatial starts in the base location, play the sound
                    if (getSpatial().getLocalTranslation().equals(baseLocation)) {
                        AudioNode fx = new AudioNode(assetManager, "Sounds/Global/RockRise nl.mp2", false);
                        fx.setLooping(false);
//                        fx.play();
                    }

                    // Play the animation
                    playAnimation(ACTIVATE_ANIMATION_LENGTH, baseLocation, baseLocation.add(MOVE_VECTOR), elapsedTime);
                } else {
                    playAnimation(DEACTIVATE_ANIMATION_LENGTH, baseLocation.add(MOVE_VECTOR), baseLocation, elapsedTime);
                }
            } else {
                lastTime = System.currentTimeMillis();
                baseProgess = baseLocation.distance(spatial.getLocalTranslation()) / baseLocation.distance(baseLocation.add(MOVE_VECTOR));
                if (!active) {
                    baseProgess = 1f - baseProgess;
                }
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        /* Optional: rendering manipulation (for advanced users) */
    }

    /**
     * Sets this controller active (not enabled/disabled)
     *
     * @param active active status
     */
    public void setActive(boolean active) {
        if (active != this.active) {
            this.moved = false;
            this.lastTime = null;
        }
        this.active = active;
    }

    /**
     * Get the level number
     *
     * @return level number
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get level variation, like a, b etc.
     *
     * @return level variation, can be null
     */
    public String getVariation() {
        return variation;
    }

    private void playAnimation(int animationLength, Vector3f base, Vector3f target, long elapsedTime) {
        float progess = (float) elapsedTime / animationLength + baseProgess;

        // See if we should end already
        if (elapsedTime > animationLength || progess > 1.0f) {

            // The end
            moved = true;
            lastTime = null;

            // Ensure the position
            getSpatial().setLocalTranslation(target);
        } else {

            // Interpolate the position
            getSpatial().setLocalTranslation(new Vector3f().interpolate(base, target, progess));
        }
    }
}
