/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.BaseAction;
import com.jme3.util.clone.Cloner;

/** Owns traversal direction without relying on negative AnimLayer time wrapping. */
final class PlaybackAction extends BaseAction {
    private TimedClip clip;

    PlaybackAction(TimedClip clip, Tween complete) {
        super(Tweens.sequence(clip, complete));
        this.clip = clip;
    }

    void setReversed(boolean reversed) { clip.setReversed(reversed); }
    void reverse() { clip.reverse(); }

    @Override public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        clip = cloner.clone(clip);
    }
}
