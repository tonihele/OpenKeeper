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
public class SfxGroupEntry {

    //private final static byte SIZE = 20; // 20 bytes

    private final SfxMapFileEntry parent;
    protected int typeId;
    protected int unknown_1;
    protected int unknown_2;
    protected int unknown_3;
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

    @Override
    public String toString() {
        return "SfxGroupEntry{" + "index=" + typeId
                + ", entries=" + Arrays.toString(entries) + "}";
    }
}