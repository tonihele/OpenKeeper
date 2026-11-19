/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.tween.action.Action;
import com.jme3.util.clone.Cloner;

/** A sampleable segment. Completion and elapsed time belong to the owner driver. */
final class PlaybackAction extends Action {
    private TimedClip clip;

    PlaybackAction(TimedClip clip) {
        super();
        this.clip = clip;
        setLength(clip.getLength());
    }

    @Override public boolean interpolate(double time) { return clip.interpolate(time); }

    void setReversed(boolean reversed) { clip.setReversed(reversed); }
    @Override public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        clip = cloner.clone(clip);
    }
}
