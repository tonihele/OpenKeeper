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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 *
 * @author ArchDemon
 */
public class SfxMapFile {

    private final static int HEADER_ID[] = new int[] {
        0xE9612C00, // dword_674038
        0x11D231D0, // dword_67403C
        0xB00009B4, // dword_674040
        0x03F293C9 // dword_674044
    };
    // Header
    private final int unknown_1; // not used
    private final int unknown_2; // not used

    private final File file;
    private SfxMapEntry[] entries;

    private class SfxMapEntry {

        protected final static byte SIZE = 24; // 24 bytes

        protected int unknown_1;
        protected int unknown_2;
        protected int unknown_3;
        protected float minDistance; // sounds are at full volume if closer than this
        protected float maxDistance; // sounds are muted if further away than this
        protected float scale; // relative amount to adjust rolloff
        protected SfxMapEEntry[] entries;

        @Override
        public String toString() {
            return "\n\tSfxMapEntry{" + "minDistance=" + minDistance
                    + ", maxDistance=" + maxDistance + ", scale=" + scale
                    + ", entries=" + Arrays.toString(entries) + "}\n\t";
        }
    }

    private class SfxMapEEntry {

        protected final static byte SIZE = 20; // 20 bytes
        /**
         * global index of this entry. Seems like User Tag in QSound
         */
        protected int index;
        protected int unknown_1;
        protected int unknown_2;
        protected int unknown_3;
        protected SfxMapEEEntry[] entries;

        @Override
        public String toString() {
            return "\n\t\tSfxMapEEntry{" + "index=" + index
                    + ", entries=" + Arrays.toString(entries) + "}\n\t\t";
        }
    }

    private class SfxMapEEEntry {

        protected final static byte SIZE = 42; // 42 bytes

        protected int end_pointer_position; // I think not needed
        protected byte[] unknown = new byte[26];
        protected int data_pointer_next; // I think not needed
        protected SfxMapEEEEntry[] entries;
        protected SfxMapData[] data;

        @Override
        public String toString() {
            return "\n\t\t\tSfxMapEEEntry{" + "entries=" + Arrays.toString(entries)
                    + ", data=" + Arrays.toString(data) + "}\n\t\t\t";
        }
    }

    private class SfxMapEEEEntry {

        protected final static byte SIZE = 16; // 16 bytes
        /**
         * 1-based entry id in *.SDT file
         */
        protected int index;
        protected int unknown_1;
        protected int unknown_2;
        /**
         * 1-based archive id in *BANK.map file
         * if (archiveId) { need some magic; }
         */
        protected int archiveId;

        @Override
        public String toString() {
            return "\n\t\t\t\tSfxMapEEEEntry{" + "index=" + index
                    + ", edi_2=" + archiveId + "}\n\t\t\t\t";
        }
    }

    private class SfxMapData {

        protected final static byte SIZE = 8; // 8 bytes

        protected int index;
        protected byte[] unknown2 = new byte[4];

        @Override
        public String toString() {
            return "\n\t\t\t\tSfxMapData{" + "index=" + index
                    + ", unknown2=" + Arrays.toString(unknown2) + '}';
        }
    }

    public SfxMapFile(File file) throws FileNotFoundException {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawMap = new RandomAccessFile(file, "r")) {
            //Header
            int[] check = new int[] {
                ConversionUtils.readInteger(rawMap),
                ConversionUtils.readInteger(rawMap),
                ConversionUtils.readInteger(rawMap),
                ConversionUtils.readInteger(rawMap)
            };
            for (int i = 0; i < HEADER_ID.length; i++) {
                if (check[i] != HEADER_ID[i]) {
                    throw new RuntimeException(file.getName() + ": The file header is not valid");
                }
            }

            unknown_1 = ConversionUtils.readInteger(rawMap);
            unknown_2 = ConversionUtils.readInteger(rawMap);
            int count = ConversionUtils.readUnsignedInteger(rawMap);

            entries = new SfxMapEntry[count];
            for (int i = 0; i < entries.length; i++) {
                SfxMapEntry entry = new SfxMapEntry();
                count = ConversionUtils.readUnsignedInteger(rawMap);
                entry.entries = new SfxMapEEntry[count];
                entry.unknown_1 = ConversionUtils.readUnsignedInteger(rawMap);
                entry.unknown_2 = ConversionUtils.readUnsignedShort(rawMap);
                entry.unknown_3 = ConversionUtils.readUnsignedShort(rawMap);
                entry.minDistance = ConversionUtils.readFloat(rawMap);
                entry.maxDistance = ConversionUtils.readFloat(rawMap);
                entry.scale = ConversionUtils.readFloat(rawMap);

                entries[i] = entry;
            }

            for (SfxMapEntry entrie : entries) {
                for (int i = 0; i < entrie.entries.length; i++) {
                    SfxMapEEntry entry = new SfxMapEEntry();
                    entry.index = ConversionUtils.readUnsignedInteger(rawMap);
                    count = ConversionUtils.readUnsignedInteger(rawMap);
                    entry.entries = new SfxMapEEEntry[count];
                    entry.unknown_1 = ConversionUtils.readUnsignedInteger(rawMap);
                    entry.unknown_2 = ConversionUtils.readUnsignedInteger(rawMap);
                    entry.unknown_3 = ConversionUtils.readUnsignedInteger(rawMap);
                    entrie.entries[i] = entry;
                }
            }

            for (SfxMapEntry entrie : entries) {
                for (SfxMapEEntry eEntrie : entrie.entries) {

                    for (int i = 0; i < eEntrie.entries.length; i++) {
                        SfxMapEEEntry entry = new SfxMapEEEntry();

                        count = ConversionUtils.readUnsignedInteger(rawMap);
                        entry.entries = new SfxMapEEEEntry[count];

                        count = ConversionUtils.readUnsignedInteger(rawMap);
                        if (count != 0) {
                            entry.data = new SfxMapData[count];
                        }

                        entry.end_pointer_position = ConversionUtils.readInteger(rawMap);
                        rawMap.read(entry.unknown);
                        entry.data_pointer_next = ConversionUtils.readInteger(rawMap); // readUnsignedInteger

                        eEntrie.entries[i] = entry;
                    }

                    for (SfxMapEEEntry eeEntrie : eEntrie.entries) {
                        for (int i = 0; i < eeEntrie.entries.length; i++) {
                            SfxMapEEEEntry entry = new SfxMapEEEEntry();
                            entry.index = ConversionUtils.readUnsignedInteger(rawMap);
                            entry.unknown_1 = ConversionUtils.readUnsignedInteger(rawMap);
                            entry.unknown_2 = ConversionUtils.readUnsignedInteger(rawMap);
                            entry.archiveId = ConversionUtils.readUnsignedInteger(rawMap);

                            eeEntrie.entries[i] = entry;
                        }

                        if (eeEntrie.data != null) {
                            for (int j = 0; j < eeEntrie.data.length; j++) {
                                SfxMapData data = new SfxMapData();
                                data.index = ConversionUtils.readUnsignedInteger(rawMap);
                                rawMap.read(data.unknown2);

                                eeEntrie.data[j] = data;
                            }
                        }
                    }
                }
            }

            if (rawMap.getFilePointer() != rawMap.length()) {
                throw new RuntimeException("Error parse data");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    @Override
    public String toString() {
        return "SfxMapFile{" + "file=" + file.getName()
                + ", entries=" + Arrays.toString(entries) + "\n}";
    }
}
