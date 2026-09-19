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
 * {@link IKwdFile#getVariables()}; values with no file field are named,
 * documented constants per {@code lighting_design.md}.
 *
 * @author Toni Helenius &lt;helenius.toni@gmail.com&gt;
 */
public final class LightConstants {

    /**
     * The original's terrain-light radius (design &sect;6.3): the terrain
     * data has no radius field, the original reuses a fixed scratch value.
     */
    public static final float TERRAIN_LIGHT_RADIUS_TILES = 4.5f;

    /**
     * The original's terrain-light intensity (design &sect;6.3): no file
     * field, always nominal.
     */
    public static final float TERRAIN_LIGHT_INTENSITY = 1.0f;

    /**
     * Cursor light radius (design &sect;6.4). No file field.
     */
    public static final float CURSOR_LIGHT_RADIUS_TILES = 7f;

    /**
     * Cursor light intensity, 255/128 (design &sect;6.4). No file field.
     */
    public static final float CURSOR_LIGHT_INTENSITY = 255f / 128f;

    /**
     * Cursor light height above the floor (design &sect;6.4). No file
     * field.
     */
    public static final float CURSOR_LIGHT_HEIGHT = 2.125f;

    /**
     * Portal light radius: this is also the hard cap every light's radius
     * is clamped to (design &sect;6.1/&sect;6.5/&sect;9).
     */
    public static final float PORTAL_LIGHT_RADIUS_TILES = 9.875f;

    /**
     * Hard cap on any light source's radius (design &sect;6.1, &sect;9).
     */
    public static final float MAX_LIGHT_RADIUS_TILES = 9.875f;

    /**
     * Portal light colour/intensity (design &sect;6.5). No file field; DK2
     * hard-codes this for room ids 3 (Portal) and 25 (Hero Portal).
     */
    public static final int PORTAL_LIGHT_RED = 255;
    public static final int PORTAL_LIGHT_GREEN = 33;
    public static final int PORTAL_LIGHT_BLUE = 26;
    public static final float PORTAL_LIGHT_INTENSITY = 1.0f;

    /**
     * Thing-light intensity by owner type (design &sect;6.1).
     */
    public static final float OBJECT_LIGHT_INTENSITY = 180f / 128f;
    public static final float CREATURE_LIGHT_INTENSITY = 1.0f;
    public static final float SHOT_LIGHT_INTENSITY = 1.0f;
    public static final float EFFECT_LIGHT_INTENSITY = 1.0f;

    /**
     * Thing-light radius scale (design &sect;6.1): the {@code Light} struct
     * radius is scaled by 2/3 before the {@link #MAX_LIGHT_RADIUS_TILES}
     * cap is applied. Torches, terrain lights and the portal light are NOT
     * scaled this way.
     */
    public static final float THING_LIGHT_RADIUS_SCALE = 2f / 3f;

    /**
     * {@code flags.PLAYER_COLOURED} clamp floor (design &sect;6.1):
     * {@code colour = max(32, colour * playerColour / 255)}.
     */
    public static final int PLAYER_COLOURED_MIN = 32;

    /**
     * The over-bright bit adds this to a colour channel (design &sect;3).
     */
    public static final int OVER_BRIGHT_BONUS = 256;
    public static final int TERRAIN_OVER_BRIGHT_BONUS = 255;

    /**
     * The rate {@link LightModulators#tick()} is expected to be driven at.
     * <p>
     * There is no simulation-tick clock exposed to the presentation layer
     * (design goal 3 deliberately keeps lighting out of the simulation), so
     * this does not read the sim's real tick rate. It is a named constant
     * matching the stock data's raw {@code GAME_TICKS} misc-variable value
     * (4 -- see the comment next to {@link MiscType#GAME_TICKS} in
     * {@code Variable.java}). Deliberately NOT read via
     * {@code MiscVariable.getValue()}: that accessor applies an unrelated,
     * poorly-understood transform to this particular variable
     * ({@code 1363 / rawValue}, flagged with its own "how save int or float
     * values???" TODO in {@code Variable.java}) that has nothing to do with
     * a ticks-per-second rate.
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
        return (int) getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_RED);
    }

    public static int getDefaultTorchLightGreen(IKwdFile kwdFile) {
        return (int) getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_GREEN);
    }

    public static int getDefaultTorchLightBlue(IKwdFile kwdFile) {
        return (int) getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_BLUE);
    }

    /**
     * The file value is a byte where 128 = 1.0 (design &sect;3).
     */
    public static float getDefaultTorchLightIntensity(IKwdFile kwdFile) {
        return getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_INTENSITY) / 128f;
    }

    /**
     * Already converted to tiles at parse time (see
     * {@code MiscVariable.setValue}).
     */
    public static float getDefaultTorchLightRadiusTiles(IKwdFile kwdFile) {
        return getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_RADIUS_TILES);
    }

    /**
     * Already converted to tiles at parse time (see
     * {@code MiscVariable.setValue}).
     */
    public static float getDefaultTorchLightHeightTiles(IKwdFile kwdFile) {
        return getMisc(kwdFile, MiscType.DEFAULT_TORCH_LIGHT_HEIGHT_TILES);
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
