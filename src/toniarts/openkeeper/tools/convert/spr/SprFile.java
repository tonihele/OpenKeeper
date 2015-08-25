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
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 *
 * @author ArchDemon
 */
public class SprFile {

    private class SprHeader {

        protected String magic; // PSFB
        protected int framesCount;
    }
    public final static int[] PALETTE = getRandomColors(256);
    private final String PSFB = "PSFB";
    private SprHeader header;
    private File sprFile;
    private SprEntry[] sprites;
    private static final Logger logger = Logger.getLogger(SprFile.class.getName());

    public SprFile(File file) {
        this.sprFile = file;

        try (RandomAccessFile data = new RandomAccessFile(sprFile, "r")) {

            header = new SprHeader();
            header.magic = ConversionUtils.bytesToString(data, 4);

            if (!header.magic.equals(PSFB)) {
                logger.log(Level.SEVERE, "This is not sprite file");
                throw new RuntimeException("This is not sprite file");
            }

            header.framesCount = ConversionUtils.readUnsignedInteger(data);
            sprites = new SprEntry[header.framesCount];

            for (int i = 0; i < sprites.length; i++) {
                SprEntry sprite = new SprEntry(data);
                sprites[i] = sprite;
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

    private static int[] getRandomColors(int n) {
        int[] result = new int[n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            result[i] = rand.nextInt();
        }
        return result;
    }

    public void extract(String destination, String fileName) throws FileNotFoundException, IOException {
        int i = 0;
        for (SprEntry sprite : sprites) {
            OutputStream outputStream = new FileOutputStream(destination + File.separator + fileName + i++ + ".png");
            sprite.buffer.writeTo(outputStream);
        }
    }
}
