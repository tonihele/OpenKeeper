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
package toniarts.openkeeper.tools.convert.spr;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;
import toniarts.openkeeper.tools.convert.ResourceReader;
import toniarts.openkeeper.tools.convert.spr.SprEntry.SprEntryHeader;

/**
 *
 * @author ArchDemon
 */
public class SprFile {

    private class SprHeader {

        protected String tag; // PSFB
        protected int framesCount;
    }

    public final static int[] PALETTE = getHalftonePalette();

    private final static String PSFB = "PSFB";
    private final SprHeader header;
    private File sprFile;
    private final List<SprEntry> sprites;

    private static final Logger LOGGER = Logger.getLogger(SprFile.class.getName());

    public SprFile(File file) {
        this.sprFile = file;

        try (IResourceReader data = new ResourceReader(sprFile)) {

            IResourceChunkReader dataReader = data.readChunk(8);
            header = new SprHeader();
            header.tag = dataReader.readString(4);

            if (!header.tag.equals(PSFB)) {
                LOGGER.log(Level.SEVERE, "This is not sprite file");
                throw new RuntimeException("This is not sprite file");
            }

            header.framesCount = dataReader.readUnsignedInteger();

            // Read the entries
            dataReader = data.readChunk(8 * header.framesCount);
            sprites = new ArrayList<>(header.framesCount);
            for (int i = 0; i < header.framesCount; i++) {
                SprEntry sprite = new SprEntry();
                SprEntryHeader entryHeader = sprite.new SprEntryHeader();
                entryHeader.width = dataReader.readUnsignedShort();
                entryHeader.height = dataReader.readUnsignedShort();
                entryHeader.offset = dataReader.readUnsignedIntegerAsLong();
                sprite.header = entryHeader;

                sprites.add(sprite);
            }

            // Read the image data for the entries
            long dataPos = data.getFilePointer();
            dataReader = data.readAll();
            for (SprEntry sprite : sprites) {
                sprite.readData(dataPos, dataReader);
            }
        } catch (Exception e) {

            //Fug
            throw new RuntimeException("Failed to read the file " + file.getName() + "!", e);
        }
    }

