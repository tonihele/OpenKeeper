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
 * A link from an SFX.map item to an item that may follow it
 *
 * @author archdemon
 *
 * @see <a href="https://github.com/Dethware/DK2/blob/master/docs/file-formats.md#link-8-bytes">The SFX.map link layout</a>
 */
public final class SfxData {

    // private final static byte SIZE = 8; // 8 bytes

    protected int index;
    protected byte[] unknown2 = new byte[4];

    /**
     * A link to the entry that may follow the one owning this link. Used by the
     * dynamic music and ambience, where the next entry is chosen among the
     * links whose intensity range contains the current intensity.
     *
     * @return 1-based index of the target entry in the same group
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return lowest intensity (0-100, inclusive) at which this link applies
     */
    public int getMinIntensity() {
        return Byte.toUnsignedInt(unknown2[2]);
    }

    /**
     * @return highest intensity (0-100, inclusive) at which this link applies
     */
    public int getMaxIntensity() {
        return Byte.toUnsignedInt(unknown2[3]);
    }

    @Override
    public String toString() {
        return "SfxData{" + "index=" + index
                + ", unknown2=" + Arrays.toString(unknown2) + '}';
    }
}