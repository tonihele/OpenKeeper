/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.view.lighting;

import java.util.Map;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;

/**
 * Lighting-related constants. Values with a file field are read from
 * {@link IKwdFile#getVariables()}; values with no file field are named
 *
 */
public final class LightConstants {

    /**
     * No lighting_design.md source -- a deliberate deviation from the
     * design's own "the mesh base colour" (i.e. black) outside every
     * light's reach: a fully unlit tile still gets this much ambient
     * floor, so torch-less ground reads as a dim, murky dungeon rather
     * than a flat black void. Applied as a per-channel floor (max), not
     * an addition, so it never washes out an already-lit tile.
     */
    public static final float MIN_AMBIENT_FLOOR = 0.12f;

    /**
     * Loose tail stretched past every light's radius
     */
    public static final float SOFT_FALLOFF_TAIL_TILES = 1.0f;

    /**
     * The original's terrain-light radius: the terrain
     * data has no radius field, the original reuses a fixed scratch value.
     */
    public static final float TERRAIN_LIGHT_RADIUS_TILES = 1.5f;

    /**
     * The original's terrain-light intensity
     */
    public static final float TERRAIN_LIGHT_INTENSITY = 1.0f;

    /**
     * Cursor light radius
     */
    public static final float CURSOR_LIGHT_RADIUS_TILES = 1.5f;

    /**
     * Cursor light intensity, 255/128
     */
    public static final float CURSOR_LIGHT_INTENSITY = 255f / 128f;

    /**
     * Cursor light height above the floor
     * field.
     */
    public static final float CURSOR_LIGHT_HEIGHT = 2.125f;

    /**
     * Portal light radius: this is also the hard cap every light's radius
     * is clamped to
     */
    public static final float PORTAL_LIGHT_RADIUS_TILES = 9.875f;

    /**
     * Hard cap on any light source's radius
     */
    public static final float MAX_LIGHT_RADIUS_TILES = 2f;

    /**
     * Portal light colour/intensity DK2
     * hard-codes this for room ids 3 (Portal) and 25 (Hero Portal).
     */
    public static final int PORTAL_LIGHT_RED = 255;
    public static final int PORTAL_LIGHT_GREEN = 33;
    public static final int PORTAL_LIGHT_BLUE = 26;
    public static final float PORTAL_LIGHT_INTENSITY = 1.0f;

    /**
     * Fallbacks for the {@code DEFAULT_TORCH_LIGHT_*} misc variables
     */
    public static final int DEFAULT_TORCH_LIGHT_RED_FALLBACK = 300;
    public static final int DEFAULT_TORCH_LIGHT_GREEN_FALLBACK = 200;
    public static final int DEFAULT_TORCH_LIGHT_BLUE_FALLBACK = 100;
    public static final float DEFAULT_TORCH_LIGHT_INTENSITY_FALLBACK = 180f / 128f;
    public static final float DEFAULT_TORCH_LIGHT_RADIUS_TILES_FALLBACK = 1.5f;
    public static final float DEFAULT_TORCH_LIGHT_HEIGHT_TILES_FALLBACK = 1.5f;

    /**
     * Thing-light intensity by owner type (design &sect;6.1).
     */
    public static final float OBJECT_LIGHT_INTENSITY = 180f / 128f;
    public static final float CREATURE_LIGHT_INTENSITY = 1.0f;
    public static final float SHOT_LIGHT_INTENSITY = 1.0f;
    public static final float EFFECT_LIGHT_INTENSITY = 1.0f;

    /**
     * Thing-light radius scale
     */
    public static final float THING_LIGHT_RADIUS_SCALE = 2f / 3f;

    /**
     * {@code flags.PLAYER_COLOURED} clamp floor
     * {@code colour = max(32, colour * playerColour / 255)}.
     */
    public static final int PLAYER_COLOURED_MIN = 32;

    /**
     * The over-bright bit adds this to a colour channel
     */
    public static final int OVER_BRIGHT_BONUS = 256;
    public static final int TERRAIN_OVER_BRIGHT_BONUS = 255;

