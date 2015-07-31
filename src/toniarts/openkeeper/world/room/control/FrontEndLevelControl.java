/*
 * Copyright (C) 2014-2015 OpenKeeper
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

    private final String type;
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

    public FrontEndLevelControl(String type, int level, String variation, AssetManager assetManager) {
        this.type = type;
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
                        fx.setPositional(true);
                        fx.play();
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
     * Get level like, like Secret or Level etc.
     *
     * @return level type
     */
    public String getType() {
        return type;
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
