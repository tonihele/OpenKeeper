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

/**
 * The music level of DK II. Game events request a level each update, the
 * highest request since the previous update wins. The level is turned into the
 * intensity that selects which part of the music and ambience plays. After
 * every update a level above {@link #LEVEL_ONE} falls back to it, so it stays
 * high only while it is requested, and the music keeps playing quietly after a
 * fight. Only {@link #set(int)} brings it back to {@link #SILENT}.
 *
 * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/runtime-behaviour.md#the-music-level">The DK II music level</a>
 */
public final class MusicLevel {

    /**
     * No music, only the ambience (the level start state)
     */
    public static final int SILENT = 0;
    /**
     * Any fight
     */
    public static final int LEVEL_ONE = 1;
    /**
     * Battle threat over MUSIC_LEVEL_TWO_THREAT_THRESHOLD
     */
    public static final int LEVEL_TWO = 2;
    /**
     * Battle threat over MUSIC_LEVEL_THREE_THREAT_THRESHOLD
     */
    public static final int LEVEL_THREE = 3;
    /**
     * Battle threat over MUSIC_LEVEL_FOUR_THREAT_THRESHOLD or a Horned Reaper
     * on the loose
     */
    public static final int LEVEL_FOUR = 4;

    private static final int INTENSITY_PER_LEVEL = 20;

    private int level = SILENT;
    private volatile int intensity;

    /**
     * Request at least the given level
     *
     * @param level the level
     */
    public void raise(int level) {
        this.level = Math.max(this.level, level);
    }

    /**
     * Force the given level
     *
     * @param level the level
     */
    public void set(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Publish the requested level as intensity and let it decay
     */
    public void update() {
        intensity = level * INTENSITY_PER_LEVEL;
        if (level > LEVEL_ONE) {
            level = LEVEL_ONE;
        }
    }

    /**
     * The intensity (0-100) the sequences use, safe to read from the audio
     * thread
     *
     * @return the intensity
     */
    public int getIntensity() {
        return intensity;
    }
}
