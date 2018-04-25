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
package toniarts.openkeeper.tools.convert.sound.sfx;

import java.io.File;
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
    private SfxMapFileEntry[] entries;

    public SfxMapFile(File file) {
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

            entries = new SfxMapFileEntry[count];
            for (int i = 0; i < entries.length; i++) {
                SfxMapFileEntry entry = new SfxMapFileEntry(this);
                count = ConversionUtils.readUnsignedInteger(rawMap);
                entry.groups = new SfxGroupEntry[count];
                entry.unknown_1 = ConversionUtils.readUnsignedInteger(rawMap);
                entry.unknown_2 = ConversionUtils.readUnsignedShort(rawMap);
                entry.unknown_3 = ConversionUtils.readUnsignedShort(rawMap);
                entry.minDistance = ConversionUtils.readFloat(rawMap);
                entry.maxDistance = ConversionUtils.readFloat(rawMap);
                entry.scale = ConversionUtils.readFloat(rawMap);

                entries[i] = entry;
            }

            for (SfxMapFileEntry entrie : entries) {
                for (int i = 0; i < entrie.groups.length; i++) {
                    SfxGroupEntry entry = new SfxGroupEntry(entrie);
                    entry.typeId = ConversionUtils.readUnsignedInteger(rawMap);
                    count = ConversionUtils.readUnsignedInteger(rawMap);
                    entry.entries = new SfxEEEntry[count];
                    entry.unknown_1 = ConversionUtils.readUnsignedInteger(rawMap);
                    entry.unknown_2 = ConversionUtils.readUnsignedInteger(rawMap);
                    entry.unknown_3 = ConversionUtils.readUnsignedInteger(rawMap);
                    entrie.groups[i] = entry;
                }
            }

            for (SfxMapFileEntry entrie : entries) {
                for (SfxGroupEntry eEntrie : entrie.groups) {

                    for (int i = 0; i < eEntrie.entries.length; i++) {
                        SfxEEEntry entry = new SfxEEEntry(eEntrie);

                        count = ConversionUtils.readUnsignedInteger(rawMap);
                        entry.sounds = new SfxSoundEntry[count];

                        count = ConversionUtils.readUnsignedInteger(rawMap);
                        if (count != 0) {
                            entry.data = new SfxData[count];
                        }

                        entry.end_pointer_position = ConversionUtils.readInteger(rawMap);
                        rawMap.read(entry.unknown);
                        entry.data_pointer_next = ConversionUtils.readInteger(rawMap); // readUnsignedInteger

                        eEntrie.entries[i] = entry;
                    }

                    for (SfxEEEntry eeEntrie : eEntrie.entries) {
                        for (int i = 0; i < eeEntrie.sounds.length; i++) {
                            SfxSoundEntry entry = new SfxSoundEntry(eeEntrie);
                            entry.index = ConversionUtils.readUnsignedInteger(rawMap);
                            entry.unknown_1 = ConversionUtils.readUnsignedInteger(rawMap);
                            entry.unknown_2 = ConversionUtils.readUnsignedInteger(rawMap);
                            entry.archiveId = ConversionUtils.readUnsignedInteger(rawMap);

                            eeEntrie.sounds[i] = entry;
                        }

                        if (eeEntrie.data != null) {
                            for (int j = 0; j < eeEntrie.data.length; j++) {
                                SfxData data = new SfxData();
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

    public SfxMapFileEntry[] getEntries() {
        return entries;
    }

    @Override
    public String toString() {
        return "SfxFile{" + "file=" + file.getName()
                + ", entries=" + Arrays.toString(entries) + "}";
    }
}
