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
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Lighting-related constants. Values with a file field are read from
 * {@link IKwdFile#getVariables()}; values with no file field are named
 *
 */
public final class LightConstants {

    // ---- switches ----

    /**
     * Terrain lights of room tiles (the interior-slab glow) and
     * the per-room-type vertex ambient. Off: tiles are lit
     * mostly by torches, the hand (cursor) and player-coloured creatures.
     * Claim-scan lights are not room lighting and are unaffected.
     */
    public static final boolean ROOM_LIGHTING_ENABLED = true;

    /**
     * Lights carried by game objects (the dungeon heart glow, ...). Off: the
     * scene is lit by torches, the hand and player-coloured creatures.
     */
    public static final boolean OBJECT_LIGHTS_ENABLED = true;

    /**
     * Whether the PULSE / FLICKER flags of object and effect lights are
     * honoured. Off, as in the original: its object / effect light creation
     * passes flags 0, so the dungeon heart glow (flagged PULSE in the data) is
     * a constant light (dungeon heart).
     */
    public static final boolean OBJECT_LIGHTS_PULSE = true;

    /**
     * Terrain lights of explored non-room tiles (paths, lava, water, ...).
     * Off for the same reason as {@link #ROOM_LIGHTING_ENABLED}; the glow of
     * perceived-but-unexplored ground (claim scan, gold seams) stays.
     */
    public static final boolean EXPLORED_TERRAIN_LIGHTS_ENABLED = true;

    /**
     * The per-terrain vertex ambient (KWD AMBIENT_LIGHT) of non-solid,
     * non-room tiles; solid tiles (rock, walls) never get it.
     * Off: such tiles get no baseline glow and are lit only by lights.
     */
    public static final boolean TERRAIN_AMBIENT_ENABLED = true;

    // ---- general ----

    /**
     * Hard cap on any light source's radius
     */
    public static final float MAX_LIGHT_RADIUS_TILES = 4.5f;

    /**
     * Loose tail stretched past every light's radius
     */
    public static final float SOFT_FALLOFF_TAIL_TILES = 0.1f;

    /**
     * Largest value of a light colour channel (255 is the plain colour, up to 511 is over-bright).
     */
    public static final int MAX_COLOUR = 511;

    /**
     * Divisor turning a 0..255 colour channel into 0..1.
     */
    public static final float COLOUR_SCALE = 255f;

    /**
     * The over-bright bit adds this to a colour channel
     */
    public static final int OVER_BRIGHT_BONUS = 256;

    /**
     * Minimum light of solid rock when the model viewer's "lit solid rock" is
     * on, so it shows where no light reaches
     */
    public static final float LIT_SOLID_ROCK_MIN_LIGHT = 0.3f;

    // ---- terrain, room slab and claim scan ----

    /**
     * Radius of terrain lights. The terrain data has no radius field, so all
     * of them use 1.125 tiles: small enough to light raised geometry such as
     * rims and wall faces, but not the floor (the "edge glow"). Not subject
     * to {@link #MAX_LIGHT_RADIUS_TILES}.
     */
    public static final float TERRAIN_LIGHT_RADIUS_TILES = 1.125f;

    /**
     * Intensity of terrain lights
     */
    public static final float TERRAIN_LIGHT_INTENSITY = 0.4f;

    public static final int TERRAIN_OVER_BRIGHT_BONUS = 255;

    /**
     * Intensity of the interior-slab terrain light of room tiles. Higher than
     * {@link #TERRAIN_LIGHT_INTENSITY}, which stays for paths, water, lava, ...
     */
    public static final float ROOM_TERRAIN_LIGHT_INTENSITY = 1f;

    /**
     * Slab lights use an intensity of 1.0
     */
    public static final float SLAB_LIGHT_INTENSITY = 1.0f;

    /**
     * Claim-scan terrain lights keep the full intensity: every other terrain
     * light is dialled back (a light saturates to its full colour within about
     * a tile, and a light on every tile summed to a fully lit map), so the
     * claim-scan glow is what stands out on unexplored rock.
     */
    public static final float CLAIM_SCAN_LIGHT_INTENSITY = 0.4f;

    // ---- torches ----

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
     * Torch lights are small and local: at most this radius (tiles), however
     * large the room / default torch radius is.
     */
    public static final float TORCH_LIGHT_MAX_RADIUS_TILES = 4f;

    /**
     * A {@code TORCH_COLOR_*} room flag adds this to its torch colour channel
     */
    public static final int TORCH_OVER_BRIGHT_BONUS = 255;

    /**
     * Brightest a room torch channel gets once the over-bright bonus is
     * added. The usual room torch (a base of 44 plus the bonus) lands at
     * about 300; the dungeon heart's base red of 244 would reach 499 and
     * turn its walls deep red, so channels are held to the usual flame.
     */
    public static final int TORCH_MAX_CHANNEL = 300;

    // ---- cursor ----

    /**
     * Cursor light radius
     */
    public static final float CURSOR_LIGHT_RADIUS_TILES = 1.5f;

    /**
     * Cursor light intensity, 255/128
     */
    public static final float CURSOR_LIGHT_INTENSITY = 255f / 128f;

    /**
     * Cursor light height above the floor, over solid rock (whose top is
     * higher up)
     */
    public static final float CURSOR_LIGHT_HEIGHT = 2.125f;

    /**
     * Cursor light height over open ground, in world space: just above the
     * floor, low enough to reach the creatures and objects standing under the
     * hand
     */
    public static final float CURSOR_LIGHT_HEIGHT_OPEN = WorldUtils.FLOOR_HEIGHT + 0.1f;

    // ---- portals ----

    /**
     * Portal light radius
     */
    public static final float PORTAL_LIGHT_RADIUS_TILES = 2f;

    /**
     * Portal light colour and intensity, used for the room ids 3 (Portal) and
     * 25 (Hero Portal).
     */
    public static final int PORTAL_LIGHT_RED = 255;
    public static final int PORTAL_LIGHT_GREEN = 33;
    public static final int PORTAL_LIGHT_BLUE = 26;
    public static final float PORTAL_LIGHT_INTENSITY = 1.0f;

    // ---- thing lights (objects, creatures, shots, effects) ----

    /**
     * Thing-light intensity by owner type.
     */
    public static final float OBJECT_LIGHT_INTENSITY = 180f / 128f;
    public static final float CREATURE_LIGHT_INTENSITY = 1.0f;
    public static final float SHOT_LIGHT_INTENSITY = 1.0f;
    public static final float EFFECT_LIGHT_INTENSITY = 1.0f;

    /**
     * Share of its own intensity a creature / effect light shines with, on
     * terrain and on other things
     */
    public static final float CREATURE_LIGHT_STRENGTH = 1f;

    /**
     * Thing-light radius scale
     */
    public static final float THING_LIGHT_RADIUS_SCALE = 2f / 3f;

    /**
     * Extra radius factor for object lights only (dungeon heart glow, ...),
     * applied on top of {@link #THING_LIGHT_RADIUS_SCALE} and still capped by
     * {@link #MAX_LIGHT_RADIUS_TILES}. Creatures and effects are unaffected.
     */
    public static final float OBJECT_LIGHT_RADIUS_SCALE = 1.5f;

    /**
     * {@code flags.PLAYER_COLOURED} clamp floor
     * {@code colour = max(32, colour * playerColour / 255)}.
     */
    public static final int PLAYER_COLOURED_MIN = 32;

    /**
     * Player-coloured thing lights (a keeper's imps and dwarves) only tint the
     * ground right around them: their radius is capped here (tiles)
     */
    public static final float PLAYER_COLOURED_LIGHT_MAX_RADIUS_TILES = 0.8f;

    /**
     * The fixed 8-slot player colour table, shade 0 (the
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

    // ---- timing and flicker ----

    /**
     * The rate {@link LightModulators#tick()} is expected to be driven at.
     */
    public static final int SIMULATION_TICKS_PER_SECOND = 4;

    /**
     * The fixed presentation clock {@link LightModulators#flickerStep()} is
     * driven at.
     */
    public static final int FLICKER_HZ = 15;

    public static final float FLICKER_MIN = 0.4f;
    public static final float FLICKER_MAX = 1.0f;
    public static final float FLICKER_STEP = 0.125f;

    private LightConstants() {
    }

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
     * The thing-light enable rule: a light exists when its
     * colour is non-zero. The file's enable bit is not what decides (the
     * original ORs it with the colour test when loading), and most shipped
     * glowing records have it clear.
     */
    public static boolean isLightEnabled(toniarts.openkeeper.tools.convert.map.Light light) {
        if (light == null || light.getColor() == null) {
            return false;
        }
        toniarts.openkeeper.utils.Color c = light.getColor();
        return c.getRed() != 0 || c.getGreen() != 0 || c.getBlue() != 0;
    }

    /**
     * A colour channel with its over-bright flag applied: when the flag is
     * set, {@code bonus} is added on top of the stored value.
     */
    public static int withBonus(int channel, boolean flagSet, int bonus) {
        return flagSet ? channel + bonus : channel;
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
     * The file value is a byte where 128 = 1.0.
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
