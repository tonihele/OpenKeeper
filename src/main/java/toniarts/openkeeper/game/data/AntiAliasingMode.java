/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Selectable anti-aliasing modes. SMAA Ultra is the default: morphological
 * post-process AA with the official ULTRA search quality. Optional MSAA
 * modes remain for users who prefer hardware multisampling, and a hybrid
 * mode combines light MSAA resolve with SMAA.
 * <p>
 * {@link #MSAA_16} stays loadable for saved prefs but is omitted from
 * {@link #VALUES} — 16× MSAA is rarely worth the GPU cost at typical
 * resolutions versus 4×/8× or SMAA.
 */
public enum AntiAliasingMode {

    OFF(0, false, "Off"),
    SMAA_ULTRA(0, true, "SMAA Ultra"),
    MSAA_2(2, false, "MSAA 2x"),
    MSAA_4(4, false, "MSAA 4x"),
    MSAA_8(8, false, "MSAA 8x"),
    /** Retained for migration; not offered in the settings UI. */
    MSAA_16(16, false, "MSAA 16x"),
    SMAA_ULTRA_MSAA_4(4, true, "SMAA Ultra + MSAA 4x");

    /** Modes shown in graphics settings (excludes {@link #MSAA_16}). */
    public static final List<AntiAliasingMode> VALUES = Collections.unmodifiableList(Arrays.asList(
            OFF, SMAA_ULTRA, MSAA_2, MSAA_4, MSAA_8, SMAA_ULTRA_MSAA_4));

    private final int samples;
    private final boolean smaa;
    private final String label;

    AntiAliasingMode(int samples, boolean smaa, String label) {
        this.samples = samples;
        this.smaa = smaa;
        this.label = label;
    }

    public int getSamples() {
        return samples;
    }

    public boolean isSmaa() {
        return smaa;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static AntiAliasingMode fromStoredName(String name) {
        if (name == null || name.isEmpty()) {
            return SMAA_ULTRA;
        }
        try {
            return AntiAliasingMode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            // Older / UI-facing label strings
        }
        for (AntiAliasingMode mode : values()) {
            if (mode.label.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return SMAA_ULTRA;
    }
}
