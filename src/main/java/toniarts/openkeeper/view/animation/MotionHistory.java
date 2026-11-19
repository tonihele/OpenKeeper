/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.animation;

import com.jme3.math.Transform;
import toniarts.openkeeper.game.component.CreatureMotion;
import toniarts.openkeeper.game.component.CreatureViewState;

/** Render-thread-owned bounded history; sampling allocates nothing and never extrapolates. */
public final class MotionHistory {
    private final CreatureMotion[] samples;
    private int count, first;
    private long lastSequence = Long.MIN_VALUE;
    private double sampledTime = Double.NaN;
    private CreatureViewState sampledRequest;

    public MotionHistory(int capacity) {
        if (capacity < 2 || capacity > 64) throw new IllegalArgumentException("History capacity must be 2..64");
        samples = new CreatureMotion[capacity];
    }

    public boolean add(CreatureMotion next) {
        if (next == null || next.sequence <= lastSequence || !Double.isFinite(next.time) || next.time < 0
                || !Double.isFinite(next.interval) || next.interval <= 0 || next.interval > 1
                || !Float.isFinite(next.x) || !Float.isFinite(next.y) || !Float.isFinite(next.z) || !Float.isFinite(next.yaw)
                || next.request == null || next.request.state == null || !Double.isFinite(next.request.stateStartTime)) return false;
        CreatureMotion last = latest();
        if (last != null && next.time <= last.time) return false;
        boolean cut = last != null && (next.epoch != last.epoch || next.immediate != last.immediate
                || next.request.creatureId != last.request.creatureId);
        if (cut) { clearSamples(); sampledTime = Double.NaN; }
        // Copy the wire component once; caller/deserializer mutation cannot corrupt history.
        CreatureMotion owned = new CreatureMotion();
        owned.time = next.time; owned.interval = next.interval; owned.epoch = next.epoch; owned.sequence = next.sequence;
        owned.x = next.x; owned.y = next.y; owned.z = next.z; owned.yaw = next.yaw; owned.immediate = next.immediate;
        owned.request = new CreatureViewState(next.request.creatureId, next.request.stateStartTime, next.request.state, next.request.sequence);
        if (count == samples.length) { samples[first] = null; first = (first + 1) % samples.length; count--; }
        samples[(first + count++) % samples.length] = owned;
        lastSequence = next.sequence;
        return true;
    }

    private CreatureMotion latest() { return count == 0 ? null : samples[(first + count - 1) % samples.length]; }
    public boolean isReady() { return count != 0; }
    public boolean isImmediate() { return count != 0 && latest().immediate; }
    public double getInterval() { return count == 0 ? 0 : latest().interval; }
    public long getEpoch() { return count == 0 ? Long.MIN_VALUE : latest().epoch; }

    public boolean sample(double desiredTime, Transform store) {
        if (count == 0 || !Double.isFinite(desiredTime)) return false;
        CreatureMotion newest = latest(), oldest = samples[first];
        double time = Math.clamp(desiredTime, oldest.time, newest.time);
        if (Double.isFinite(sampledTime)) time = Math.max(sampledTime, time);
        CreatureMotion a = oldest, b = oldest;
        for (int i = 1; i < count; i++) {
            b = samples[(first + i) % samples.length];
            if (b.time >= time) break;
            a = b;
        }
        double weight = b.time == a.time ? 0 : Math.clamp((time - a.time) / (b.time - a.time), 0, 1);
        store.getTranslation().set((float)(a.x + (b.x - a.x) * weight), (float)(a.y + (b.y - a.y) * weight), (float)(a.z + (b.z - a.z) * weight));
        double delta = Math.IEEEremainder((double)b.yaw - a.yaw, Math.PI * 2);
        store.getRotation().fromAngles(0, (float)(a.yaw + delta * weight), 0);
        sampledTime = time;
        sampledRequest = time >= b.time ? b.request : a.request;
        return true;
    }

    public double getSampledTime() { return sampledTime; }
    public CreatureViewState getSampledRequest() { return sampledRequest; }
    private void clearSamples() { java.util.Arrays.fill(samples, null); count = first = 0; sampledRequest = null; }
    public void clear() { clearSamples(); sampledTime = Double.NaN; lastSequence = Long.MIN_VALUE; }
}
