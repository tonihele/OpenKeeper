/* Copyright (C) 2014-2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.function.DoubleSupplier;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.kmf.Anim;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.utils.AssetUtils;

/** Prepares owner-isolated visuals; AnimationPlayback owns their elapsed-time lifecycle. */
public final class AnimationLoader {
    private static final System.Logger LOGGER = System.getLogger(AnimationLoader.class.getName());
    private static final String MODEL_REVISION = "AnimationLoader.modelRevision";
    private static final String LAST_FAILURE = "AnimationLoader.lastFailure";
    private static final String PAUSED = "AnimationLoader.paused";
    private static final String WAS_ENABLED = "AnimationLoader.wasEnabled";

    public enum PlaybackResult { STARTED, UNAVAILABLE, FAILED }
    @FunctionalInterface public interface ModelSource {
        /** Return an owner-isolated model with resource decorations already applied. */
        Spatial load(String name, ArtResource resource);
    }
    private AnimationLoader() { }

    private static List<Spatial> prepareResource(ArtResource resource, ModelSource source) {
        List<Spatial> prepared = new ArrayList<>(3);
        if (resource.getType() != ArtResource.ArtResourceType.ANIMATING_MESH
                && resource.getType() != ArtResource.ArtResourceType.MESH
                && resource.getType() != ArtResource.ArtResourceType.PROCEDURAL_MESH) return prepared;
        prepared.add(loadModel(source,resource.getName(),resource,resource.getName(),true));
        if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_START_ANIMATION))
            prepared.add(loadModel(source,resource.getName()+"Start",resource,"Start",false));
        if (resource.getFlags().contains(ArtResource.ArtResourceFlag.HAS_END_ANIMATION))
            prepared.add(loadModel(source,resource.getName()+"End",resource,"End",false));
        return prepared;
    }

    private static Spatial loadModel(ModelSource source, String name, ArtResource resource, String nodeName, boolean main) {
        Spatial wrapper = source.load(name,resource);
        if (!(wrapper instanceof Node node) || node.getQuantity() == 0)
            throw new IllegalArgumentException("Empty model resource: "+name);
        Spatial model = node.getChild(0);
        var composer = model.getControl(AnimComposer.class);
        if (resource.getType() == ArtResource.ArtResourceType.ANIMATING_MESH && composer == null)
            throw new IllegalArgumentException("Missing animation composer: "+name);
        model.setName(nodeName); model.setCullHint(Spatial.CullHint.Always);
        if (composer != null) {
            composer.setEnabled(false);
            composer.addAction(AnimationPlayback.ACTION,new PlaybackAction(new TimedClip(composer,resource,main)));
        }
        return model;
    }

    public static LoopMode getLoopModeOnChannel(Spatial spatial) {
        return Anim.FrameFactorFunction.valueOf(spatial.getUserData(KmfModelLoader.FRAME_FACTOR_FUNCTION))
                == Anim.FrameFactorFunction.CLAMP ? LoopMode.Cycle : LoopMode.Loop;
    }

    public static PlaybackResult playAnimation(Spatial spatial, ArtResource art, AssetManager assets) {
        return playAnimation(spatial,art,assets,false);
    }
    public static PlaybackResult playAnimation(Spatial spatial, ArtResource art, AssetManager assets, boolean endFrame) {
        return playAnimation(spatial,art,(name,resource)->AssetUtils.loadModel(assets,name,resource),endFrame);
    }
    public static PlaybackResult playAnimation(Spatial spatial, ArtResource art, ModelSource source, boolean endFrame) {
        if (!(spatial instanceof Node root) || art == null || art.getType() == ArtResource.ArtResourceType.NONE)
            return PlaybackResult.UNAVAILABLE;
        var cache = root.getControl(PreparedAnimationCache.class);
        boolean retained = false;
        try {
            List<Spatial> prepared = cache == null ? null : cache.get(art);
            // An active entry cannot be sampled transactionally without changing its live buffers.
            // Reuse inactive entries; active restarts prepare an isolated replacement.
            if (prepared != null) for (Spatial child : prepared) if (child.getParent() == root) { prepared = null; break; }
            retained = prepared != null;
            if (prepared == null) {
                prepared = prepareResource(art,source);
            } else for (Spatial child : prepared) child.depthFirstTraversal(node -> {
                node.setUserData(WAS_ENABLED,null);
                var composer = node.getControl(AnimComposer.class);
                if (composer != null) {
                    composer.reset(); composer.setEnabled(false); composer.setGlobalSpeed(1);
                }
            });
            if (prepared.isEmpty()) return PlaybackResult.UNAVAILABLE;
            Node staging = new Node("Animation preparation");
            for (Spatial child : prepared) staging.attachChild(child);
            var playback = new AnimationPlayback(prepared,art);
            var control = root.getControl(AnimationControl.class);
            playback.prepare(endFrame ? Double.MAX_VALUE : 0,control == null
                    ? AnimationControl.PlaybackPolicy.LOOP : control.getPlaybackPolicy());

            var oldChildren = List.copyOf(root.getChildren());
            var oldPlayback = root.getControl(AnimationPlayback.class);
            var oldData = new HashMap<String,Object>();
            for (String key : root.getUserDataKeys()) oldData.put(key,root.getUserData(key));
            try {
                for (Spatial child : oldChildren)
                    if (!Boolean.FALSE.equals(child.getUserData(AssetUtils.USER_DATA_KEY_REMOVABLE))) child.removeFromParent();
                for (Spatial child : prepared) root.attachChild(child);
                root.removeControl(AnimationPlayback.class);
                root.addControlAt(0,playback);
                if (control != null) control.onAnimationAttached();
                if (cache != null) cache.put(art,prepared);
                root.setUserData(MODEL_REVISION,getModelRevision(root)+1);
            } catch (Exception failure) {
                root.removeControl(playback);
                root.detachAllChildren();
                for (Spatial child : oldChildren) root.attachChild(child);
                if (oldPlayback != null) root.addControlAt(0,oldPlayback);
                for (String key : List.copyOf(root.getUserDataKeys())) root.setUserData(key,null);
                oldData.forEach(root::setUserData);
                throw failure;
            }
            root.setUserData(LAST_FAILURE,null);
            return PlaybackResult.STARTED;
        } catch (Exception exception) {
            // A failed inactive entry must not poison future preparation attempts.
            if (cache != null && retained) cache.remove(art);
            if (!art.getName().equals(root.getUserData(LAST_FAILURE))) {
                LOGGER.log(Level.WARNING,"Could not prepare animation "+art.getName(),exception);
                root.setUserData(LAST_FAILURE,art.getName());
            }
            return PlaybackResult.FAILED;
        }
    }

    public static int getModelRevision(Spatial spatial) {
        Integer revision = spatial.getUserData(MODEL_REVISION);
        return revision == null ? 0 : revision;
    }
    /** Initial/discontinuous seeks never replay historical callbacks. */
    public static void seekAnimation(Spatial spatial, ArtResource resource, double age) {
        var playback = spatial.getControl(AnimationPlayback.class);
        if (playback != null && resource != null) playback.seek(age);
    }
    /** World poses and markers consume the same bounded, pause-aware presentation time. */
    public static void bindPlaybackClock(Spatial spatial, DoubleSupplier clock, double startTime) {
        var playback = spatial.getControl(AnimationPlayback.class);
        if (playback != null) playback.bindClock(clock,startTime);
    }
    public static double getPlaybackAge(Spatial spatial) {
        var playback = spatial.getControl(AnimationPlayback.class);
        return playback == null ? Double.NaN : playback.age();
    }
    /** Retains the last valid pose when a logical state has no renderable replacement. */
    public static void holdAnimation(Spatial spatial) {
        var playback = spatial.getControl(AnimationPlayback.class);
        if (playback != null) playback.holdCurrentPose();
    }
    public static double getMainStartDelay(Spatial spatial) {
        var playback = spatial.getControl(AnimationPlayback.class);
        return playback == null ? 0 : playback.startLength();
    }
    public static boolean isPaused(Spatial spatial) {
        for (Spatial node = spatial; node != null; node = node.getParent())
            if (Boolean.TRUE.equals(node.getUserData(PAUSED))) return true;
        return false;
    }
    public static boolean isHidden(Spatial spatial) {
        for (Spatial node = spatial; node != null; node = node.getParent())
            if (node.getCullHint() == Spatial.CullHint.Always) return true;
        return false;
    }
    public static void refreshPendingPoses(Spatial root) {
        root.depthFirstTraversal(node -> {
            var composer = node.getControl(AnimComposer.class);
            if (composer != null) for (var clip : composer.getAnimClips()) for (var track : clip.getTracks())
                if (track instanceof toniarts.openkeeper.animation.PoseTrack pose) pose.refreshPendingPose();
        });
    }
    public static void pauseAnimations(Spatial spatial) {
        spatial.setUserData(PAUSED,true);
        spatial.depthFirstTraversal(node -> {
            var composer = node.getControl(AnimComposer.class);
            if (composer != null && node.getUserData(WAS_ENABLED) == null) {
                node.setUserData(WAS_ENABLED,composer.isEnabled()); composer.setEnabled(false);
            }
        });
    }
    public static void resumeAnimations(Spatial spatial) {
        spatial.depthFirstTraversal(node -> {
            var composer = node.getControl(AnimComposer.class);
            Boolean enabled = node.getUserData(WAS_ENABLED);
            if (composer != null && enabled != null) composer.setEnabled(enabled);
            node.setUserData(WAS_ENABLED,null); node.setUserData(PAUSED,null);
        });
    }
}
