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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.Utils;

/**
 * Dungeon Keeper II *Bank.map files. The map files contain sound playback
 * events of some sorts<br>
 * The file is LITTLE ENDIAN I might say<br>
 * File structure specifications by Tomasz Lis
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class BankMapFile {

    // Header
    private final short unknown1[]; // 20
    private final int unknown2;
    //
    private final File file;
    private final List<BankMapFileEntry> bankMapFileEntries;
    private final List<String> soundArchiveEntries;

    /**
     * Reads the *Bank.map file structure
     *
     * @param file the *Bank.map file to read
     */
    public BankMapFile(File file) {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawMap = new RandomAccessFile(file, "r")) {

            //Header
            unknown1 = new short[20];
            for (int x = 0; x < unknown1.length; x++) {
                unknown1[x] = (short) rawMap.readUnsignedByte();
            }
            unknown2 = Utils.readUnsignedInteger(rawMap);
            int entries = Utils.readUnsignedInteger(rawMap);

            //Read the entries
            bankMapFileEntries = new ArrayList<>(entries);
            for (int i = 0; i < entries; i++) {

                //Entries are 11 bytes of size
                //    long unknown00;
                //    long unknown04;
                //    char unknown08[3];
                BankMapFileEntry entry = new BankMapFileEntry();
                entry.setUnknown1(Utils.readUnsignedIntegerAsLong(rawMap));
                entry.setUnknown2(Utils.readUnsignedInteger(rawMap));
                short[] unknown3 = new short[3];
                for (int x = 0; x < unknown3.length; x++) {
                    unknown3[x] = (short) rawMap.readUnsignedByte();
                }
                entry.setUnknown3(unknown3);

                bankMapFileEntries.add(entry);
            }

            // After the entries there are names (that point to the SDT archives it seems)
            // It seems the amount is the same as entries
            soundArchiveEntries = new ArrayList<>(entries);
            for (int i = 0; i < entries; i++) {

                // 4 bytes = length of the name (including the null terminator)
                int length = Utils.readUnsignedInteger(rawMap);
                byte[] bytes = new byte[length];
                rawMap.read(bytes);
                soundArchiveEntries.add(Utils.bytesToString(bytes).trim());
            }

        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
