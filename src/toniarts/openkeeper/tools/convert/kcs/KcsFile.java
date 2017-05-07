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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.ConversionUtils;

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

    private final List<KcsEntry> kcsEntries;

    /**
     * Constructs a new Kcs file reader<br>
     * Reads the KCS file
     *
     * @param file the kcs file to read
     */
    public KcsFile(File file) {

        //Read the file
        try (RandomAccessFile rawKcs = new RandomAccessFile(file, "r")) {

            //Header
            int numOfEntries = ConversionUtils.readUnsignedInteger(rawKcs);
            rawKcs.skipBytes(12); // 12 bytes of emptiness?

            //Read the entries
            kcsEntries = new ArrayList<>(numOfEntries);
            for (int i = 0; i < numOfEntries; i++) {

                //Entries have 56 bytes in them
                KcsEntry entry = new KcsEntry();
                entry.setPosition(ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs));
                entry.setDirection(ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs));
                entry.setLeft(ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs));
                entry.setUp(ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs),
                        ConversionUtils.readFloat(rawKcs));
                entry.setLens(ConversionUtils.readFloat(rawKcs));
                entry.setNear(ConversionUtils.readFloat(rawKcs));
                kcsEntries.add(entry);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    public List<KcsEntry> getKcsEntries() {
        return kcsEntries;
    }
}
