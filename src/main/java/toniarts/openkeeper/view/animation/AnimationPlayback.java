/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.util.clone.Cloner;
import java.util.function.DoubleSupplier;

/** One elapsed-time owner for start, repeated main and end; composers only sample. */
final class AnimationPlayback extends AbstractControl {
    static final String ACTION = "animWithListeners";
    private static final DoubleSupplier LOCAL_TIME = () -> Double.NaN;
    private Spatial[] models;
    private AnimComposer[] composers;
    private PlaybackAction[] actions;
    private LoopMode loopMode;
    private double age;
    private long traversals;
    private double exitAt = Double.NaN;
    private boolean hold, stopped, wasHidden;
    private int visibleSegment = -1;
    private transient DoubleSupplier clock = LOCAL_TIME;
    private double origin;
    private boolean clockInitialized;

    /** Runtime playback is not serialized; converted clips remain ordinary AnimComposers. */
    public AnimationPlayback() { setEnabled(false); }

    AnimationPlayback(Node root, String mainName) {
        models = new Spatial[]{root.getChild("Start"), root.getChild(mainName), root.getChild("End")};
        composers = new AnimComposer[3]; actions = new PlaybackAction[3];
        for (int i = 0; i < 3; i++) if (models[i] != null) {
            composers[i] = models[i].getControl(AnimComposer.class);
            if (composers[i] != null) {
                actions[i] = (PlaybackAction) composers[i].getAction(ACTION);
                composers[i].setEnabled(false);
            }
        }
        loopMode = actions[1] == null ? LoopMode.DontLoop : AnimationLoader.getLoopModeOnChannel(models[1]);
    }

    double age() { return age; }
    void holdCurrentPose() { stopped = true; }
    boolean isPlaying() { return !stopped && actions[1] != null; }
    double startLength() { return length(0); }
    private double length(int index) { return actions[index] == null ? 0 : actions[index].getLength(); }
    private AnimationControl control() { return spatial.getControl(AnimationControl.class); }
    private AnimationControl.PlaybackPolicy policy() {
        var control = control();
        return control == null ? AnimationControl.PlaybackPolicy.LOOP : control.getPlaybackPolicy();
    }
    private boolean current() { return spatial != null && spatial.getControl(AnimationPlayback.class) == this; }

    void bindClock(DoubleSupplier clock, double origin) {
        this.clock = clock; this.origin = origin; clockInitialized = false;
        double now = clock.getAsDouble();
        if (Double.isFinite(now)) { seek(Math.max(0, now - origin)); clockInitialized = true; }
    }

    /** Discontinuous/initial seeking samples state without replaying historical callbacks. */
    void seek(double time) {
        if (!Double.isFinite(time)) return;
        age = Math.max(0, time); stopped = hold = false; exitAt = Double.NaN;
        double length = length(1), mainAge = Math.max(0, age - length(0));
        traversals = length > 0 ? (long) Math.floor(mainAge / length) : 0;
        if (length > 0 && mainAge >= length
                && (policy() != AnimationControl.PlaybackPolicy.LOOP || loopMode == LoopMode.DontLoop)) {
            exitAt = length;
            hold = policy() == AnimationControl.PlaybackPolicy.HOLD;
            stopped = hold || mainAge >= exitAt + length(2);
        }
        renderPose();
    }

    @Override protected void controlUpdate(float tpf) {
        boolean hidden = AnimationLoader.isHidden(spatial);
        if (wasHidden && !hidden) AnimationLoader.refreshPendingPoses(spatial);
        wasHidden = hidden;
        if (AnimationLoader.isPaused(spatial) || actions[1] == null) return;
        double next;
        if (clock != LOCAL_TIME) {
            double now = clock.getAsDouble();
            if (!Double.isFinite(now)) return;
            if (!clockInitialized) { seek(Math.max(0, now - origin)); clockInitialized = true; return; }
            next = Math.max(age, now - origin);
        } else {
            if (!Float.isFinite(tpf) || tpf <= 0) return;
            // Keep the existing per-owner speed control; source ping-pong uses positive elapsed time.
            double speed = composers[1] == null ? 1 : composers[1].getGlobalSpeed();
            if (!Double.isFinite(speed) || speed <= 0) return;
            next = age + tpf * speed;
        }
        if (!Double.isFinite(next) || next == age || stopped) return;
        age = next;
        double length = length(1), mainAge = age - length(0);
        if (length > 0 && Double.isNaN(exitAt)) {
            long completed = (long) Math.floor(Math.max(0, mainAge) / length);
            while (traversals < completed) {
                sample(1, length, loopMode == LoopMode.Cycle && (traversals & 1) != 0);
                traversals++;
                var control = control();
                if (control != null) control.onAnimationCycleDone();
                if (!current()) return;
                var policy = policy();
                if (policy != AnimationControl.PlaybackPolicy.LOOP || loopMode == LoopMode.DontLoop) {
                    exitAt = traversals * length;
                    hold = policy == AnimationControl.PlaybackPolicy.HOLD;
                    break;
                }
            }
        }
        renderPose();
        if (!Double.isNaN(exitAt) && (hold || mainAge >= exitAt + length(2))) {
            stopped = true;
            var control = control();
            if (!hold && control != null) control.onAnimationStop();
        }
    }

    private void renderPose() {
        if (age < length(0)) { sample(0, age, false); return; }
        double length = length(1), mainAge = Math.max(0, age - length(0));
        if (length == 0) { show(1); return; }
        if (!Double.isNaN(exitAt)) {
            if (hold || actions[2] == null) sample(1, length, false);
            else sample(2, Math.min(mainAge - exitAt, length(2)), false);
            return;
        }
        long traversal = (long) Math.floor(mainAge / length);
        sample(1, mainAge % length, loopMode == LoopMode.Cycle && (traversal & 1) != 0);
    }

    private void show(int index) {
        if (visibleSegment == index) return;
        visibleSegment = index;
        for (int i = 0; i < 3; i++) if (models[i] != null) {
            models[i].setCullHint(i == index ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
            if (composers[i] != null) composers[i].setEnabled(false);
        }
    }

    private void sample(int index, double phase, boolean reversed) {
        show(index);
        var composer = composers[index];
        if (composer == null) return;
        if (composer.getCurrentAction() != actions[index]) composer.setCurrentAction(ACTION);
        composer.setTime(Math.min(Math.max(0,phase),Math.nextDown(length(index))));
        actions[index].setReversed(reversed);
        actions[index].interpolate(Math.max(0,phase));
    }

    @Override protected void controlRender(RenderManager rm, ViewPort vp) { }
    @Override public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner,original);
        models = cloner.clone(models); composers = cloner.clone(composers); actions = cloner.clone(actions);
        visibleSegment = -1;
        clock = LOCAL_TIME; clockInitialized = false;
    }
}
