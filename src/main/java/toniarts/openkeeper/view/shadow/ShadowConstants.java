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
package toniarts.openkeeper.view.shadow;

/**
 * The numbers of the creature / object shadows. Distances are in tiles.
 */
public final class ShadowConstants {

    private ShadowConstants() {
        // No!
    }

    /**
     * The shadow quality: 0 off, 1 cached frozen footprint, 2 top-down
     * footprint following the animation, 3 tilted silhouette per light.
     * Higher values behave like 3.
     */
    public static final int DEFAULT_LEVEL = 3;
    public static final int MAX_LEVEL = 3;

    /**
     * Most shadows a single caster gets, one per light
     */
    public static final int MAX_SHADOWS_PER_CASTER = 3;

    /**
     * Most static lights in a tile's light set
     */
    public static final int SET_STATIC_MAX = 12;

    /**
     * Most lights, static and dynamic, in a tile's light set
     */
    public static final int SET_TOTAL_MAX = 24;

    /**
     * How many tiles a tile looks for lights, in each direction (7x7 tiles)
     */
    public static final int NEIGHBOURHOOD_RADIUS = 3;

    /**
     * The darkest a single shadow gets
     */
    public static final float MAX_ALPHA = 0.7f;
    public static final float FADE_SLOPE = 1.5f;

    /**
     * The global shadow range's cap, and so the farthest a light reaches
     */
    public static final float MAX_RANGE = 2.5f;

    /**
     * The range controller's frame rate window: the target range is
     * {@code (fps - RANGE_FPS_FLOOR) * RANGE_PER_FPS}
     */
    public static final float RANGE_FPS_FLOOR = 10f;
    public static final float RANGE_PER_FPS = 0.25f;
    public static final float RANGE_DEAD_BAND = 0.5f;
    public static final float RANGE_STEP = 0.1f;

    /**
     * Width and height of the coverage texture, in pixels
     */
    public static final int SILHOUETTE_SIZE = 32;

    /**
     * Raster units per pixel (8x8 supersampling)
     */
    public static final int SUBSAMPLES = 8;

    /**
     * The raster is 0..256 units across, centred on the caster
     */
    public static final int RASTER_UNITS = SILHOUETTE_SIZE * SUBSAMPLES;
    public static final float RASTER_CENTRE = RASTER_UNITS / 2f;

    /**
     * The bounding sphere covers +-125 of the +-128 raster units
     */
    public static final float RASTER_EXTENT = 125f;

    /**
     * Coverage added per covered subsample, saturating at 255
     */
    public static final int COVERAGE_PER_SUBSAMPLE = 4;

    /**
     * How far above the floor the shadow quad lies
     */
    public static final float FLOOR_LIFT = 0.01f;

    /**
     * Tilt (rad) of the level 3 view per tile of distance between the light
     * and the caster: 67.5 degrees at the 2.5 tile range
     */
    public static final float TILT_PER_TILE = 0.4712f;

    /**
     * The height of the virtual light of the levels 1 and 2, whatever the
     * real one's
     */
    public static final float VIRTUAL_LIGHT_HEIGHT = 2f;

    /**
     * Steps of the light direction (relative to the caster's heading) the
     * level 1 cache keeps a footprint for
     */
    public static final int LEVEL_1_HEADING_STEPS = 16;
}
