/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import toniarts.openkeeper.utils.WorldUtils;

/** CPU reference for the liquid wave profiles evaluated by the shader. */
public final class LiquidWaves {

    public static final float LEVEL = WorldUtils.WATER_LEVEL;
    public static final float AMPLITUDE = 0.04f;
    public static final double TICKS_PER_SECOND = 30.0;
    public static final double TICK_PERIOD = 128.0;
    // Water-specific scale follows PR #852; native water samples still need validation.
    public static final float WATER_SPATIAL_FREQUENCY = 16f;
    public static final float LAVA_SPATIAL_FREQUENCY = 64f;
    private static final double PHASE_SCALE = Math.PI / 64.0;

    private LiquidWaves() {
    }

    /** Configured spatial frequency in map-grid units for this liquid. */
    public static float frequency(boolean water) {
        return water ? WATER_SPATIAL_FREQUENCY : LAVA_SPATIAL_FREQUENCY;
    }

    /** Phase of one of the native waves in integer map-grid coordinates. */
    public static double phase(double coordinate, double tick) {
        return phase(coordinate, tick, false);
    }

    public static double phase(double coordinate, double tick, boolean water) {
        double units = coordinate * frequency(water) + tick;
        units -= Math.floor(units / TICK_PERIOD) * TICK_PERIOD;
        return units * PHASE_SCALE;
    }

    public static float height(double x, double z, double tick) {
        return height(x, z, tick, false);
    }

    public static float height(double x, double z, double tick, boolean water) {
        return LEVEL - AMPLITUDE * (float) (Math.sin(phase(x, tick, water))
                + Math.sin(phase(z, tick, water)));
    }

    /** Reproduces the recovered native normal field, not a finite-difference estimate. */
    public static void normal(double x, double z, double tick, Vector3f result) {
        normal(x, z, tick, false, result);
    }

    public static void normal(double x, double z, double tick, boolean water, Vector3f result) {
        result.set((float) Math.cos(phase(x, tick, water)), 3.0f,
                (float) Math.cos(phase(z, tick, water))).normalizeLocal();
    }
}
