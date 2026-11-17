/* Copyright (C) 2026 OpenKeeper. Licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.view.map;

import com.jme3.math.Vector3f;
import toniarts.openkeeper.utils.WorldUtils;

/** CPU reference for the recovered DKII lava wave equation used by the shader. */
public final class LiquidWaves {

    public static final float LEVEL = WorldUtils.WATER_LEVEL;
    public static final float AMPLITUDE = 0.04f;
    public static final double TICKS_PER_SECOND = 30.0;
    public static final double TICK_PERIOD = 128.0;
    private static final double PHASE_SCALE = Math.PI / 64.0;

    private LiquidWaves() {
    }

    /**
     * Phase of one of the two native waves, using the integer tile-corner
     * coordinates carried in the mesh's second texture-coordinate set.
     */
    public static double phase(double coordinate, double tick) {
        double units = coordinate * 64.0 + tick;
        units -= Math.floor(units / TICK_PERIOD) * TICK_PERIOD;
        return units * PHASE_SCALE;
    }

    public static float height(double x, double z, double tick) {
        return LEVEL - AMPLITUDE * (float) (Math.sin(phase(x, tick)) + Math.sin(phase(z, tick)));
    }

    /** Reproduces the recovered native normal field, not a finite-difference estimate. */
    public static void normal(double x, double z, double tick, Vector3f result) {
        result.set((float) Math.cos(phase(x, tick)), 3.0f,
                (float) Math.cos(phase(z, tick))).normalizeLocal();
    }
}
