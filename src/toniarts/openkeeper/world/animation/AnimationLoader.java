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
package toniarts.openkeeper.world.animation;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.Anim;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.control.UnitFlowerControl;

/**
 * Static helpers to handle animations in our basic scene objects
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AnimationLoader {

    private static final String START_ANIMATION_NAME = "Start";
    private static final String END_ANIMATION_NAME = "End";
    private static final String ANIM_NAME = "anim";
    private static final Logger LOGGER = Logger.getLogger(AnimationLoader.class.getName());

    private AnimationLoader() {
        // No!
    }

    private static void attachResource(final Node root, final AnimationControl animationControl, final ArtResource resource, AssetManager assetManager) {
        if (resource != null && (resource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH
                || resource.getType() == ArtResource.ArtResourceType.MESH
                || resource.getType() == ArtResource.ArtResourceType.PROCEDURAL_MESH)) {
            try {

                Spatial spat = loadModel(assetManager, resource.getName(), root);
                spat.setName(resource.getName());

                // If the animations has end and/or start, it is located in a different file
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                    String name = resource.getName() + START_ANIMATION_NAME;
                    Spatial spatStart = loadModel(assetManager, name, root);
                    spatStart.setName(START_ANIMATION_NAME);

                    // Create kinda a custom animation control
                    AnimControl animControl = spatStart.getControl(AnimControl.class);
                    if (animControl != null) {
                        animControl.setEnabled(false);
                        animControl.addListener(new AnimEventListener() {

                            @Override
                            public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

                                // Start the real animation
                                Spatial spat = root.getChild(resource.getName());
                                AnimControl animControl = spat.getControl(AnimControl.class);
                                spat.setCullHint(Spatial.CullHint.Inherit);
                                AnimChannel c = animControl.getChannel(0);
                                LoopMode loopMode = c.getLoopMode();
                                c.setAnim(ANIM_NAME, 0);
                                if (loopMode != null) {
                                    c.setLoopMode(loopMode);
                                }
                                animControl.setEnabled(true);

                                // Hide us
                                control.setEnabled(false);
                                control.getSpatial().setCullHint(Spatial.CullHint.Always);
                            }

                            @Override
                            public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                            }
                        });
                        animControl.createChannel();
                    }
                }
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                    String name = resource.getName() + END_ANIMATION_NAME;
                    Spatial spatEnd = loadModel(assetManager, name, root);
                    spatEnd.setName(END_ANIMATION_NAME);

                    // Create kinda a custom animation control
                    AnimControl animControl = spatEnd.getControl(AnimControl.class);
                    if (animControl != null) {
                        animControl.setEnabled(false);
                        animControl.addListener(new AnimEventListener() {

                            @Override
                            public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {

                                // Stop us
                                control.setEnabled(false);

                                // Signal stop
                                animationControl.onAnimationStop();
                            }

                            @Override
                            public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                            }
                        });
                        animControl.createChannel();
                    }
                }

                // Create kinda a custom animation control
                AnimControl animControl = spat.getControl(AnimControl.class);
                if (animControl != null) {
                    animControl.setEnabled(false);
                    animControl.addListener(new AnimEventListener() {

                        private int cyclesCount = 0;

                        @Override
                        public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
                            cyclesCount++;

                            // Signal that the main animation cycle is done
                            animationControl.onAnimationCycleDone();

                            // See if we need to stop
                            if (animationControl.isStopAnimation() || channel.getLoopMode() == LoopMode.DontLoop) {

                                // Stop us
                                control.setEnabled(false);

                                // We need to stop
                                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {

                                    // Hide us
                                    control.getSpatial().setCullHint(Spatial.CullHint.Always);

                                    Spatial spat = root.getChild(END_ANIMATION_NAME);
                                    AnimControl animControl = spat.getControl(AnimControl.class);
                                    spat.setCullHint(Spatial.CullHint.Inherit);
                                    AnimChannel c = animControl.getChannel(0);
                                    LoopMode loopMode = c.getLoopMode();
                                    c.setAnim(ANIM_NAME, 0);
                                    if (loopMode != null) {
                                        c.setLoopMode(loopMode);
                                    }
                                    animControl.setEnabled(true);
                                } else {

                                    // Signal stop
                                    animationControl.onAnimationStop();
                                }
                            }
                        }

                        @Override
                        public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {

                        }
                    });
                    AnimChannel channel = animControl.createChannel();
                    setLoopModeOnChannel(spat, channel);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage());
            }
        }
    }

    public static void setLoopModeOnChannel(final Spatial spat, final AnimChannel channel) {
        final Anim.FrameFactorFunction func = Anim.FrameFactorFunction.valueOf(spat.getUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION));
        switch (func) {
            case CLAMP: {
                channel.setLoopMode(LoopMode.Cycle);
                break;
            }
            case WRAP: {
                channel.setLoopMode(LoopMode.Loop);
                break;
            }
        }
    }

    private static Spatial loadModel(AssetManager assetManager, String resourceName, Node creatureRoot) {

        // Load the model and attach it without the root
        Spatial model = AssetUtils.loadModel(assetManager, resourceName);
        model = ((Node) model).getChild(0);
        model.setCullHint(Spatial.CullHint.Always);
        creatureRoot.attachChild(model);
        return model;
    }

    /**
     * Start playing an animation on the creature
     *
     * @param spatial the creature root
     * @param anim wanted animation
     * @param assetManager the AssetManager
     * @param endFrame start from the end frame
     */
    public static void playAnimation(Spatial spatial, ArtResource anim, AssetManager assetManager, boolean endFrame) {

        Node root = (Node) spatial;

        // Attach the anim node and get rid of the rest
        for (Spatial child : root.getChildren()) {
            if (Boolean.FALSE.equals(child.getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))) {
                continue;
            }
            child.removeFromParent();
        }
        attachResource(root, root.getControl(AnimationControl.class), anim, assetManager);

        try {
            // Get the anim node
            String animNodeName = anim.getName();
            if (anim.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                animNodeName = START_ANIMATION_NAME;
            }
            Spatial spat = root.getChild(animNodeName);
            if (spat != null) {

                // Hide all
                hideAllNodes(root);

                // Show the anim
                AnimControl animControl = spat.getControl(AnimControl.class);
                spat.setCullHint(Spatial.CullHint.Inherit);
                if (animControl != null) { // Not all are anims
                    AnimChannel channel = animControl.getChannel(0);
                    LoopMode loopMode = channel.getLoopMode();
                    channel.setAnim(ANIM_NAME, 0);
                    if (loopMode != null) {
                        channel.setLoopMode(loopMode);
                    }
                    if (endFrame) {
                        channel.setTime(Integer.MAX_VALUE);
                    }
                    animControl.setEnabled(true);
                }
            }
        } catch (Exception e) {
            // FIXME sometimes NPE in CreatureControl.java
            // line: playAnimation(creature.getAnimEntranceResource());
            LOGGER.log(Level.SEVERE, "Creature animation playing error: {0}", e.toString());
        }
    }

    /**
     * @param spatial
     * @param anim
     * @param assetManager
     * @see #playAnimation(com.jme3.scene.Spatial,
     * toniarts.openkeeper.tools.convert.map.ArtResource,
     * com.jme3.asset.AssetManager, boolean)
     */
    public static void playAnimation(Spatial spatial, ArtResource anim, AssetManager assetManager) {
        playAnimation(spatial, anim, assetManager, false);
    }

    private static void hideAllNodes(Node root) {
        UnitFlowerControl aufc = root.getControl(UnitFlowerControl.class);
        for (Spatial child : root.getChildren()) {

            // Don't hide the unit flower
            if (Boolean.FALSE.equals(child.getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))) {
                continue;
            }

            child.setCullHint(Spatial.CullHint.Always);

            // Also stop any animations
            AnimControl animControl = (AnimControl) child.getControl(AnimControl.class);
            if (animControl != null) {
                animControl.setEnabled(false);
            }
        }
    }

    /**
     * Restarts all animations that were playing
     *
     * @param spatial the root node of the creature
     */
    public static void resumeAnimations(Spatial spatial) {
        setAnimSpeeds((Node) spatial, 1.0f);
    }

    /**
     * Stops all animations that were playing
     *
     * @param spatial the root node of the creature
     */
    public static void pauseAnimations(Spatial spatial) {
        setAnimSpeeds((Node) spatial, 0.0f);
    }

    private static void setAnimSpeeds(Node node, float speed) {
        for (Spatial child : node.getChildren()) {
            AnimControl animControl = child.getControl(AnimControl.class);
            if (animControl != null) {
                animControl.getChannel(0).setSpeed(speed);
            }
        }
    }

}
