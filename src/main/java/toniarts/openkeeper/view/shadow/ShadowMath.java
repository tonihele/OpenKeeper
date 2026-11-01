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
 * The shadow formulas: opacity and where the shadow quad lies. Pure
 * functions, all distances horizontal unless said otherwise.
 */
public final class ShadowMath {

    /**
     * Where a shadow quad lies along the light-to-caster direction, measured
     * from the caster's foot point
     */
    public record Extent(float near, float far) {

    }

    private ShadowMath() {
        // No!
    }

    /**
     * The opacity of a shadow: full strength up to a bit over half of the
     * fade range, then falling linearly to nothing at it.
     *
     * @param fadeRange R', the range this caster's shadows fade over
     * @param distance the horizontal distance between the light and the caster
     * @return the opacity, 0 or less if there is nothing to draw
     */
    public static float opacity(float fadeRange, float distance) {
        if (fadeRange <= 0f) {
            return 0f;
        }
        float alpha = ShadowConstants.FADE_SLOPE * (fadeRange - distance) / fadeRange;
        return Math.min(alpha, ShadowConstants.MAX_ALPHA);
    }

    /**
     * The tilt from straight down of the level 3 view, 0 right under the light
     *
     * @param distance the horizontal distance between the light and the caster
     */
    public static float tilt(float distance) {
        return distance * ShadowConstants.TILT_PER_TILE;
    }

    /**
     * Where the quad of the given level lies.
     *
     * @param level the shadow quality, 1 and 2 share the point light
     * projection, 3 and above project along the tilted view
     * @param distance the horizontal distance between the light and the caster
     * @param height the caster's centre above the shadow plane
     * @param radius the caster's bounding radius
     */
    public static Extent extent(int level, float distance, float height, float radius) {
        if (level < 3) {
            float half = distance / ShadowConstants.VIRTUAL_LIGHT_HEIGHT;
            return new Extent(half * (height - radius), half * (height + radius));
        }

        // The image row q (along the tilted view) maps linearly to the floor:
        // q / cos(theta) + height * tan(theta). The bounding sphere spans
        // q = +-radius, which is the near and far of the tilted square
        float theta = tilt(distance);
        float shift = height * (float) Math.tan(theta);
        float reach = radius / (float) Math.cos(theta);
        return new Extent(shift - reach, shift + reach);
    }
}
