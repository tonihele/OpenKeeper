/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.AbstractTween;
import com.jme3.anim.tween.action.Action;
import com.jme3.util.clone.Cloner;
import toniarts.openkeeper.animation.AnimationTiming;
import toniarts.openkeeper.animation.PoseTrack;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.ArtResource;

/** Maps authored FPS onto historical 30 Hz J3O keys, including the final frame interval. */
final class TimedClip extends AbstractTween {
    private Action action;
    private final double sourceLength;
    private final double rate;
    private boolean reversed;

    TimedClip(AnimComposer composer, ArtResource resource, boolean mainSegment) {
        super(duration(composer, resource, mainSegment));
        action = composer.action(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
        sourceLength = action.getLength();
        rate = AnimationTiming.framesPerSecond(resource) / AnimationTiming.SOURCE_FPS;
    }

    private static double duration(AnimComposer composer, ArtResource resource, boolean mainSegment) {
        var clip = composer.getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
        if (clip == null) throw new IllegalArgumentException("Missing converted animation clip");
        int frames = 1;
        for (var track : clip.getTracks()) if (track instanceof PoseTrack pose) frames = Math.max(frames, pose.getTimes().length);
        double fallback = frames / AnimationTiming.framesPerSecond(resource);
        return mainSegment ? AnimationTiming.duration(resource, fallback) : fallback;
    }

    @Override protected void doInterpolate(double fraction) {
        if (reversed) fraction = 1 - fraction;
        action.interpolate(Math.min(sourceLength, fraction * getLength() * rate));
    }

    void setReversed(boolean reversed) { this.reversed = reversed; }

    @Override public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        action = cloner.clone(action);
    }
}
