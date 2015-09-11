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
package toniarts.openkeeper.tools.convert.bf4;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.bf4.Bf4Entry.FontEntryFlag;

/**
 * Reads the Dungeon Keeper 2 BF4 files, bitmap fonts that is<br>
 * Format reverse engineered by:
 * <li>George Gensure</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Bf4File {

    private static final String BF4_HEADER_IDENTIFIER = "F4FB";
    private final List<Bf4Entry> entries;

    /**
     * Constructs a new BF4 file reader Reads the BF4 file structure
     *
     * @param file the bf4 file to read
     */
    public Bf4File(File file) {

        // Read the file
        try (RandomAccessFile rawBf4 = new RandomAccessFile(file, "r")) {

            // Check the header
            byte[] header = new byte[4];
            rawBf4.read(header);
            if (!BF4_HEADER_IDENTIFIER.equals(ConversionUtils.bytesToString(header))) {
                throw new RuntimeException("Header should be " + BF4_HEADER_IDENTIFIER + " and it was " + header + "! Cancelling!");
            }
            short maxWidth = ConversionUtils.toUnsignedByte(rawBf4.readByte()); // This is know to be bogus value
            short maxHeight = ConversionUtils.toUnsignedByte(rawBf4.readByte());
            int offsetsCount = ConversionUtils.readUnsignedShort(rawBf4);

            // Read the offsets
            List<Integer> offsets = new ArrayList<>(offsetsCount);
            for (int i = 0; i < offsetsCount; i++) {
                offsets.add(ConversionUtils.readUnsignedInteger(rawBf4));
            }

            // Read the font entries
            entries = new ArrayList<>(offsetsCount);
            for (Integer offset : offsets) {
                rawBf4.seek(offset);
                entries.add(readFontEntry(rawBf4));
            }
        } catch (IOException e) {

            // Fug
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
    }

    /**
     * Reads a single font entry from the file
     *
     * @param rawBf4 the file
     * @return the font entry
     * @throws IOException may fail
     */
    private Bf4Entry readFontEntry(RandomAccessFile rawBf4) throws IOException {
        Bf4Entry entry = new Bf4Entry();
        byte[] bytes = new byte[2];
        rawBf4.read(bytes);
        entry.setCharacter(ConversionUtils.bytesToStringUtf16(bytes).charAt(0));
        entry.setUnknown1(ConversionUtils.readUnsignedShort(rawBf4));
        entry.setDataSize(ConversionUtils.readInteger(rawBf4));
        entry.setTotalSize(ConversionUtils.readUnsignedInteger(rawBf4));
        entry.setFlag(ConversionUtils.parseFlagValue(rawBf4.readUnsignedByte(), FontEntryFlag.class));
        entry.setUnknown2(ConversionUtils.toUnsignedByte(rawBf4.readByte()));
        entry.setUnknown3(ConversionUtils.toUnsignedByte(rawBf4.readByte()));
        entry.setUnknown4(ConversionUtils.toUnsignedByte(rawBf4.readByte()));
        entry.setWidth(ConversionUtils.readUnsignedShort(rawBf4));
        entry.setHeight(ConversionUtils.readUnsignedShort(rawBf4));
        entry.setOffsetX(rawBf4.readByte());
        entry.setOffsetY(rawBf4.readByte());
        entry.setOuterWidth(ConversionUtils.readShort(rawBf4));
        bytes = new byte[entry.getDataSize()];
        rawBf4.read(bytes);
        entry.setImageData(bytes);
        return entry;
    }
}
