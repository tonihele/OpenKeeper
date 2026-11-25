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
package toniarts.openkeeper.game.sound;

/** User preference for positional audio rendering on the current output device. */
public enum AudioOutputMode {
    AUTO("Auto"),
    HEADPHONES("Headphones (HRTF)"),
    SPEAKERS("Speakers");

    private final String label;

    AudioOutputMode(String label) {
        this.label = label;
    }

    public static AudioOutputMode fromSetting(Object value) {
        if (value instanceof String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // An old or unrecognized preference uses the device default.
            }
        }
        return AUTO;
    }

    @Override
    public String toString() {
        return label;
    }
}
