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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A playable sound event of a category: a priority, flags and the groups of clips
 * to pick from.
 */
public class SoundEvent {

    private final int id;
    private final int priority;
    private final int flags;
    private final List<SoundEventGroup> groups;

    public SoundEvent(int id, int priority, int flags, List<SoundEventGroup> groups) {
        this.id = id;
        this.priority = priority;
        this.flags = flags;
        this.groups = groups;
    }

    public int getId() {
        return id;
    }

    /**
     * Event priority, higher is more important.
     */
    public int getPriority() {
        return priority;
    }

    public int getFlags() {
        return flags;
    }

    public List<SoundEventGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public SoundVoiceBehaviour getBehaviour() {
        return SoundEventFlags.behaviourOf(flags);
    }

    public boolean isDeferred() {
        return SoundEventFlags.isSet(flags, SoundEventFlags.DEFERRED);
    }

    public boolean isSegmented() {
        return SoundEventFlags.isSet(flags, SoundEventFlags.SEGMENTED);
    }

    public boolean hasPropertyLinks() {
        return SoundEventFlags.isSet(flags, SoundEventFlags.PROPERTY_LINKS);
    }

    @Nullable
    public SoundEventGroup getGroup(int index1Based) {
        int index = index1Based - 1;
        if (index < 0 || index >= groups.size()) {
            return null;
        }
        return groups.get(index);
    }

    @Override
    public String toString() {
        return "SoundEvent[0x" + Integer.toHexString(id) + ", prio=" + priority + ", flags=0x"
                + Integer.toHexString(flags) + ", groups=" + groups.size() + ']';
    }
}