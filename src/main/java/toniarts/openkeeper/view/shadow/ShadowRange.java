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
 * The global shadow range {@code R}: how far a light's shadow reaches, and the
 * distance the shadows fade over. A slow level-of-detail control driven by
 * the frame rate, with hysteresis: at 20 fps and above it is at its cap, at
 * 10 fps and below all shadows are off.
 */
public final class ShadowRange {

    private float range = ShadowConstants.MAX_RANGE;

    /**
     * Call once per rendered frame, before the shadows are selected
     *
     * @param tpf the frame time in seconds
     */
    public void update(float tpf) {
        if (tpf <= 0f) {
            return;
        }
        float fps = 1f / tpf;
        float target = (fps - ShadowConstants.RANGE_FPS_FLOOR) * ShadowConstants.RANGE_PER_FPS;
        if (target < range - ShadowConstants.RANGE_DEAD_BAND) {
            range -= ShadowConstants.RANGE_STEP;
        }
        if (target > range + ShadowConstants.RANGE_DEAD_BAND) {
            range += ShadowConstants.RANGE_STEP;
        }
        range = Math.clamp(range, 0f, ShadowConstants.MAX_RANGE);
    }

    public float get() {
        return range;
    }
}
