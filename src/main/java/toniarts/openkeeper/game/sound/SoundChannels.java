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
package toniarts.openkeeper.game.sound;

/**
 * The volume channels a category can be affected by, taken from the channel
 * mask of the category's {@code SFX.map} entry.
 */
public final class SoundChannels {

    public static final int MASTER = 0;
    public static final int MUSIC = 1;
    public static final int SPEECH = 2;
    public static final int EFFECTS = 3;

    private SoundChannels() {
    }
}