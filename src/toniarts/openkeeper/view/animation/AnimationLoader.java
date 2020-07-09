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
package toniarts.openkeeper.view.animation;

import com.jme3.anim.AnimComposer;
import static com.jme3.anim.AnimComposer.DEFAULT_LAYER;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.BaseAction;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.Anim;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Static helpers to handle animations in our basic scene objects
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class AnimationLoader {

    private static final String START_ANIMATION_NAME = "Start";
    private static final String END_ANIMATION_NAME = "End";
    private static final String ANIM_NAME = "anim";
    private static final String PLAY_ANIM_NAME = "animWithListeners";
    private static final AnimEventListener ANIM_EVENT_LISTENER = new AnimEventListener();

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
                    AnimComposer animControl = spatStart.getControl(AnimComposer.class);
                    if (animControl != null) {
                        animControl.setEnabled(false);
                        BaseAction myAction = new BaseAction(Tweens.sequence(animControl.action(ANIM_NAME), Tweens.callMethod(ANIM_EVENT_LISTENER, "onStartAnimationDone", animControl, root, resource)));
                        animControl.addAction(PLAY_ANIM_NAME, myAction);
                    }
                }
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                    String name = resource.getName() + END_ANIMATION_NAME;
                    Spatial spatEnd = loadModel(assetManager, name, root);
                    spatEnd.setName(END_ANIMATION_NAME);

                    // Create kinda a custom animation control
                    AnimComposer animControl = spatEnd.getControl(AnimComposer.class);
                    if (animControl != null) {
                        animControl.setEnabled(false);
                        BaseAction myAction = new BaseAction(Tweens.sequence(animControl.action(ANIM_NAME), Tweens.callMethod(ANIM_EVENT_LISTENER, "onEndAnimationDone", animControl, animationControl)));
                        animControl.addAction(PLAY_ANIM_NAME, myAction);
                    }
                }

                // Create kinda a custom animation control
                AnimComposer animControl = spat.getControl(AnimComposer.class);
                if (animControl != null) {
                    animControl.setEnabled(false);
                    BaseAction myAction = new BaseAction(Tweens.sequence(animControl.action(ANIM_NAME), Tweens.callMethod(ANIM_EVENT_LISTENER, "onAnimationDone", animControl, animationControl, root, resource, getLoopModeOnChannel(spat))));
                    animControl.addAction(PLAY_ANIM_NAME, myAction);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage());
            }
        }
    }

    public static LoopMode getLoopModeOnChannel(final Spatial spat) {
        final Anim.FrameFactorFunction func = Anim.FrameFactorFunction.valueOf(spat.getUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION));
        switch (func) {
            case CLAMP: {
                return LoopMode.Cycle;
            }
            case WRAP: {
                return LoopMode.Loop;
            }
        }

        return LoopMode.DontLoop;
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

        if(anim == null) {
            LOGGER.log(Level.WARNING, "Animation null!");
            return;
        }

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
                AnimComposer animControl = spat.getControl(AnimComposer.class);
                spat.setCullHint(Spatial.CullHint.Inherit);
                if (animControl != null) { // Not all are anims
                    animControl.setCurrentAction(PLAY_ANIM_NAME);
                    if (endFrame) {
                        animControl.setTime(DEFAULT_LAYER, Integer.MAX_VALUE);
                    }
                    animControl.setEnabled(true);
                }
            }
        } catch (Exception e) {
            // FIXME sometimes NPE in CreatureControl.java
            // line: playAnimation(creature.getAnimEntranceResource());
            LOGGER.log(Level.SEVERE, e, () -> "Creature animation playing error: " + e.toString());
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
        //UnitFlowerControl aufc = root.getControl(UnitFlowerControl.class);
        for (Spatial child : root.getChildren()) {

            // Don't hide the unit flower
            if (Boolean.FALSE.equals(child.getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))) {
                continue;
            }

            child.setCullHint(Spatial.CullHint.Always);

            // Also stop any animations
            AnimComposer animControl = child.getControl(AnimComposer.class);
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

        // This will reset all animations to go forward... Some cyclic animations may have been going backwards, but that is a small thing
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
            AnimComposer animControl = child.getControl(AnimComposer.class);
            if (animControl != null) {
                animControl.setGlobalSpeed(speed);
            }
        }
    }

    /**
     * Small event listener class to emulate the old AnimationEventListener from
     * jME
     */
    private static class AnimEventListener {

        public AnimEventListener() {
        }

        public void onStartAnimationDone(AnimComposer control, Node root, ArtResource resource) {

            // Start the real animation
            Spatial spat = root.getChild(resource.getName());
            AnimComposer animControl = spat.getControl(AnimComposer.class);
            spat.setCullHint(Spatial.CullHint.Inherit);
            animControl.setCurrentAction(PLAY_ANIM_NAME);
            animControl.setEnabled(true);

            // Hide us
            control.reset();
            control.setEnabled(false);
            control.getSpatial().setCullHint(Spatial.CullHint.Always);
        }

        public void onEndAnimationDone(AnimComposer control, AnimationControl animationControl) {

            // Stop us
            control.reset();
            control.setEnabled(false);

            // Signal stop
            animationControl.onAnimationStop();
        }

        public void onAnimationDone(AnimComposer control, AnimationControl animationControl, Node root, ArtResource resource, LoopMode loopMode) {

            // Signal that the main animation cycle is done
            animationControl.onAnimationCycleDone();

            // See if we need to stop
            if (animationControl.isStopAnimation() || loopMode == LoopMode.DontLoop) {

                // Stop us
                control.reset();
                control.setEnabled(false);

                // We need to stop
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {

                    // Hide us
                    control.getSpatial().setCullHint(Spatial.CullHint.Always);

                    Spatial spat = root.getChild(END_ANIMATION_NAME);
                    AnimComposer animControl = spat.getControl(AnimComposer.class);
                    spat.setCullHint(Spatial.CullHint.Inherit);
                    animControl.setCurrentAction(PLAY_ANIM_NAME);
                    animControl.setEnabled(true);
                } else {

                    // Signal stop
                    animationControl.onAnimationStop();
                }
            } else if (loopMode == LoopMode.Cycle) {

                // Change playing direction
                control.setGlobalSpeed(-control.getGlobalSpeed());
            }
        }

    }

}
