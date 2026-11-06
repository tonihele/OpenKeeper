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
 * Named graphics bundles for players who do not want to tune AA / AF / SSAO /
 * cheap-lighting individually. Does <em>not</em> change the shipping default
 * (effectively {@link #HIGH}: SMAA Ultra, SSAO off) until the user picks one.
 */
public enum GraphicsQualityPreset {

    LOW(AntiAliasingMode.OFF, false, 0, true, "Low"),
    MEDIUM(AntiAliasingMode.MSAA_4, false, 4, false, "Medium"),
    /** Matches the shipping / suite path: SMAA Ultra, SSAO off, AF off. */
    HIGH(AntiAliasingMode.SMAA_ULTRA, false, 0, false, "High"),
    ULTRA(AntiAliasingMode.SMAA_ULTRA_MSAA_4, true, 16, false, "Ultra"),
    /** User mixed individual controls; not offered as a selectable preset. */
    CUSTOM(AntiAliasingMode.SMAA_ULTRA, false, 0, false, "Custom");

    /** Presets shown in graphics settings (excludes {@link #CUSTOM}). */
    public static final List<GraphicsQualityPreset> VALUES = Collections.unmodifiableList(Arrays.asList(
            LOW, MEDIUM, HIGH, ULTRA));

    private final AntiAliasingMode antiAliasing;
    private final boolean ssao;
    private final int anisotropy;
    private final boolean cheapLighting;
    private final String label;

    GraphicsQualityPreset(AntiAliasingMode antiAliasing, boolean ssao, int anisotropy,
            boolean cheapLighting, String label) {
        this.antiAliasing = antiAliasing;
        this.ssao = ssao;
        this.anisotropy = anisotropy;
        this.cheapLighting = cheapLighting;
        this.label = label;
    }

    public AntiAliasingMode getAntiAliasing() {
        return antiAliasing;
    }

    public boolean isSsao() {
        return ssao;
    }

    public int getAnisotropy() {
        return anisotropy;
    }

    public boolean isCheapLighting() {
        return cheapLighting;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    public static GraphicsQualityPreset fromStoredName(String name) {
        if (name == null || name.isEmpty()) {
            return HIGH;
        }
        try {
            return GraphicsQualityPreset.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            // Older / UI-facing label strings
        }
        for (GraphicsQualityPreset preset : values()) {
            if (preset.label.equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return HIGH;
    }
}
