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
package toniarts.openkeeper.tools.convert.kcs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.ByteArrayResourceReader;
import toniarts.openkeeper.tools.convert.FileResourceReader;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;

/**
 * Stores the KCS file entries<br>
 * KCS file is a path information file, used to move cameras and spatials
 * around. Paths.wad contains a load of them<br>
 * The file is LITTLE ENDIAN I might say. I don't know it is relevant but DK II
 * run these at 30 FPS. So 150 entries equals 5 seconds of movement animation...
 * I reverse engineered this comparing the KCS files and the accompanied TXT
 * file<br>
 * Actual format reverse engineered by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class KcsFile {

    private List<KcsEntry> kcsEntries;

    /**
     * Constructs a new Kcs file reader<br>
     * Reads the KCS file
     *
     * @param file the kcs file to read
     */
    public KcsFile(Path file) {

        // Read the file
        try (IResourceReader rawKcs = new FileResourceReader(file)) {
            parseKcsFile(rawKcs);
        } catch (IOException e) {

            // Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    public KcsFile(byte[] data) {

        // Read the file
        try (IResourceReader rawKcs = new ByteArrayResourceReader(data)) {
            parseKcsFile(rawKcs);
        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to parse KCS data!", e);
        }
    }

    private void parseKcsFile(final IResourceReader rawKcs) throws IOException {

        // Header
        IResourceChunkReader rawKcsReader = rawKcs.readChunk(4);
        int numOfEntries = rawKcsReader.readUnsignedInteger();
        rawKcs.skipBytes(12); // 12 bytes of emptiness?

        // Read the entries
        rawKcsReader = rawKcs.readChunk(numOfEntries * 56);
        kcsEntries = new ArrayList<>(numOfEntries);
        for (int i = 0; i < numOfEntries; i++) {

            // Entries have 56 bytes in them
            KcsEntry entry = new KcsEntry();
            entry.setPosition(rawKcsReader.readFloat(), rawKcsReader.readFloat(), rawKcsReader.readFloat());
            entry.setDirection(rawKcsReader.readFloat(), rawKcsReader.readFloat(), rawKcsReader.readFloat());
            entry.setLeft(rawKcsReader.readFloat(), rawKcsReader.readFloat(), rawKcsReader.readFloat());
            entry.setUp(rawKcsReader.readFloat(), rawKcsReader.readFloat(), rawKcsReader.readFloat());
            entry.setLens(rawKcsReader.readFloat());
            entry.setNear(rawKcsReader.readFloat());
            kcsEntries.add(entry);
        }
    }

    public List<KcsEntry> getKcsEntries() {
        return kcsEntries;
    }
}
