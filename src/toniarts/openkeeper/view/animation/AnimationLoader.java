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
import com.jme3.anim.tween.Tweens;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.Anim;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;

/**
 * Static helpers to handle animations in our basic scene objects
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class AnimationLoader {

    private static final Logger logger = System.getLogger(AnimationLoader.class.getName());

    private static final String START_ANIM_NODE_NAME = "Start";
    private static final String END_ANIM_NODE_NAME = "End";
    private static final String ANIM_WITH_LISTENERS_NAME = "animWithListeners";
    private static final AnimEventListener ANIM_EVENT_LISTENER = new AnimEventListener();

    private AnimationLoader() {
        // No!
    }

    private static void attachResource(final Node root, final AnimationControl animationControl, final ArtResource resource, AssetManager assetManager) {
        if (resource != null && (resource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH
                || resource.getType() == ArtResource.ArtResourceType.MESH
                || resource.getType() == ArtResource.ArtResourceType.PROCEDURAL_MESH)) {
            try {

                Spatial spat = loadModel(assetManager, resource.getName(), resource, root);
                spat.setName(resource.getName());

                // If the animations has end and/or start, it is located in a different file
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                    String name = resource.getName() + START_ANIM_NODE_NAME;
                    Spatial spatStart = loadModel(assetManager, name, resource, root);
                    spatStart.setName(START_ANIM_NODE_NAME);

                    // Create kinda a custom animation control
                    var animComposer = spatStart.getControl(AnimComposer.class);
                    if (animComposer != null) {
                        animComposer.setEnabled(false);
                        animComposer.actionSequence(ANIM_WITH_LISTENERS_NAME, animComposer.action(KmfModelLoader.DUMMY_ANIM_CLIP_NAME), Tweens.callMethod(ANIM_EVENT_LISTENER, "onStartAnimationDone", animComposer, root, resource));
                    }
                }
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                    String name = resource.getName() + END_ANIM_NODE_NAME;
                    Spatial spatEnd = loadModel(assetManager, name, resource, root);
                    spatEnd.setName(END_ANIM_NODE_NAME);

                    // Create kinda a custom animation control
                    var animComposer = spatEnd.getControl(AnimComposer.class);
                    if (animComposer != null) {
                        animComposer.setEnabled(false);
                        animComposer.actionSequence(ANIM_WITH_LISTENERS_NAME, animComposer.action(KmfModelLoader.DUMMY_ANIM_CLIP_NAME), Tweens.callMethod(ANIM_EVENT_LISTENER, "onEndAnimationDone", animComposer, animationControl));
                    }
                }

                // Create kinda a custom animation control
                var animComposer = spat.getControl(AnimComposer.class);
                if (animComposer != null) {
                    animComposer.setEnabled(false);
                    animComposer.actionSequence(ANIM_WITH_LISTENERS_NAME, animComposer.action(KmfModelLoader.DUMMY_ANIM_CLIP_NAME), Tweens.callMethod(ANIM_EVENT_LISTENER, "onAnimationDone", animComposer, animationControl, root, resource, getLoopModeOnChannel(spat)));
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage());
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

    private static Spatial loadModel(AssetManager assetManager, String resourceName, ArtResource artResource, Node creatureRoot) {

        // Load the model and attach it without the root
        Spatial model = AssetUtils.loadModel(assetManager, resourceName, artResource);
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
            logger.log(Level.WARNING, "Animation null!");
            return;
        }

        try {

            // Get the anim node
            String animNodeName = anim.getName();
            if (anim.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                animNodeName = START_ANIM_NODE_NAME;
            }
            Spatial spat = root.getChild(animNodeName);
            if (spat != null) {

                // Hide all
                hideAllNodes(root);

                // Show the anim
                var animComposer = spat.getControl(AnimComposer.class);
                spat.setCullHint(Spatial.CullHint.Inherit);
                if (animComposer != null) { // Not all are anims
                    animComposer.setCurrentAction(ANIM_WITH_LISTENERS_NAME);
                    if (endFrame) {
                        animComposer.setTime(Integer.MAX_VALUE);
                    }
                    animComposer.setEnabled(true);
                }
            }
        } catch (Exception e) {
            // FIXME sometimes NPE in CreatureControl.java
            // line: playAnimation(creature.getAnimEntranceResource());
            logger.log(Level.ERROR, () -> "Creature animation playing error: " + e.toString(), e);
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
            var animComposer = child.getControl(AnimComposer.class);
            if (animComposer != null) {
                animComposer.setEnabled(false);
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
            var animComposer = child.getControl(AnimComposer.class);
            if (animComposer != null) {
                animComposer.setGlobalSpeed(speed);
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

        public void onStartAnimationDone(AnimComposer composer, Node root, ArtResource resource) {

            // Start the real animation
            var mainAnimSpat = root.getChild(resource.getName());
            var mainAnimComposer = mainAnimSpat.getControl(AnimComposer.class);
            mainAnimSpat.setCullHint(Spatial.CullHint.Inherit);
            mainAnimComposer.setCurrentAction(ANIM_WITH_LISTENERS_NAME);
            mainAnimComposer.setEnabled(true);

            // Hide us
            composer.reset();
            composer.setEnabled(false);
            composer.getSpatial().setCullHint(Spatial.CullHint.Always);
        }

        public void onEndAnimationDone(AnimComposer composer, AnimationControl animationControl) {

            // Stop us
            composer.reset();
            composer.setEnabled(false);

            // Signal stop
            animationControl.onAnimationStop();
        }

        public void onAnimationDone(AnimComposer composer, AnimationControl animationControl, Node root, ArtResource resource, LoopMode loopMode) {

            // Signal that the main animation cycle is done
            animationControl.onAnimationCycleDone();

            // See if we need to stop
            if (animationControl.isStopAnimation() || loopMode == LoopMode.DontLoop) {

                // Stop us
                composer.reset();
                composer.setEnabled(false);

                // We need to stop
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {

                    // Hide us
                    composer.getSpatial().setCullHint(Spatial.CullHint.Always);

                    var endAnimSpat = root.getChild(END_ANIM_NODE_NAME);
                    var endAnimComposer = endAnimSpat.getControl(AnimComposer.class);
                    endAnimSpat.setCullHint(Spatial.CullHint.Inherit);
                    endAnimComposer.setCurrentAction(ANIM_WITH_LISTENERS_NAME);
                    endAnimComposer.setEnabled(true);
                } else {

                    // Signal stop
                    animationControl.onAnimationStop();
                }
            } else if (loopMode == LoopMode.Cycle) {

                // Change playing direction
                composer.setGlobalSpeed(-composer.getGlobalSpeed());
            }
        }

    }

}
