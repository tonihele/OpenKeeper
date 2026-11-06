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

import java.util.EnumSet;
import java.util.Set;
import toniarts.openkeeper.tools.convert.IFlagEnum;

/**
 * *Bank.map file entry structure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class BankMapFileEntry {

    /**
     * Archive flags. Bits 3, 4, 6 and 7 are 0 in every shipped record
     */
    public enum BankFlag implements IFlagEnum {

        /**
         * {@code <name>xx.sdt} is loaded whole into memory
         */
        RESIDENT(0x01),
        /**
         * {@code <name>xx.sdt} is streamed from disk (music tracks, Track_Zero, SetPieces, all speech)
         */
        STREAMED(0x02),
        /**
         * {@code <name>.sf2} SoundFont (one_shot_atmos), falls back to resident
         */
        SOUNDFONT(0x04),
        /**
         * Never read by the game. Probably "archive holds MPEG-compressed clips" (an authoring tool flag): set on
         * all Global archives that are (mostly) MP2, clear on the only two all-PCM ones (JackinBox, Maiden). Clear
         * on all speech archives too, though they are MP2, those come from a separately built set of banks.
         */
        MPEG_COMPRESSED(0x20);

        private final long flagValue;

        private BankFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    }

    private long archiveHandle; // 0xFFFFFFFF in the file, runtime handle
    private short refCount; // runtime, file value is garbage
    private long namePtr; // runtime, file value is garbage
    private Set<BankFlag> flags = EnumSet.noneOf(BankFlag.class);
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
     * The archive flags
     *
     * @return the flags
     */
    public Set<BankFlag> getFlags() {
        return flags;
    }

    protected void setFlags(Set<BankFlag> flags) {
        this.flags = flags;
    }

    protected void setUnused(short unused) {
        this.unused = unused;
    }

    public boolean isResident() {
        return flags.contains(BankFlag.RESIDENT);
    }

    public boolean isStreamed() {
        return flags.contains(BankFlag.STREAMED);
    }

    public boolean isSoundFont() {
        return flags.contains(BankFlag.SOUNDFONT);
    }

    public boolean isMpegCompressed() {
        return flags.contains(BankFlag.MPEG_COMPRESSED);
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
