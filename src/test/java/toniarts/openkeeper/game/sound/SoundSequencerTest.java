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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SoundSequencerTest {

    private Random previousRandom;

    @BeforeEach
    void seedRandom() {
        previousRandom = SoundRng.getRandom();
        SoundRng.setRandom(new Random(1));
    }

    @AfterEach
    void restoreRandom() {
        SoundRng.setRandom(previousRandom);
    }

    /**
     * Groups whose links are given as {target (1-based), min, max} triplets,
     * resolved like the parser does
     */
    private static List<SoundEventGroup> groups(int[]... linksPerGroup) {
        List<SoundEventGroup> groups = new ArrayList<>();
        for (int[] links : linksPerGroup) {
            List<SoundLink> groupLinks = new ArrayList<>();
            for (int i = 0; i < links.length; i += 3) {
                groupLinks.add(new SoundLink(links[i], links[i + 1], links[i + 2]));
            }
            groups.add(new SoundEventGroup(100, 100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), groupLinks));
        }
        for (SoundEventGroup group : groups) {
            for (SoundLink link : group.getLinks()) {
                link.setTarget(groups.get(link.getGroupIndex() - 1));
            }
        }
        return groups;
    }

    /**
     * Group 1 is the entry point to 2 (calm) or 4 (intense), 2 and 3 loop and
     * jump to 4 when intense, 4 loops only when intense
     */
    private static List<SoundEventGroup> music() {
        return groups(
                new int[]{2, 20, 39, 4, 40, 100},
                new int[]{3, 20, 39, 4, 40, 100},
                new int[]{2, 20, 39, 4, 40, 100},
                new int[]{4, 40, 100});
    }

    @Test
    void startsFromTheLinksOfTheFirstGroup() {
        List<SoundEventGroup> groups = music();

        assertSame(groups.get(1), new SoundSequencer(groups).next(20));
        assertSame(groups.get(3), new SoundSequencer(groups).next(80));
    }

    @Test
    void followsLinksMatchingTheProperty() {
        List<SoundEventGroup> groups = music();
        SoundSequencer sequencer = new SoundSequencer(groups);

        assertSame(groups.get(1), sequencer.next(20));
        assertSame(groups.get(2), sequencer.next(39));
        assertSame(groups.get(1), sequencer.next(20));
        assertSame(groups.get(3), sequencer.next(40));
        assertSame(groups.get(3), sequencer.next(100));
    }

    @Test
    void staysSilentWithoutMatchAndResumesFromTheFirstGroup() {
        List<SoundEventGroup> groups = music();
        SoundSequencer sequencer = new SoundSequencer(groups);

        assertNull(sequencer.next(0));
        assertSame(groups.get(3), sequencer.next(60));
        assertNull(sequencer.next(20));
        assertNull(sequencer.next(19));
        assertSame(groups.get(1), sequencer.next(20));
    }

    @Test
    void picksUniformlyAndRepeatedLinksWeighMore() {
        List<SoundEventGroup> groups = groups(new int[]{2, 0, 100, 3, 0, 100, 2, 0, 100}, new int[0], new int[0]);
        Map<SoundEventGroup, Integer> counts = new HashMap<>();
        for (int i = 0; i < 30000; i++) {
            counts.merge(new SoundSequencer(groups).next(50), 1, Integer::sum);
        }

        assertEquals(20000, counts.get(groups.get(1)), 600);
        assertEquals(10000, counts.get(groups.get(2)), 600);
    }
}
