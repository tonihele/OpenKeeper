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
import java.util.ArrayList;
import java.util.List;
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
    private static final String MODEL_REVISION = "AnimationLoader.modelRevision";
    private static final String LAST_FAILURE = "AnimationLoader.lastFailure";
    private static final String PAUSED = "AnimationLoader.paused";
    private static final String WAS_ENABLED = "AnimationLoader.wasEnabled";

    public enum PlaybackResult { STARTED, UNAVAILABLE, FAILED }

    @FunctionalInterface
    interface ModelSource {
        Spatial load(String name, ArtResource resource);
    }

    private AnimationLoader() {
        // No!
    }

    private static List<Spatial> prepareResource(final Node root, final ArtResource resource, ModelSource source) {
        List<Spatial> prepared = new ArrayList<>(3);
        if (resource != null && (resource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH
                || resource.getType() == ArtResource.ArtResourceType.MESH
                || resource.getType() == ArtResource.ArtResourceType.PROCEDURAL_MESH)) {

                Spatial spat = loadModel(source, resource.getName(), resource);
                prepared.add(spat);
                spat.setName(resource.getName());

                // If the animations has end and/or start, it is located in a different file
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION)) {
                    String name = resource.getName() + START_ANIM_NODE_NAME;
                    Spatial spatStart = loadModel(source, name, resource);
                    prepared.add(spatStart);
                    spatStart.setName(START_ANIM_NODE_NAME);

                    // Create kinda a custom animation control
                    var animComposer = spatStart.getControl(AnimComposer.class);
                    if (animComposer != null) {
                        animComposer.setEnabled(false);
                        animComposer.actionSequence(ANIM_WITH_LISTENERS_NAME, new TimedClip(animComposer, resource, false), Tweens.callMethod(ANIM_EVENT_LISTENER, "onStartAnimationDone", animComposer, root, resource));
                    }
                }
                if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION)) {
                    String name = resource.getName() + END_ANIM_NODE_NAME;
                    Spatial spatEnd = loadModel(source, name, resource);
                    prepared.add(spatEnd);
                    spatEnd.setName(END_ANIM_NODE_NAME);

                    // Create kinda a custom animation control
                    var animComposer = spatEnd.getControl(AnimComposer.class);
                    if (animComposer != null) {
                        animComposer.setEnabled(false);
                        animComposer.actionSequence(ANIM_WITH_LISTENERS_NAME, new TimedClip(animComposer, resource, false), Tweens.callMethod(ANIM_EVENT_LISTENER, "onEndAnimationDone", animComposer, root));
                    }
                }

                // Create kinda a custom animation control
                var animComposer = spat.getControl(AnimComposer.class);
                if (animComposer != null) {
                    animComposer.setEnabled(false);
                    animComposer.addAction(ANIM_WITH_LISTENERS_NAME, new PlaybackAction(
                            new TimedClip(animComposer, resource, true),
                            Tweens.callMethod(ANIM_EVENT_LISTENER, "onAnimationDone", animComposer, root, resource, getLoopModeOnChannel(spat))));
                }
        }
        return prepared;
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

    private static Spatial loadModel(ModelSource source, String resourceName, ArtResource artResource) {

        // Load the model and attach it without the root
        Spatial model = source.load(resourceName, artResource);
        if (!(model instanceof Node node) || node.getQuantity() == 0) {
            throw new IllegalArgumentException("Empty model resource: " + resourceName);
        }
        model = ((Node) model).getChild(0);
        if (artResource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH
                && model.getControl(AnimComposer.class) == null) {
            throw new IllegalArgumentException("Missing animation composer: " + resourceName);
        }
        model.setCullHint(Spatial.CullHint.Always);
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
    public static PlaybackResult playAnimation(Spatial spatial, ArtResource anim, AssetManager assetManager, boolean endFrame) {
        return playAnimation(spatial, anim, (name, resource) -> AssetUtils.loadModel(assetManager, name, resource), endFrame);
    }

    static PlaybackResult playAnimation(Spatial spatial, ArtResource anim, ModelSource source, boolean endFrame) {
        if (!(spatial instanceof Node root) || anim == null || anim.getType() == ArtResource.ArtResourceType.NONE) {
            return PlaybackResult.UNAVAILABLE;
        }
        try {
            List<Spatial> prepared = prepareResource(root, anim, source);
            if (prepared.isEmpty()) return PlaybackResult.UNAVAILABLE;

        // Attach the anim node and get rid of the rest
        for (Spatial child : root.getChildren()) {
            if (Boolean.FALSE.equals(child.getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))) {
                continue;
            }
            child.removeFromParent();
        }
            for (Spatial child : prepared) root.attachChild(child);
            root.setUserData(MODEL_REVISION, getModelRevision(root) + 1);
            root.setUserData(LAST_FAILURE, null);

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
                    animComposer.setEnabled(true);
                }
            }
            if (endFrame) seekAnimation(root, anim, Double.MAX_VALUE);
            if (isPaused(root)) pauseAnimations(root);
            return PlaybackResult.STARTED;
        } catch (Exception e) {
            if (!anim.getName().equals(root.getUserData(LAST_FAILURE))) {
                logger.log(Level.WARNING, "Could not prepare animation " + anim.getName(), e);
                root.setUserData(LAST_FAILURE, anim.getName());
            }
            return PlaybackResult.FAILED;
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
    public static PlaybackResult playAnimation(Spatial spatial, ArtResource anim, AssetManager assetManager) {
        return playAnimation(spatial, anim, assetManager, false);
    }

    public static int getModelRevision(Spatial spatial) {
        Integer revision = spatial.getUserData(MODEL_REVISION);
        return revision == null ? 0 : revision;
    }

    /** Seeks a newly received request without replaying historical gameplay events. */
    public static void seekAnimation(Spatial spatial, ArtResource resource, double age) {
        if (!(spatial instanceof Node root) || resource == null || !Double.isFinite(age)) return;
        age = Math.max(0, age);
        int revision = getModelRevision(root);
        Spatial start = root.getChild(START_ANIM_NODE_NAME);
        if (start != null && start.getControl(AnimComposer.class) != null) {
            var composer = start.getControl(AnimComposer.class);
            double length = composer.getAction(ANIM_WITH_LISTENERS_NAME).getLength();
            if (age < length) { sampleAt(composer, age); return; }
            sampleAt(composer, length);
            if (revision != getModelRevision(root)) return;
            age -= length;
        }
        Spatial main = root.getChild(resource.getName());
        if (main == null || main.getControl(AnimComposer.class) == null) return;
        var composer = main.getControl(AnimComposer.class);
        double length = composer.getAction(ANIM_WITH_LISTENERS_NAME).getLength();
        if (length <= 0) return;
        var control = root.getControl(AnimationControl.class);
        boolean once = control != null && (control.isStopAnimation() || control.isHoldLastFrame());
        PlaybackAction playback = (PlaybackAction) composer.getAction(ANIM_WITH_LISTENERS_NAME);
        playback.setReversed(false);
        if (once && age >= length) {
            sampleAt(composer, length);
            if (revision != getModelRevision(root)) return;
            Spatial end = root.getChild(END_ANIM_NODE_NAME);
            if (end != null && end.getControl(AnimComposer.class) != null) {
                var endComposer = end.getControl(AnimComposer.class);
                sampleAt(endComposer, Math.min(age - length, endComposer.getAction(ANIM_WITH_LISTENERS_NAME).getLength()));
            }
        } else if (getLoopModeOnChannel(main) == LoopMode.Cycle && !once) {
            double phase = age % (length * 2);
            playback.setReversed(phase >= length);
            sampleAt(composer, phase >= length ? phase - length : phase);
        } else sampleAt(composer, once ? age : age % length);
    }

    private static void sampleAt(AnimComposer composer, double phase) {
        if (composer.getCurrentAction() == null) composer.setCurrentAction(ANIM_WITH_LISTENERS_NAME);
        var action = composer.getCurrentAction();
        if (action.getLength() <= 0) return;
        composer.setTime(Math.min(phase, Math.nextDown(action.getLength())));
        action.interpolate(phase);
    }

    private static boolean isPaused(Spatial spatial) {
        for (Spatial node = spatial; node != null; node = node.getParent())
            if (Boolean.TRUE.equals(node.getUserData(PAUSED))) return true;
        return false;
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
        spatial.depthFirstTraversal(node -> {
            var composer = node.getControl(AnimComposer.class);
            Boolean enabled = node.getUserData(WAS_ENABLED);
            if (composer != null && enabled != null) composer.setEnabled(enabled);
            node.setUserData(WAS_ENABLED, null);
            node.setUserData(PAUSED, null);
        });
    }

    /**
     * Stops all animations that were playing
     *
     * @param spatial the root node of the creature
     */
    public static void pauseAnimations(Spatial spatial) {
        spatial.setUserData(PAUSED, true);
        spatial.depthFirstTraversal(node -> {
            var composer = node.getControl(AnimComposer.class);
            if (composer != null && node.getUserData(WAS_ENABLED) == null) {
                node.setUserData(WAS_ENABLED, composer.isEnabled());
                composer.setEnabled(false);
            }
        });
    }

    /**
     * Small event listener class to emulate the old AnimationEventListener from
     * jME
     */
    private static final class AnimEventListener {

        public AnimEventListener() {
        }

        public void onStartAnimationDone(AnimComposer composer, Node root, ArtResource resource) {
            if (composer.getSpatial().getParent() != root) return;

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

        public void onEndAnimationDone(AnimComposer composer, Node root) {
            if (composer.getSpatial().getParent() != root) return;
            var animationControl = root.getControl(AnimationControl.class);

            // Stop us
            composer.reset();
            composer.setEnabled(false);

            // Signal stop
            if (animationControl != null) animationControl.onAnimationStop();
        }

        public void onAnimationDone(AnimComposer composer, Node root, ArtResource resource, LoopMode loopMode) {
            if (composer.getSpatial().getParent() != root) return;
            var animationControl = root.getControl(AnimationControl.class);

            // Signal that the main animation cycle is done
            if (animationControl != null) animationControl.onAnimationCycleDone();
            if (composer.getSpatial().getParent() != root) return;

            // An indicator stays where its animation took it
            if (animationControl != null && animationControl.isHoldLastFrame()) {

                // The pose stays where the animation left it while the composer is off, and the next animation
                // turns its composer on again
                composer.setEnabled(false);
                return;
            }

            // See if we need to stop
            if ((animationControl != null && animationControl.isStopAnimation()) || loopMode == LoopMode.DontLoop) {

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
                    if (animationControl != null) animationControl.onAnimationStop();
                }
            } else if (loopMode == LoopMode.Cycle) {

                // AnimLayer wraps negative time without completing another cycle.
                // Keep its clock forward and reverse only source-pose traversal.
                ((PlaybackAction) composer.getAction(ANIM_WITH_LISTENERS_NAME)).reverse();
            }
        }

    }

}
