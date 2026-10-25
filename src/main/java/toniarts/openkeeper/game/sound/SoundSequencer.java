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
import java.util.Random;

/**
 * Decides which entry of a sound group plays next, as DK II does for its
 * sequenced backgrounds.<br>
 * Linked groups (music, ambience): the next entry is picked uniformly among the
 * current entry's links that match the current intensity. If none matches, the
 * sequence goes silent; it resumes from the links of the first entry, which
 * acts only as an entry point and is not played itself. Weighting is done in
 * the data by repeating a link.<br>
 * Other groups (one shot atmosphere): a random entry is played and the next one
 * starts a random delay after the previous one started.
 *
 * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/runtime-behaviour.md#the-linked-streaming-player">How DK II plays its dynamic music</a>
 */
public final class SoundSequencer {

    /**
     * The next start comes this much later than the entry's delay says
     */
    public static final int START_DELAY_PADDING = 250;

    private final List<SoundEntry> entries;
    private final Random random;
    private final boolean linked;
    private int current = -1;

    /**
     * @param entries the entries of the sound group, in order
     * @param random random source
     */
    public SoundSequencer(List<SoundEntry> entries, Random random) {
        this.entries = entries;
        this.random = random;
        this.linked = entries.stream().anyMatch(entry -> !entry.getLinks().isEmpty());
    }

    public boolean isLinked() {
        return linked;
    }

    /**
     * Advances the sequence
     *
     * @param intensity the current intensity (0-100), only used by linked
     * groups
     * @return the entry to play, or null to stay silent
     */
    public SoundEntry next(int intensity) {
        if (entries.isEmpty()) {
            return null;
        }
        if (!linked) {
            current = random.nextInt(entries.size());
            return entries.get(current);
        }

        SoundEntry from = entries.get(current < 0 ? 0 : current);
        List<SoundEntry.Link> matchingLinks = new ArrayList<>();
        for (SoundEntry.Link link : from.getLinks()) {
            if (link.matches(intensity)) {
                matchingLinks.add(link);
            }
        }
        if (matchingLinks.isEmpty()) {
            current = -1;
            return null;
        }

        current = matchingLinks.get(random.nextInt(matchingLinks.size())).target();
        return entries.get(current);
    }

    /**
     * Picks one of the entry's sounds by their cumulative weights
     *
     * @param entry the entry
     * @return the sound, or null if the entry has none
     */
    public SoundFile pickFile(SoundEntry entry) {
        List<SoundFile> files = entry.getFiles();
        if (files.isEmpty()) {
            return null;
        }
        int r = random.nextInt(0x10000);
        for (SoundFile file : files) {
            if (r <= file.getWeight()) {
                return file;
            }
        }
        return files.get(0);
    }

    /**
     * @param entry the entry
     * @return volume picked from the entry's range, 0.0-1.0
     */
    public float pickVolume(SoundEntry entry) {
        return pickInRange(entry.getMinVolume(), entry.getMaxVolume()) / 100f;
    }

    /**
     * @param entry the entry just started
     * @return milliseconds from the entry's start to the next entry's start
     */
    public int pickStartDelay(SoundEntry entry) {
        return START_DELAY_PADDING + pickInRange(entry.getMinDelay(), entry.getMaxDelay());
    }

    private int pickInRange(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min);
    }
}
