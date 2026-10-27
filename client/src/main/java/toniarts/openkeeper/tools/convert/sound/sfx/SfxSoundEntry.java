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

/**
 *
 * @author archdemon
 */
public final class SfxSoundEntry {

    protected final static byte SIZE = 16; // 16 bytes

    private final SfxEEEntry parent;
    protected int index;
    protected int unknown_1;
    protected int unknown_2;
    protected int archiveId;

    public SfxSoundEntry(SfxEEEntry parent) {
        this.parent = parent;
    }

    public SfxEEEntry getParent() {
        return parent;
    }

    /**
     * 1-based entry id in *.SDT file
     *
     * @return  index
     */
    public int getIndex() {
        return index;
    }

    /**
     * 1-based archive id in *BANK.map file
     *
     * @return index
     */
    public int getArchiveId() {
        return archiveId;
    }

    @Override
    public String toString() {
        return "SfxSoundEntry{" + "index=" + index+ ", archiveId=" + archiveId + "}";
    }
}