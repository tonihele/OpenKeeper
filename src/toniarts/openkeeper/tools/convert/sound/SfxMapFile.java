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

    private class SfxMapEntry { // 24 bytes

        final static byte SIZE = 24;
        protected int quantity;
        protected byte[] unknown = new byte[8];
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

    private class SfxMapEEntry { // 20 bytes

        final static byte SIZE = 20;
        protected int unknown;
        protected int quantity;
        protected byte[] unknowns = new byte[12];
        protected SfxMapEEEntry[] entries;

        @Override
        public String toString() {
            return "\n\t\tSfxMapEEntry{" + "entries=" + Arrays.toString(entries) + "}\n\t\t";
        }
    }

    private class SfxMapEEEntry { // 42 bytes

        final static byte SIZE = 42;
        final static byte SIZE_ADDITIONAL = 8;
        protected int quantity;
        protected int shift;  // if shift need some magic
        protected int end_pointer_position; // I think not needed
        protected byte[] unknown = new byte[26];
        protected int data_pointer_next; // I think not needed
        protected SfxMapEEEEntry[] entries;
        // data_pointer_next + shift * 8 = file_pointer_position of next SfxMapEntry
        protected SfxMapData[] data; // why ?????????

        @Override
        public String toString() {
            return "\n\t\t\tSfxMapEEEntry{" + "entries=" + Arrays.toString(entries)
                    + ", data=" + Arrays.toString(data) + "}\n\t\t\t";
        }
    }

    private class SfxMapEEEEntry { // 16 bytes

        final static byte SIZE = 16;
        protected int index;
        protected byte[] unknown = new byte[8];
        // if (edi_2) { need some magic; }
        protected int edi_2;

        @Override
        public String toString() {
            return "\n\t\t\t\tSfxMapEEEEntry{" + "index=" + index
                    + ", edi_2=" + edi_2 + "}\n\t\t\t\t";
        }
    }

    private class SfxMapData { // 8 bytes
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
            int quantity = ConversionUtils.readUnsignedInteger(rawMap);

            entries = new SfxMapEntry[quantity];
            for (int i = 0; i < entries.length; i++) {
                SfxMapEntry entry = new SfxMapEntry();
                entry.quantity = ConversionUtils.readUnsignedInteger(rawMap);
                rawMap.read(entry.unknown);
                entry.minDistance = ConversionUtils.readFloat(rawMap);
                entry.maxDistance = ConversionUtils.readFloat(rawMap);
                entry.scale = ConversionUtils.readFloat(rawMap);
                entry.entries = new SfxMapEEntry[entry.quantity];
                entries[i] = entry;
            }

            for (SfxMapEntry entrie : entries) {
                for (int j = 0; j < entrie.entries.length; j++) {
                    SfxMapEEntry entry = new SfxMapEEntry();
                    entry.unknown = ConversionUtils.readInteger(rawMap);  // readUnsignedInteger
                    entry.quantity = ConversionUtils.readUnsignedInteger(rawMap);
                    rawMap.read(entry.unknowns);
                    entry.entries = new SfxMapEEEntry[entry.quantity];
                    entrie.entries[j] = entry;
                }
            }

            for (SfxMapEntry entrie : entries) {
                for (SfxMapEEntry eEntrie : entrie.entries) {

                    for (int k = 0; k < eEntrie.entries.length; k++) {
                        SfxMapEEEntry entry = new SfxMapEEEntry();
                        entry.quantity = ConversionUtils.readUnsignedInteger(rawMap);
                        entry.shift = ConversionUtils.readInteger(rawMap); // readUnsignedInteger
                        entry.end_pointer_position = ConversionUtils.readInteger(rawMap);
                        rawMap.read(entry.unknown);
                        entry.data_pointer_next = ConversionUtils.readInteger(rawMap); // readUnsignedInteger
                        entry.entries = new SfxMapEEEEntry[entry.quantity];
                        eEntrie.entries[k] = entry;
                    }

                    for (SfxMapEEEntry eeEntrie : eEntrie.entries) {
                        for (int n = 0; n < eeEntrie.entries.length; n++) {
                            SfxMapEEEEntry entry = new SfxMapEEEEntry();
                            entry.index = ConversionUtils.readUnsignedInteger(rawMap);
                            rawMap.read(entry.unknown);
                            entry.edi_2 = ConversionUtils.readUnsignedInteger(rawMap);
                            eeEntrie.entries[n] = entry;
                        }

                        if (eeEntrie.shift != 0) {
                            eeEntrie.data = new SfxMapData[eeEntrie.shift];
                            for (int l = 0; l < eeEntrie.data.length; l++) {
                                SfxMapData data = new SfxMapData();
                                data.index = ConversionUtils.readUnsignedInteger(rawMap);
                                rawMap.read(data.unknown2);

                                eeEntrie.data[l] = data;
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
