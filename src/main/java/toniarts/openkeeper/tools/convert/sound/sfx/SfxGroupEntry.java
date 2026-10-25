/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.tools.convert.sound.sfx;

import java.util.Arrays;
import java.util.EnumSet;
import toniarts.openkeeper.tools.convert.IFlagEnum;

/**
 *
 * @author archdemon
 */
public final class SfxGroupEntry {

    /**
     * How a group is played. The game picks the player from SEGMENTED,
     * REPEATING, LINEAR_SEQUENCE and PROPERTY_LINKS; the meaning of the other
     * flags is as named in OpenKeeper PR #594.
     *
     * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/runtime-behaviour.md#player-classes">Player classes</a>
     */
    public enum PlaybackFlag implements IFlagEnum {

        DEFERRED(0x0001),
        /**
         * Segments are played one after another on one stream (with
         * PROPERTY_LINKS: the music and the ambience)
         */
        SEGMENTED(0x0002),
        /**
         * Keeps playing entries, without it a group plays a single sound
         */
        REPEATING(0x0004),
        LOOP_EACH(0x0008),
        /**
         * The entries play in a fixed loop (the options screen music)
         */
        LINEAR_SEQUENCE(0x0010),
        BACKEND_EVENT(0x0020),
        HEAD_RELATIVE(0x0200),
        /**
         * The next entry is picked among the links matching the intensity
         */
        PROPERTY_LINKS(0x0400);

        private final long flagValue;

        private PlaybackFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    }

    //private final static byte SIZE = 20; // 20 bytes

    private final SfxMapFileEntry parent;
    protected int typeId;
    protected int unknown_1; // runtime entries pointer
    protected int unknown_2; // probably a priority
    protected EnumSet<PlaybackFlag> flags;
    protected short parameterId; // id of the first instance parameter, 2 (intensity) for music and ambience
    protected short unknown_3;
    protected SfxEEEntry[] entries;

    public SfxGroupEntry(SfxMapFileEntry parent) {
        this.parent = parent;
    }

    public SfxMapFileEntry getParent() {
        return parent;
    }

    public SfxEEEntry[] getEntries() {
        return entries;
    }

    /**
     * Sound Type id. Must be instance of SoundType
     *
     * @return id
     */
    public int getTypeId() {
        return typeId;
    }

    /**
     * Playback flags, these select how the group is played. E.g. MUSIC and
     * AMBIENCE are SEGMENTED, REPEATING and PROPERTY_LINKS sequences that
     * follow the entry links, the one shot atmosphere is REPEATING without
     * SEGMENTED: a sequence of randomly delayed sounds.
     *
     * @return the flags
     */
    public EnumSet<PlaybackFlag> getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        return "SfxGroupEntry{" + "index=" + typeId
                + ", entries=" + Arrays.toString(entries) + "}";
    }
}