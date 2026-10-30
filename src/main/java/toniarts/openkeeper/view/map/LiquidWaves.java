/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;

/** Native liquid height-field calculation, in OpenKeeper's y-up coordinates. */
public final class LiquidWaves {
    public static final float LEVEL = 0.85f;
    public static final int SUBDIVISIONS = 2;
    // DKII advances an integer counter per rendered update. Use a stable reference
    // rate here instead of making animation speed depend on the current frame rate.
    public static final double TICKS_PER_SECOND = 30;

    private static final long START_NANOS = System.nanoTime();

    private LiquidWaves() { }

    /** Shared across rebuilt surfaces, so revealing another tile never restarts waves. */
    public static double tick() {
        return ((System.nanoTime() - START_NANOS) * 1e-9 * TICKS_PER_SECOND) % 128;
    }

    public static double phase(double coordinate, double tick, boolean water) {
        double units = coordinate * (water ? 16 : 64) + tick;
        units -= Math.floor(units / 128) * 128;
        return units * (float) Math.PI / 64;
    }

    public static float height(double x, double z, double tick, boolean water) {
        return LEVEL - (float) (.04 * (Math.sin(phase(x, tick, water)) + Math.sin(phase(z, tick, water))));
    }

    public static void normal(double x, double z, double tick, boolean water, Vector3f result) {
        result.set((float) Math.cos(phase(x, tick, water)), 3,
                (float) Math.cos(phase(z, tick, water))).normalizeLocal();
    }
}
