/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.tools.convert.sound;

/**
 * *Bank.map file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class BankMapFileEntry {

    /**
     * {@code <name>xx.sdt} is loaded whole into memory
     */
    public static final int FLAG_RESIDENT = 0x01;
    /**
     * {@code <name>xx.sdt} is streamed from disk (music tracks, Track_Zero, SetPieces, all speech)
     */
    public static final int FLAG_STREAMED = 0x02;
    /**
     * {@code <name>.sf2} SoundFont (one_shot_atmos), falls back to resident
     */
    public static final int FLAG_SOUNDFONT = 0x04;
    /**
     * Never read by the game. Probably "archive holds MPEG-compressed clips" (an authoring tool flag): set on
     * all Global archives that are (mostly) MP2, clear on the only two all-PCM ones (JackinBox, Maiden). Clear
     * on all speech archives too, though they are MP2, those come from a separately built set of banks.
     */
    public static final int FLAG_MPEG_COMPRESSED = 0x20;

    private long archiveHandle; // 0xFFFFFFFF in the file, runtime handle
    private short refCount; // runtime, file value is garbage
    private long namePtr; // runtime, file value is garbage
    private short flags; // see FLAG_* constants, bits 3, 4, 6 and 7 are 0 in every shipped record
    private short unused; // never read
    private String name;

    protected void setArchiveHandle(long archiveHandle) {
        this.archiveHandle = archiveHandle;
    }

    protected void setRefCount(short refCount) {
        this.refCount = refCount;
    }

    protected void setNamePtr(long namePtr) {
        this.namePtr = namePtr;
    }

    /**
     * The archive flag byte, see the {@code FLAG_*} constants
     *
     * @return the raw flags
     */
    public int getFlags() {
        return flags;
    }

    protected void setFlags(short flags) {
        this.flags = flags;
    }

    protected void setUnused(short unused) {
        this.unused = unused;
    }

    public boolean isResident() {
        return (flags & FLAG_RESIDENT) != 0;
    }

    public boolean isStreamed() {
        return (flags & FLAG_STREAMED) != 0;
    }

    public boolean isSoundFont() {
        return (flags & FLAG_SOUNDFONT) != 0;
    }

    public boolean isMpegCompressed() {
        return (flags & FLAG_MPEG_COMPRESSED) != 0;
    }

    /**
     * Part of filename from "Sfx" directory
     * Without quality "HD" or "HW" and without extension "sdt"
     *
     * @return filename
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }
}
