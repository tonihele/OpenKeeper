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
public class SfxEEEntry {

    // private final static byte SIZE = 42; // 42 bytes

    private final SfxGroupEntry parent;
    protected int end_pointer_position; // I think not needed
    protected byte[] unknown = new byte[26];
    protected int data_pointer_next; // I think not needed
    protected SfxSoundEntry[] sounds;
    protected SfxData[] data;

    public SfxEEEntry(SfxGroupEntry parent) {
        this.parent = parent;
    }

    public SfxGroupEntry getParent() {
        return parent;
    }

    public SfxSoundEntry[] getSounds() {
        return sounds;
    }

    public SfxData[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "SfxEEEntry{" + "sounds=" + Arrays.toString(sounds)
                + ", data=" + Arrays.toString(data) + "}";
    }
}