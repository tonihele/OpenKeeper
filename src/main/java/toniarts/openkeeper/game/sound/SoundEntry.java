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

import java.util.ArrayList;
import java.util.List;

/**
 * One entry of a sound group. An entry has one or more alternative sounds and,
 * in sequenced groups like the music, links to the entries that may follow it.
 */
public final class SoundEntry {

    /**
     * A possible follow-up entry, valid when the current intensity is within
     * [minIntensity, maxIntensity]
     *
     * @param target 0-based index of the target entry in the group
     * @param minIntensity inclusive
     * @param maxIntensity inclusive
     */
    public record Link(int target, int minIntensity, int maxIntensity) {

        public boolean matches(int intensity) {
            return minIntensity <= intensity && intensity <= maxIntensity;
        }
    }

    private final List<SoundFile> files = new ArrayList<>();
    private final List<Link> links = new ArrayList<>();
    private final int minVolume;
    private final int maxVolume;
    private final int minDelay;
    private final int maxDelay;

    public SoundEntry(int minVolume, int maxVolume, int minDelay, int maxDelay) {
        this.minVolume = minVolume;
        this.maxVolume = maxVolume;
        this.minDelay = minDelay;
        this.maxDelay = maxDelay;
    }

    public List<SoundFile> getFiles() {
        return files;
    }

    public List<Link> getLinks() {
        return links;
    }

    /**
     * @return minimum volume in percent
     */
    public int getMinVolume() {
        return minVolume;
    }

    /**
     * @return maximum volume in percent
     */
    public int getMaxVolume() {
        return maxVolume;
    }

    /**
     * @return minimum delay from this entry's start to the next entry's start
     * in milliseconds (sequenced one shots only)
     */
    public int getMinDelay() {
        return minDelay;
    }

    /**
     * @return maximum delay from this entry's start to the next entry's start
     * in milliseconds (sequenced one shots only)
     */
    public int getMaxDelay() {
        return maxDelay;
    }
}
