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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import toniarts.openkeeper.game.sound.SoundVoice.Behaviour;

/**
 * A playable sound event of a category: a priority, flags and the groups of clips
 * to pick from.
 */
public class SoundEvent {

    private final int id;
    private final int priority;
    private final EnumSet<SoundEventFlag> flags;
    private final List<SoundEventGroup> groups;

    public SoundEvent(int id, int priority, Set<SoundEventFlag> flags, List<SoundEventGroup> groups) {
        this.id = id;
        this.priority = priority;
        this.flags = flags.isEmpty() ? EnumSet.noneOf(SoundEventFlag.class) : EnumSet.copyOf(flags);
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

    public Set<SoundEventFlag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    public List<SoundEventGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public Behaviour getBehaviour() {
        if (flags.contains(SoundEventFlag.BACKEND_EVENT)) {
            return Behaviour.BACKEND_EVENT;
        }
        if (flags.contains(SoundEventFlag.REPEATING)) {
            if (flags.contains(SoundEventFlag.LOOP_EACH)) {
                return Behaviour.REPEAT_LOOP_EACH;
            }
            if (flags.contains(SoundEventFlag.SEGMENTED)) {
                return Behaviour.LINEAR_SEQUENCE;
            }
            return Behaviour.REPEAT;
        }
        if (flags.contains(SoundEventFlag.SEGMENTED)) {
            return Behaviour.PLAY_ONCE_SEGMENTED;
        }
        return Behaviour.ONE_SHOT;
    }

    public boolean isDeferred() {
        return flags.contains(SoundEventFlag.DEFERRED);
    }

    public boolean isSegmented() {
        return flags.contains(SoundEventFlag.SEGMENTED);
    }

    public boolean hasPropertyLinks() {
        return flags.contains(SoundEventFlag.PROPERTY_LINKS);
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
        return "SoundEvent[0x" + Integer.toHexString(id) + ", prio=" + priority + ", flags=" + flags
                + ", groups=" + groups.size() + ']';
    }
}