    private static int[] getHSBColors(int n) {
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = Color.getHSBColor((float) i / (n - 1), 0.85f, 1.0f).getRGB();
        }
        return result;
    }

    // Windows Halftone Palette 256-colors
    private static int[] getHalftonePalette() {
        int[] result = {
            0xFF000000, 0xFF800000, 0xFF008000, 0xFF808000, 0xFF000080, 0xFF800080, 0xFF008080, 0xFFC0C0C0,
            0xFF7F7F7F, 0xFFA6CAF0, 0xFF040404, 0xFF080808, 0xFF0C0C0C, 0xFF111111, 0xFF161616, 0xFF1C1C1C,
            0xFF222222, 0xFF292929, 0xFF555555, 0xFF4D4D4D, 0xFF424242, 0xFF393939, 0xFFFF7C80, 0xFFFF5050,
            0xFFD60093, 0xFFCCECFF, 0xFFEFD6C6, 0xFFE7E7D6, 0xFFADA990, 0xFF330000, 0xFF660000, 0xFF990000,
            0xFFCC0000, 0xFF003300, 0xFF333300, 0xFF663300, 0xFF993300, 0xFFCC3300, 0xFFFF3300, 0xFF006600,
            0xFF336600, 0xFF666600, 0xFF996600, 0xFFCC6600, 0xFFFF6600, 0xFF009900, 0xFF339900, 0xFF669900,
            0xFF999900, 0xFFCC9900, 0xFFFF9900, 0xFF00CC00, 0xFF33CC00, 0xFF66CC00, 0xFF99CC00, 0xFFCCCC00,
            0xFFFFCC00, 0xFF66FF00, 0xFF99FF00, 0xFFCCFF00, 0xFF000033, 0xFF330033, 0xFF660033, 0xFF990033,
            0xFFCC0033, 0xFFFF0033, 0xFF003333, 0xFF333333, 0xFF663333, 0xFF993333, 0xFFCC3333, 0xFFFF3333,
            0xFF006633, 0xFF336633, 0xFF666633, 0xFF996633, 0xFFCC6633, 0xFFFF6633, 0xFF009933, 0xFF339933,
            0xFF669933, 0xFF999933, 0xFFCC9933, 0xFFFF9933, 0xFF00CC33, 0xFF33CC33, 0xFF66CC33, 0xFF99CC33,
            0xFFCCCC33, 0xFFFFCC33, 0xFF33FF33, 0xFF66FF33, 0xFF99FF33, 0xFFCCFF33, 0xFFFFFF33, 0xFF000066,
            0xFF330066, 0xFF660066, 0xFF990066, 0xFFCC0066, 0xFFFF0066, 0xFF003366, 0xFF333366, 0xFF663366,
            0xFF993366, 0xFFCC3366, 0xFFFF3366, 0xFF006666, 0xFF336666, 0xFF666666, 0xFF996666, 0xFFCC6666,
            0xFF009966, 0xFF339966, 0xFF669966, 0xFF999966, 0xFFCC9966, 0xFFFF9966, 0xFF00CC66, 0xFF33CC66,
            0xFF99CC66, 0xFFCCCC66, 0xFFFFCC66, 0xFF00FF66, 0xFF33FF66, 0xFF99FF66, 0xFFCCFF66, 0xFFFF00CC,
            0xFFCC00FF, 0xFF009999, 0xFF993399, 0xFF990099, 0xFFCC0099, 0xFF000099, 0xFF333399, 0xFF660099,
            0xFFCC3399, 0xFFFF0099, 0xFF006699, 0xFF336699, 0xFF663399, 0xFF996699, 0xFFCC6699, 0xFFFF3399,
            0xFF339999, 0xFF669999, 0xFF999999, 0xFFCC9999, 0xFFFF9999, 0xFF00CC99, 0xFF33CC99, 0xFF66CC66,
            0xFF99CC99, 0xFFCCCC99, 0xFFFFCC99, 0xFF00FF99, 0xFF33FF99, 0xFF66CC99, 0xFF99FF99, 0xFFCCFF99,
            0xFFFFFF99, 0xFF0000CC, 0xFF330099, 0xFF6600CC, 0xFF9900CC, 0xFFCC00CC, 0xFF003399, 0xFF3333CC,
            0xFF6633CC, 0xFF9933CC, 0xFFCC33CC, 0xFFFF33CC, 0xFF0066CC, 0xFF3366CC, 0xFF666699, 0xFF9966CC,
            0xFFCC66CC, 0xFFFF6699, 0xFF0099CC, 0xFF3399CC, 0xFF6699CC, 0xFF9999CC, 0xFFCC99CC, 0xFFFF99CC,
            0xFF00CCCC, 0xFF33CCCC, 0xFF66CCCC, 0xFF99CCCC, 0xFFCCCCCC, 0xFFFFCCCC, 0xFF00FFCC, 0xFF33FFCC,
            0xFF66FF99, 0xFF99FFCC, 0xFFCCFFCC, 0xFFFFFFCC, 0xFF3300CC, 0xFF6600FF, 0xFF9900FF, 0xFF0033CC,
            0xFF3333FF, 0xFF6633FF, 0xFF9933FF, 0xFFCC33FF, 0xFFFF33FF, 0xFF0066FF, 0xFF3366FF, 0xFF6666CC,
            0xFF9966FF, 0xFFCC66FF, 0xFFFF66CC, 0xFF0099FF, 0xFF3399FF, 0xFF6699FF, 0xFF9999FF, 0xFFCC99FF,
            0xFFFF99FF, 0xFF00CCFF, 0xFF33CCFF, 0xFF66CCFF, 0xFF99CCFF, 0xFFCCCCFF, 0xFFFFCCFF, 0xFF33FFFF,
            0xFF66FFCC, 0xFF99FFFF, 0xFFCCFFFF, 0xFFFF6666, 0xFF66FF66, 0xFFFFFF66, 0xFF6666FF, 0xFFFF66FF,
            0xFF66FFFF, 0xFFA50021, 0xFF5F5F5F, 0xFF777777, 0xFF868686, 0xFF969696, 0xFFCBCBCB, 0xFFB2B2B2,
            0xFFD7D7D7, 0xFFDDDDDD, 0xFFE3E3E3, 0xFFEAEAEA, 0xFFF1F1F1, 0xFFF8F8F8, 0xFFE0E0E0, 0xFFA0A0A4,
            0xFF808080, 0xFFFF0000, 0xFF00FF00, 0xFFFFFF00, 0xFF0000FF, 0xFFFF00FF, 0xFF00FFFF, 0xFFFFFFFF};

        return result;
    }

    public void extract(String destination, String fileName) throws FileNotFoundException, IOException {
        int i = 0;
        for (SprEntry sprite : sprites) {
            OutputStream outputStream = new FileOutputStream(destination + File.separator + fileName + "#" + i++ + ".png");
            sprite.buffer.writeTo(outputStream);
        }
    }
}
