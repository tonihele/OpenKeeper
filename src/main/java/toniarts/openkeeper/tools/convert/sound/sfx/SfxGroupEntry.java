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

/**
 *
 * @author archdemon
 */
public final class SfxGroupEntry {

    //private final static byte SIZE = 20; // 20 bytes

    private final SfxMapFileEntry parent;
    protected int typeId;
    protected int unknown_1; // runtime entries pointer
    protected int unknown_2; // probably a priority
    protected int flags;
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
     * AMBIENCE (0x0606) are sequences that follow the entry links, the one
     * shot atmosphere (0x0204) is a sequence of randomly delayed sounds.
     *
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        return "SfxGroupEntry{" + "index=" + typeId
                + ", entries=" + Arrays.toString(entries) + "}";
    }
}