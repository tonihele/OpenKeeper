/*
 * Copyright (C) 2014-2021 OpenKeeper
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
package toniarts.openkeeper.tools.convert.textures;

import java.awt.image.BufferedImage;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 * Helps the texture conversion by creating BufferedImages from the converted
 * raw data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ImageUtil {

    /**
     * Creates a BufferedImage out of raw byte data
     *
     * @param width width of the image
     * @param height height of the image
     * @param hasAlpha whether the image should preserve alpha information
     * @param pixels the raw data, 4 unsigned bytes per pixel (R, G, B, A
     * respectively)
     * @return BufferedImage formed from the raw data
     */
    public static BufferedImage createImage(int width, int height, boolean hasAlpha, byte[] pixels) {
        BufferedImage img = new BufferedImage(width, height, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);

        // Draw the image, pixel by pixel
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int base = width * y * 4 + x * 4;
                int r = ConversionUtils.toUnsignedByte(pixels[base]);
                int g = ConversionUtils.toUnsignedByte(pixels[base + 1]);
                int b = ConversionUtils.toUnsignedByte(pixels[base + 2]);
                int a = ConversionUtils.toUnsignedByte(pixels[base + 3]);
                int col = (a << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(x, y, col);
            }
        }

        return img;
    }

}
