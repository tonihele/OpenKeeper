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
package toniarts.openkeeper.tools.convert.textures.loadingscreens;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 * Loading and title screens carry the .444 file extension. They are packed with
 * some sort of TQI packing (but our movie decoder doesn't unpack them). Again,
 * little endian.<br>
 * Texture extraction & info code by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LoadingScreenFile {

    private final BufferedImage image;

    public LoadingScreenFile(ByteArrayOutputStream fileData) {

        ByteBuffer buf = ByteBuffer.wrap(fileData.toByteArray());
        buf.order(ByteOrder.LITTLE_ENDIAN);

        //Read the header
        int width = buf.getShort() & 0xFFFF;
        int height = buf.getShort() & 0xFFFF;
        boolean alphaFlag = ((buf.getInt() & 0xFFFFFFFFL) >> 7 != 0);

        // Read the data as unsigned ints
        int count = buf.remaining() / 4;
        long[] data = new long[count];
        for (int i = 0; i < count; i++) {
            data[i] = buf.getInt() & 0xFFFFFFFFL;
        }

        // Decompress to image
        image = decompressTexture(data, width, height, alphaFlag);
    }

    private BufferedImage decompressTexture(long[] data, int width, int height, boolean alphaFlag) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Decompress the texture
        byte[] pixels = new LoadingScreenTextureDecoder().dd_texture(data, width * (32 / 8)/*(bpp / 8 = bytes per pixel)*/, width, height, alphaFlag);

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

    /**
     * Get the decompressed RGBA image
     *
     * @return the image
     */
    public BufferedImage getImage() {
        return image;
    }
}
