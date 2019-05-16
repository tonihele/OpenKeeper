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
import java.util.Arrays;
import toniarts.openkeeper.tools.convert.IResourceReader;
import toniarts.openkeeper.tools.convert.ResourceReader;

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
        try (IResourceReader rawMap = new ResourceReader(file)) {
            //Header
            int[] check = new int[] {
                rawMap.readInteger(),
                rawMap.readInteger(),
                rawMap.readInteger(),
                rawMap.readInteger()
            };
            for (int i = 0; i < HEADER_ID.length; i++) {
                if (check[i] != HEADER_ID[i]) {
                    throw new RuntimeException(file.getName() + ": The file header is not valid");
                }
            }

            unknown_1 = rawMap.readInteger();
            unknown_2 = rawMap.readInteger();
            int count = rawMap.readUnsignedInteger();

            entries = new SfxMapFileEntry[count];
            for (int i = 0; i < entries.length; i++) {
                SfxMapFileEntry entry = new SfxMapFileEntry(this);
                count = rawMap.readUnsignedInteger();
                entry.groups = new SfxGroupEntry[count];
                entry.unknown_1 = rawMap.readUnsignedInteger();
                entry.unknown_2 = rawMap.readUnsignedShort();
                entry.unknown_3 = rawMap.readUnsignedShort();
                entry.minDistance = rawMap.readFloat();
                entry.maxDistance = rawMap.readFloat();
                entry.scale = rawMap.readFloat();

                entries[i] = entry;
            }

            for (SfxMapFileEntry entrie : entries) {
                for (int i = 0; i < entrie.groups.length; i++) {
                    SfxGroupEntry entry = new SfxGroupEntry(entrie);
                    entry.typeId = rawMap.readUnsignedInteger();
                    count = rawMap.readUnsignedInteger();
                    entry.entries = new SfxEEEntry[count];
                    entry.unknown_1 = rawMap.readUnsignedInteger();
                    entry.unknown_2 = rawMap.readUnsignedInteger();
                    entry.unknown_3 = rawMap.readUnsignedInteger();
                    entrie.groups[i] = entry;
                }
            }

            for (SfxMapFileEntry entrie : entries) {
                for (SfxGroupEntry eEntrie : entrie.groups) {

                    for (int i = 0; i < eEntrie.entries.length; i++) {
                        SfxEEEntry entry = new SfxEEEntry(eEntrie);

                        count = rawMap.readUnsignedInteger();
                        entry.sounds = new SfxSoundEntry[count];

                        count = rawMap.readUnsignedInteger();
                        if (count != 0) {
                            entry.data = new SfxData[count];
                        }

                        entry.end_pointer_position = rawMap.readInteger();
                        entry.unknown = rawMap.read(entry.unknown.length);
                        entry.data_pointer_next = rawMap.readInteger(); // readUnsignedInteger

                        eEntrie.entries[i] = entry;
                    }

                    for (SfxEEEntry eeEntrie : eEntrie.entries) {
                        for (int i = 0; i < eeEntrie.sounds.length; i++) {
                            SfxSoundEntry entry = new SfxSoundEntry(eeEntrie);
                            entry.index = rawMap.readUnsignedInteger();
                            entry.unknown_1 = rawMap.readUnsignedInteger();
                            entry.unknown_2 = rawMap.readUnsignedInteger();
                            entry.archiveId = rawMap.readUnsignedInteger();

                            eeEntrie.sounds[i] = entry;
                        }

                        if (eeEntrie.data != null) {
                            for (int j = 0; j < eeEntrie.data.length; j++) {
                                SfxData data = new SfxData();
                                data.index = rawMap.readUnsignedInteger();
                                data.unknown2 = rawMap.read(data.unknown2.length);

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
