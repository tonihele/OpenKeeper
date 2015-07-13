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
import toniarts.openkeeper.tools.convert.Utils;

/**
 *
 * @author ArchDemon
 */
public class SfxMapFile {

    final static int dword_674038 = 0xE9612C00;
    final static int dword_67403C = 0x11D231D0;
    final static int dword_674040 = 0xB00009B4;
    final static int dword_674044 = 0x03F293C9;
    private final File file;
    private SfxMapHeader header;
    private SfxMapHeaderEntries[] entries;

    private class SfxMapHeader { // 20 bytes + 8 bytes

        protected int unknown_1;
        protected int unknown_2;
        protected int quantity;

        @Override
        public String toString() {
            return "SfxMapHeader{" + "quantity=" + quantity + '}';
        }
    }

    private class SfxMapHeaderEntries { // 24 bytes

        final static byte SIZE = 24;
        protected int quantity;
        protected byte[] unknown = new byte[20];
        protected SfxMapHeaderEntryHeader[] entries;

        @Override
        public String toString() {
            return "SfxMapHeaderEntries{" + "quantity=" + quantity + ", entries=" + entries + '}';
        }
    }

    private class SfxMapHeaderEntryHeader { // 20 bytes

        final static byte SIZE = 20;
        protected int unknown;
        protected int quantity;
        protected byte[] unknowns = new byte[12];
        protected SfxMapHeaderEntry[] entries;

        @Override
        public String toString() {
            return "SfxMapHeaderEntryHeader{" + "quantity=" + quantity + ", entries=" + entries + '}';
        }
    }

    private class SfxMapHeaderEntry { // 42 bytes

        final static byte SIZE = 42;
        final static byte SIZE_ADDITIONAL = 8;
        protected int quantity;
        protected int shift;  // if shift need some magic
        protected int end_pointer_position; // I think not needed
        protected byte[] unknown = new byte[26];
        protected int data_pointer_next; // I think not needed
        protected SfxMapEntry[] entries;
        // data_pointer_next + shift * 8 = file_pointer_position of next SfxMapEntry
        protected byte[] additionalData; // why ?????????

        @Override
        public String toString() {
            return "SfxMapHeaderEntry{" + "quantity=" + quantity + ", shift=" + shift + ", entries=" + entries + '}';
        }
    }

    private class SfxMapEntry { // 16 bytes

        final static byte SIZE = 16;
        protected int quantity;
        protected byte[] unknown = new byte[8];
        // if (edi_2) { need some magic; }
        protected int edi_2;

        @Override
        public String toString() {
            return "SfxMapEntry{" + "quantity=" + quantity + ", edi_2=" + edi_2 + '}';
        }
    }

    public SfxMapFile(File file) throws FileNotFoundException {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawMap = new RandomAccessFile(file, "r")) {
            //Header
            header = new SfxMapHeader();

            int[] check = new int[4];
            check[0] = Utils.readInteger(rawMap);
            check[1] = Utils.readInteger(rawMap);
            check[2] = Utils.readInteger(rawMap);
            check[3] = Utils.readInteger(rawMap);

            if (check[0] != dword_674038 || check[1] != dword_67403C
                    || check[2] != dword_674040 || check[3] != dword_674044) {
                throw new RuntimeException("The file header is not valid");
            }

            header.unknown_1 = Utils.readInteger(rawMap);
            if (header.unknown_1 != 0) {
                System.out.println("Header unknown_1 != 0. Value = " + header.unknown_1);
            }
            header.unknown_2 = Utils.readInteger(rawMap);
            if (header.unknown_2 != 0) {
                System.out.println("Header unknown_2 != 0. Value = " + header.unknown_2);
            }
            header.quantity = Utils.readUnsignedInteger(rawMap);

            entries = new SfxMapHeaderEntries[header.quantity];
            for (int i = 0; i < entries.length; i++) {
                SfxMapHeaderEntries entry = new SfxMapHeaderEntries();
                entry.quantity = Utils.readUnsignedInteger(rawMap);
                rawMap.read(entry.unknown);
                entry.entries = new SfxMapHeaderEntryHeader[entry.quantity];
                entries[i] = entry;
            }

            for (int i = 0; i < entries.length; i++) {
                for (int j = 0; j < entries[i].entries.length; j++) {
                    SfxMapHeaderEntryHeader entry = new SfxMapHeaderEntryHeader();
                    entry.unknown = Utils.readInteger(rawMap);  // readUnsignedInteger
                    entry.quantity = Utils.readUnsignedInteger(rawMap);
                    rawMap.read(entry.unknowns);

                    entry.entries = new SfxMapHeaderEntry[entry.quantity];
                    entries[i].entries[j] = entry;
                }
            }

            for (int i = 0; i < entries.length; i++) {
                for (int j = 0; j < entries[i].entries.length; j++) {
                    for (int k = 0; k < entries[i].entries[j].entries.length; k++) {

                        SfxMapHeaderEntry entry = new SfxMapHeaderEntry();
                        entry.quantity = Utils.readUnsignedInteger(rawMap);
                        entry.shift = Utils.readInteger(rawMap); // readUnsignedInteger
                        entry.end_pointer_position = Utils.readInteger(rawMap);
                        rawMap.read(entry.unknown);
                        entry.data_pointer_next = Utils.readInteger(rawMap); // readUnsignedInteger
                        entry.entries = new SfxMapEntry[entry.quantity];

                        entries[i].entries[j].entries[k] = entry;
                    }

                    for (int k = 0; k < entries[i].entries[j].entries.length; k++) {
                        for (int n = 0; n < entries[i].entries[j].entries[k].entries.length; n++) {
                            SfxMapEntry entry = new SfxMapEntry();
                            entry.quantity = Utils.readUnsignedInteger(rawMap);
                            rawMap.read(entry.unknown);
                            entry.edi_2 = Utils.readUnsignedInteger(rawMap);

                            entries[i].entries[j].entries[k].entries[n] = entry;
                        }
                        int shift = entries[i].entries[j].entries[k].shift;
                        if (shift != 0) {
                            entries[i].entries[j].entries[k].additionalData = new byte[SfxMapHeaderEntry.SIZE_ADDITIONAL * shift];
                            rawMap.read(entries[i].entries[j].entries[k].additionalData);
                        }
                    }
                }
            }
            /*for (int i = 0; i < entries.length; i++) {
             for (int j = 0; j < entries[i].entries.length; j++) {
             for (int k = 0; k < entries[i].entries[j].entries.length; k++) {
             for (int n = 0; n < entries[i].entries[j].entries[k].entries.length; n++) {

             SfxMapEntry entry = new SfxMapEntry();
             entry.quantity = Utils.readUnsignedInteger(rawMap);
             rawMap.read(entry.unknown);
             entry.edi_2 = Utils.readUnsignedInteger(rawMap);

             entries[i].entries[j].entries[k].entries[n] = entry;
             }
             int shift = entries[i].entries[j].entries[k].shift;
             if (shift != 0 ) {
             entries[i].entries[j].entries[k].additionalData = new byte[SfxMapHeaderEntry.SIZE_ADDITIONAL * shift];
             rawMap.read(entries[i].entries[j].entries[k].additionalData);
             }
             }
             }
             }*/
            if (rawMap.getFilePointer() != rawMap.length()) {
                throw new RuntimeException("Error parse data");
            }

        } catch (IOException e) {
            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    @Override
    public String toString() {
        return "SfxMapFile{" + "file=" + file.getName() + ", quantity=" + header.quantity + ", entries=" + entries + '}';
    }
}
