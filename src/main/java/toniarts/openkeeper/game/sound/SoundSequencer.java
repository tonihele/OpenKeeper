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
import javax.annotation.Nullable;

/**
 * Picks the segments of a property linked sound event, the music, as DK II
 * does. When a segment ends, the next one is picked uniformly among the current
 * group's links that match the property (the music intensity); the data weighs
 * a link by repeating it. If no link matches, the music goes silent. It resumes
 * from the links of the first group, which is only an entry point.
 *
 * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/runtime-behaviour.md#the-linked-streaming-player">How DK II plays its dynamic music</a>
 */
public final class SoundSequencer {

    private final List<SoundEventGroup> groups;
    @Nullable
    private SoundEventGroup current;

    public SoundSequencer(SoundEvent event) {
        this(event.getGroups());
    }

    SoundSequencer(List<SoundEventGroup> groups) {
        this.groups = groups;
    }

    /**
     * Advances the sequence
     *
     * @param property the current property (the music intensity, 0-100)
     * @return the group to play next, or null to stay silent
     */
    @Nullable
    public SoundEventGroup next(int property) {
        if (groups.isEmpty()) {
            return null;
        }

        SoundEventGroup from = current != null ? current : groups.get(0);
        List<SoundLink> matchingLinks = new ArrayList<>();
        for (SoundLink link : from.getLinks()) {
            if (link.getTarget() != null && link.matchesProperty(property)) {
                matchingLinks.add(link);
            }
        }
        if (matchingLinks.isEmpty()) {
            current = null;
            return null;
        }

        current = matchingLinks.get(SoundRng.nextInt(matchingLinks.size())).getTarget();
        return current;
    }
}