    /**
     * Player-coloured reinforced walls/paths
     * -- this is DK2's own claimed-terrain tinting, not part of the
     * dynamic-light model). Since vertex ambient multiplies the diffuse
     * texture, blend the owner's colour with a neutral grey rather than
     * using it at full strength, or channels it doesn't cover would be
     * crushed to black and the terrain would read as a flat colour block
     * instead of a tinted texture. Tunable, not derived from game data.
     */
    public static final float PLAYER_COLOURED_TERRAIN_BASE = 0.45f;
    public static final float PLAYER_COLOURED_TERRAIN_TINT = 0.35f;

    /**
     * The rate {@link LightModulators#tick()} is expected to be driven at.
     */
    public static final int SIMULATION_TICKS_PER_SECOND = 4;

    /**
     * The fixed presentation clock {@link LightModulators#flickerStep()} is
     * driven at (design &sect;7.2, D4).
     */
    public static final int FLICKER_HZ = 30;

    public static final float FLICKER_MIN = 0.5f;
    public static final float FLICKER_MAX = 1.0f;
    public static final float FLICKER_STEP = 0.125f;

    private LightConstants() {
    }

    public static int getDefaultTorchLightRed(IKwdFile kwdFile) {
        int value = (int) getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_RED);
        return value > 0 ? value : DEFAULT_TORCH_LIGHT_RED_FALLBACK;
    }

    public static int getDefaultTorchLightGreen(IKwdFile kwdFile) {
        int value = (int) getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_GREEN);
        return value > 0 ? value : DEFAULT_TORCH_LIGHT_GREEN_FALLBACK;
    }

    public static int getDefaultTorchLightBlue(IKwdFile kwdFile) {
        int value = (int) getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_BLUE);
        return value > 0 ? value : DEFAULT_TORCH_LIGHT_BLUE_FALLBACK;
    }

    /**
     * The file value is a byte where 128 = 1.0 (design &sect;3).
     */
    public static float getDefaultTorchLightIntensity(IKwdFile kwdFile) {
        float intensity = getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_INTENSITY) / 128f;
        return intensity > 0f ? intensity : DEFAULT_TORCH_LIGHT_INTENSITY_FALLBACK;
    }

    /**
     * Already converted to tiles at parse time (see
     * {@code MiscVariable.setValue}). A level that omits the variable
     * yields a zero-radius source that renders nothing (see the fallback
     * constants' comment), so zero falls back to a working pool.
     */
    public static float getDefaultTorchLightRadiusTiles(IKwdFile kwdFile) {
        float radius = getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_RADIUS_TILES);
        return radius > 0f ? radius : DEFAULT_TORCH_LIGHT_RADIUS_TILES_FALLBACK;
    }

    /**
     * Already converted to tiles at parse time (see
     * {@code MiscVariable.setValue}).
     */
    public static float getDefaultTorchLightHeightTiles(IKwdFile kwdFile) {
        float height = getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_HEIGHT_TILES);
        return height > 0f ? height : DEFAULT_TORCH_LIGHT_HEIGHT_TILES_FALLBACK;
    }

    public static int getClaimScanLightRed(IKwdFile kwdFile) {
        return (int) getMisc(kwdFile, MiscType.CLAIM_SCAN_LIGHT_RED);
    }

    public static int getClaimScanLightGreen(IKwdFile kwdFile) {
        return (int) getMisc(kwdFile, MiscType.CLAIM_SCAN_LIGHT_GREEN);
    }

    public static int getClaimScanLightBlue(IKwdFile kwdFile) {
        return (int) getMisc(kwdFile, MiscType.CLAIM_SCAN_LIGHT_BLUE);
    }

    private static float getMisc(IKwdFile kwdFile, MiscType type) {
        Map<MiscType, MiscVariable> variables = kwdFile.getVariables();
        MiscVariable variable = (variables != null) ? variables.get(type) : null;
        return (variable != null) ? variable.getValue() : 0f;
    }
}
