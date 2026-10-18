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
     * Loose tail stretched past every light's radius
     */
    public static final float SOFT_FALLOFF_TAIL_TILES = 0.5f;

    /**
     * The original's terrain-light radius: the terrain data has no radius
     * field, the original reuses the scratch CLight at CBridge+0x35B with a
     * fixed radius of 4.5 tiles. Not subject to {@link #MAX_LIGHT_RADIUS_TILES}.
     */
    public static final float TERRAIN_LIGHT_RADIUS_TILES = 1.5f;

    /**
     * The original's terrain-light intensity
     */
    public static final float TERRAIN_LIGHT_INTENSITY = 0.4f;

    /**
     * Terrain lights of room tiles (the interior-slab glow of design 6.3) and
     * the per-room-type vertex ambient (design 8.1). Off: tiles are lit
     * mostly by torches, the hand (cursor) and player-coloured creatures.
     * Claim-scan lights are not room lighting and are unaffected.
     */
    public static final boolean ROOM_LIGHTING_ENABLED = false;

    /**
     * Lights carried by game objects (the dungeon heart glow, ...). Off: the
     * scene is lit by torches, the hand and player-coloured creatures.
     */
    public static final boolean OBJECT_LIGHTS_ENABLED = true;

    /**
     * Whether the PULSE / FLICKER flags of object and effect lights are
     * honoured. Off, as in the original: its object / effect light creation
     * passes flags 0, so the dungeon heart glow (flagged PULSE in the data) is
     * a constant light (lighting_design.md, dungeon heart).
     */
    public static final boolean OBJECT_LIGHTS_PULSE = false;

    /**
     * Terrain lights of explored non-room tiles (paths, lava, water, ...).
     * Off for the same reason as {@link #ROOM_LIGHTING_ENABLED}; the glow of
     * perceived-but-unexplored ground (claim scan, gold seams) stays.
     */
    public static final boolean EXPLORED_TERRAIN_LIGHTS_ENABLED = true;

    /**
     * Torch lights are small and local: at most this radius (tiles), however
     * large the room / default torch radius is.
     */
    public static final float TORCH_LIGHT_MAX_RADIUS_TILES = 0.2f;

    /**
     * Claim-scan terrain lights keep the full intensity: every other terrain
     * light is dialled back (a light saturates to its full colour within about
     * a tile, and a light on every tile summed to a fully lit map), so the
     * claim-scan glow is what stands out on unexplored rock.
     */
    public static final float CLAIM_SCAN_LIGHT_INTENSITY = 0.3f;

    /**
     * Slab lights (design &sect;6.7) use the design's intensity of 1.0
     */
    public static final float SLAB_LIGHT_INTENSITY = 1.0f;

    /**
     * Cursor light radius
     */
    public static final float CURSOR_LIGHT_RADIUS_TILES = 0.75f;

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
    public static final float PORTAL_LIGHT_RADIUS_TILES = 2f;

    /**
     * Hard cap on any light source's radius
     */
    public static final float MAX_LIGHT_RADIUS_TILES = 1.5f;
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
     * Player-coloured thing lights (a keeper's imps and dwarves) only tint the
     * ground right around them: their radius is capped here (tiles)
     */
    public static final float PLAYER_COLOURED_LIGHT_MAX_RADIUS_TILES = 0.1f;

    /**
     * Design &sect;6.1's fixed 8-slot player colour table, shade 0 (the
     * shade lights use). The slot is the player number: 0 unowned, 1 good
     * (also the fallback), 2 neutral, 3..7 the keepers. It is code in the
     * original, not data.
     */
    private static final int[][] PLAYER_LIGHT_COLOURS = {
        {0, 0, 0},
        {191, 213, 224},
        {255, 255, 255},
        {230, 50, 50},
        {50, 78, 230},
        {50, 230, 51},
        {226, 230, 50},
        {230, 50, 212}
    };
    private static final int PLAYER_LIGHT_FALLBACK_SLOT = 1;

    /**
     * The player colour a {@code PLAYER_COLOURED} light is multiplied by.
     * An absent or out-of-range player falls back to slot 1 (pale blue-grey),
     * not to black.
     *
     * @param playerId the owner's player number, {@code null} if none
     * @return {r, g, b}, 0..255
     */
    public static int[] getPlayerLightColour(Short playerId) {
        int slot = playerId == null || playerId < 0 || playerId >= PLAYER_LIGHT_COLOURS.length
                ? PLAYER_LIGHT_FALLBACK_SLOT : playerId;
        return PLAYER_LIGHT_COLOURS[slot].clone();
    }

    /**
     * Converts a {@code Light} block's {@code mKPos} offset to a jME offset.
     * The data is (x, y, z) with z the height above the floor (the imp's is
     * (0, 0, 0.5)); jME has y up and the map's y on its z axis, so the
     * height must land on jME's y -- copying the components across put the
     * light at floor level, half a tile to the side.
     */
    public static com.jme3.math.Vector3f toJmeOffset(javax.vecmath.Vector3f mKPos) {
        return new com.jme3.math.Vector3f(mKPos.x, mKPos.z, mKPos.y);
    }

    /**
     * Design &sect;6.1's thing-light enable rule: a light exists when its
     * colour is non-zero. The file's enable bit is not what decides (the
     * original ORs it with the colour test when loading), and most shipped
     * glowing records have it clear.
     */
    public static boolean isLightEnabled(toniarts.openkeeper.tools.convert.map.Light light) {
        if (light == null || light.getColor() == null) {
            return false;
        }
        java.awt.Color c = light.getColor();
        return c.getRed() != 0 || c.getGreen() != 0 || c.getBlue() != 0;
    }

    /**
     * The over-bright bit adds this to a colour channel
     */
    public static final int OVER_BRIGHT_BONUS = 256;
    public static final int TERRAIN_OVER_BRIGHT_BONUS = 255;

    /**
     * A {@code TORCH_COLOR_*} room flag adds this to its torch colour channel
     */
    public static final int TORCH_OVER_BRIGHT_BONUS = 255;

    /**
     * The rate {@link LightModulators#tick()} is expected to be driven at.
     */
    public static final int SIMULATION_TICKS_PER_SECOND = 4;

    /**
     * The fixed presentation clock {@link LightModulators#flickerStep()} is
     * driven at (design &sect;7.2, D4).
     */
    public static final int FLICKER_HZ = 30;

    public static final float FLICKER_MIN = 0.2f;
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
