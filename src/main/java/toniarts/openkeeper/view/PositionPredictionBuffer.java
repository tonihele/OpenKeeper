/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.logic.MovementSettings;

/**
 * Small client-side history for rendering a creature between authoritative
 * position updates. The buffer is render-thread owned and never writes back to
 * entity data.
 */
final class PositionPredictionBuffer {

    private static final int MAX_SAMPLES = 16;
    // MAX_SAMPLES stays a power of two so the circular indexes can use a mask.
    private static final int SAMPLE_MASK = MAX_SAMPLES - 1;
    private static final float TWO_PI = (float) (Math.PI * 2d);
    private static final float PI = (float) Math.PI;

    private final MovementSettings settings;
    private final Sample[] samples = new Sample[MAX_SAMPLES];
    private int sampleHead;
    private int sampleCount;

    PositionPredictionBuffer(MovementSettings settings) {
        this.settings = settings;
    }

    /**
     * Adds a received authoritative state. Repeated positions remain in the
     * bounded history so a stopped creature also resets its extrapolated
     * velocity. A large correction starts a new history immediately.
     *
     * @return {@code true} if the sample changed the history
     */
    boolean addSample(double time, Position position) {
        if (!Double.isFinite(time) || position == null || position.position == null
                || !Float.isFinite(position.position.x) || !Float.isFinite(position.position.y)
                || !Float.isFinite(position.position.z) || !Float.isFinite(position.rotation)) {
            return false;
        }
        float x = position.position.x;
        float y = position.position.y;
        float z = position.position.z;
        if (sampleCount == 0) {
            append(time, x, y, z, position.rotation);
            return true;
        }
        Sample last = samples[index(sampleCount - 1)];
        if (time < last.time) {
            return false;
        }

        float dx = x - last.x;
        float dy = y - last.y;
        float dz = z - last.z;
        float distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > settings.teleportDistance() * settings.teleportDistance()) {
            sampleHead = 0;
            sampleCount = 0;
        } else if (time == last.time) {
            sampleCount--;
        }
        append(time, x, y, z, position.rotation);
        return true;
    }

    private void append(double time, float x, float y, float z, float rotation) {
        if (sampleCount == MAX_SAMPLES) {
            sampleHead = (sampleHead + 1) & SAMPLE_MASK;
            sampleCount--;
        }
        int index = index(sampleCount);
        Sample sample = samples[index];
        if (sample == null) {
            sample = new Sample();
            samples[index] = sample;
        }
        sample.set(time, x, y, z, rotation);
        sampleCount++;
    }

    boolean hasHistory() {
        return sampleCount > 1;
    }

    /**
     * Samples delayed history or projects the last measured velocity by at
     * most the configured extrapolation window. Writes the position into the
     * supplied vector and returns the facing angle.
     */
    float sample(double time, Vector3f store) {
        if (sampleCount == 0) {
            store.set(Vector3f.ZERO);
            return 0f;
        }

        Sample latest = samples[index(sampleCount - 1)];
        if (Double.isNaN(time)) {
            return write(latest, store);
        }
        Sample after = latest;
        if (time > latest.time) {
            Sample previous = sampleCount > 1 ? samples[index(sampleCount - 2)] : null;
            return extrapolate(time, latest, previous, store);
        }
        if (time == latest.time) {
            return write(latest, store);
        }

        for (int offset = sampleCount - 2; offset >= 0; offset--) {
            Sample before = samples[index(offset)];
            if (time >= before.time) {
                double span = after.time - before.time;
                float alpha = span <= 0d ? 1f : clamp((float) ((time - before.time) / span), 0f, 1f);
                store.set(lerp(before.x, after.x, alpha), lerp(before.y, after.y, alpha),
                        lerp(before.z, after.z, alpha));
                return before.rotation + angleDifference(after.rotation, before.rotation) * alpha;
            }
            after = before;
        }
        return write(after, store);
    }

    private int index(int offset) {
        return (sampleHead + offset) & SAMPLE_MASK;
    }

    private float extrapolate(double time, Sample latest, Sample previous, Vector3f store) {
        if (previous == null || settings.maxExtrapolation() <= 0d) {
            return write(latest, store);
        }

        double sampleSpan = latest.time - previous.time;
        double extrapolation = Math.min(time - latest.time, settings.maxExtrapolation());
        if (sampleSpan <= 0d || extrapolation <= 0d) {
            return write(latest, store);
        }

        float scale = (float) (extrapolation / sampleSpan);
        float projectedX = (latest.x - previous.x) * scale;
        float projectedY = (latest.y - previous.y) * scale;
        float projectedZ = (latest.z - previous.z) * scale;
        float projectedDistance = (float) Math.sqrt(projectedX * projectedX + projectedY * projectedY + projectedZ * projectedZ);
        if (projectedDistance > settings.teleportDistance()) {
            float correction = settings.teleportDistance() / projectedDistance;
            projectedX *= correction;
            projectedY *= correction;
            projectedZ *= correction;
        }
        store.set(latest.x + projectedX, latest.y + projectedY, latest.z + projectedZ);

        float rotationChange = angleDifference(latest.rotation, previous.rotation) * scale;
        return latest.rotation + clamp(rotationChange, -PI, PI);
    }

    private static float write(Sample sample, Vector3f store) {
        store.set(sample.x, sample.y, sample.z);
        return sample.rotation;
    }

    private static float angleDifference(float target, float current) {
        float difference = (target - current) % TWO_PI;
        if (difference > PI) {
            difference -= TWO_PI;
        } else if (difference < -PI) {
            difference += TWO_PI;
        }
        return difference;
    }

    private static float lerp(float from, float to, float alpha) {
        return from + (to - from) * alpha;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Sample {

        private double time;
        private float x;
        private float y;
        private float z;
        private float rotation;

        private void set(double time, float x, float y, float z, float rotation) {
            this.time = time;
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotation = rotation;
        }
    }
}
