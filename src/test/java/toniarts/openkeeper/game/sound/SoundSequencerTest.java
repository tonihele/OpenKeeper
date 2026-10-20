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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundSequencerTest {

    private static SoundEntry entry(SoundEntry.Link... links) {
        SoundEntry entry = new SoundEntry(100, 100, 0, 0);
        entry.getLinks().addAll(List.of(links));
        return entry;
    }

    private static SoundEntry.Link link(int target, int min, int max) {
        return new SoundEntry.Link(target, min, max);
    }

    /**
     * Entry 0 is the entry point to 1 (calm) or 3 (intense), 1 and 2 loop and
     * jump to 3 when intense, 3 loops only when intense
     */
    private static List<SoundEntry> music() {
        return List.of(
                entry(link(1, 20, 39), link(3, 40, 100)),
                entry(link(2, 20, 39), link(3, 40, 100)),
                entry(link(1, 20, 39), link(3, 40, 100)),
                entry(link(3, 40, 100)));
    }

    @Test
    void startsFromTheLinksOfTheFirstEntry() {
        List<SoundEntry> entries = music();

        assertSame(entries.get(1), new SoundSequencer(entries, new Random(1)).next(20));
        assertSame(entries.get(3), new SoundSequencer(entries, new Random(1)).next(80));
    }

    @Test
    void followsLinksMatchingTheIntensity() {
        List<SoundEntry> entries = music();
        SoundSequencer sequencer = new SoundSequencer(entries, new Random(1));

        assertSame(entries.get(1), sequencer.next(20));
        assertSame(entries.get(2), sequencer.next(39));
        assertSame(entries.get(1), sequencer.next(20));
        assertSame(entries.get(3), sequencer.next(40));
        assertSame(entries.get(3), sequencer.next(100));
    }

    @Test
    void staysSilentWithoutMatchAndResumesFromTheFirstEntry() {
        List<SoundEntry> entries = music();
        SoundSequencer sequencer = new SoundSequencer(entries, new Random(1));

        assertNull(sequencer.next(0));
        assertSame(entries.get(3), sequencer.next(60));
        assertNull(sequencer.next(20));
        assertNull(sequencer.next(19));
        assertSame(entries.get(1), sequencer.next(20));
    }

    @Test
    void picksUniformlyAndRepeatedLinksWeighMore() {
        List<SoundEntry> entries = List.of(
                entry(link(1, 0, 100), link(2, 0, 100), link(1, 0, 100)),
                entry(),
                entry());
        Map<SoundEntry, Integer> counts = new HashMap<>();
        Random random = new Random(42);
        for (int i = 0; i < 30000; i++) {
            counts.merge(new SoundSequencer(entries, random).next(50), 1, Integer::sum);
        }

        assertEquals(20000, counts.get(entries.get(1)), 600);
        assertEquals(10000, counts.get(entries.get(2)), 600);
    }

    @Test
    void unlinkedGroupsPlayRandomEntriesWithDelays() {
        SoundEntry entry = new SoundEntry(21, 21, 8000, 20000);
        SoundSequencer sequencer = new SoundSequencer(List.of(entry), new Random(3));

        assertFalse(sequencer.isLinked());
        assertSame(entry, sequencer.next(0));
        assertSame(entry, sequencer.next(100));
        for (int i = 0; i < 1000; i++) {
            int delay = sequencer.pickStartDelay(entry);
            assertTrue(delay >= 8250 && delay < 20250, "delay " + delay);
        }
        assertEquals(0.21f, sequencer.pickVolume(entry), 0.0001f);
    }

    @Test
    void picksFilesByCumulativeWeight() {
        SoundEntry entry = new SoundEntry(100, 100, 0, 0);
        SoundFile rare = new SoundFile(null, 0, "rare", 0, 0x0fff);
        SoundFile common = new SoundFile(null, 1, "common", 0, 0xffff);
        entry.getFiles().addAll(List.of(rare, common));
        SoundSequencer sequencer = new SoundSequencer(List.of(entry), new Random(5));

        int rareCount = 0;
        for (int i = 0; i < 16000; i++) {
            if (sequencer.pickFile(entry) == rare) {
                rareCount++;
            }
        }
        assertEquals(1000, rareCount, 150);
    }
}
