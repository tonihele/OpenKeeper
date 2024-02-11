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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import toniarts.openkeeper.tools.convert.BufferedResourceReader;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;

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

    private final Path file;
    private final SfxMapFileEntry[] entries;

    public SfxMapFile(Path file) {
        this.file = file;

        // Read the file
        try (IResourceReader rawMap = new BufferedResourceReader(file)) {

            // Header
            IResourceChunkReader rawMapReader = rawMap.readChunk(28);
            int[] check = new int[] {
                rawMapReader.readInteger(),
                rawMapReader.readInteger(),
                rawMapReader.readInteger(),
                rawMapReader.readInteger()
            };
            for (int i = 0; i < HEADER_ID.length; i++) {
                if (check[i] != HEADER_ID[i]) {
                    throw new RuntimeException(file.toString() + ": The file header is not valid");
                }
            }

            unknown_1 = rawMapReader.readInteger();
            unknown_2 = rawMapReader.readInteger();
            int count = rawMapReader.readUnsignedInteger();

            rawMapReader = rawMap.readChunk(count * 24);
            int groups = 0;
            entries = new SfxMapFileEntry[count];
            for (int i = 0; i < entries.length; i++) {
                SfxMapFileEntry entry = new SfxMapFileEntry(this);
                count = rawMapReader.readUnsignedInteger();
                groups += count;
                entry.groups = new SfxGroupEntry[count];
                entry.unknown_1 = rawMapReader.readUnsignedInteger();
                entry.unknown_2 = rawMapReader.readUnsignedShort();
                entry.unknown_3 = rawMapReader.readUnsignedShort();
                entry.minDistance = rawMapReader.readFloat();
                entry.maxDistance = rawMapReader.readFloat();
                entry.scale = rawMapReader.readFloat();

                entries[i] = entry;
            }

            rawMapReader = rawMap.readChunk(groups * 20);
            for (SfxMapFileEntry entrie : entries) {
                for (int i = 0; i < entrie.groups.length; i++) {
                    SfxGroupEntry entry = new SfxGroupEntry(entrie);
                    entry.typeId = rawMapReader.readUnsignedInteger();
                    count = rawMapReader.readUnsignedInteger();
                    entry.entries = new SfxEEEntry[count];
                    entry.unknown_1 = rawMapReader.readUnsignedInteger();
                    entry.unknown_2 = rawMapReader.readUnsignedInteger();
                    entry.unknown_3 = rawMapReader.readUnsignedInteger();
                    entrie.groups[i] = entry;
                }
            }

            rawMapReader = rawMap.readAll();
            for (SfxMapFileEntry entrie : entries) {
                for (SfxGroupEntry eEntrie : entrie.groups) {

                    for (int i = 0; i < eEntrie.entries.length; i++) {
                        SfxEEEntry entry = new SfxEEEntry(eEntrie);

                        count = rawMapReader.readUnsignedInteger();
                        entry.sounds = new SfxSoundEntry[count];

                        count = rawMapReader.readUnsignedInteger();
                        if (count != 0) {
                            entry.data = new SfxData[count];
                        }

                        entry.end_pointer_position = rawMapReader.readInteger();
                        entry.unknown = rawMapReader.read(entry.unknown.length);
                        entry.data_pointer_next = rawMapReader.readInteger(); // readUnsignedInteger

                        eEntrie.entries[i] = entry;
                    }

                    for (SfxEEEntry eeEntrie : eEntrie.entries) {
                        for (int i = 0; i < eeEntrie.sounds.length; i++) {
                            SfxSoundEntry entry = new SfxSoundEntry(eeEntrie);
                            entry.index = rawMapReader.readUnsignedInteger();
                            entry.unknown_1 = rawMapReader.readUnsignedInteger();
                            entry.unknown_2 = rawMapReader.readUnsignedInteger();
                            entry.archiveId = rawMapReader.readUnsignedInteger();

                            eeEntrie.sounds[i] = entry;
                        }

                        if (eeEntrie.data != null) {
                            for (int j = 0; j < eeEntrie.data.length; j++) {
                                SfxData data = new SfxData();
                                data.index = rawMapReader.readUnsignedInteger();
                                data.unknown2 = rawMapReader.read(data.unknown2.length);

                                eeEntrie.data[j] = data;
                            }
                        }
                    }
                }
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
        return "SfxFile{" + "file=" + file.toString()
                + ", entries=" + Arrays.toString(entries) + "}";
    }
}
