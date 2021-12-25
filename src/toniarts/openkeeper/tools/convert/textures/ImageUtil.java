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

import java.awt.Point;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;

/**
 * Helps the texture conversion by creating BufferedImages from the converted
 * raw data
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ImageUtil {

    public static BufferedImage createImage(int width, int height, boolean hasAlpha, byte[] pixels) {
        int len = 4 * width * height;
        if (!hasAlpha) {
            len = 3 * width * height;
        }

        // Draw the image
        ColorModel cm;
        SampleModel sampleModel;
        if (hasAlpha) {
            int[] offsets = {0, 1, 2, 3};
            sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, 4, 4 * width, offsets);
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 8}, true, false, ComponentColorModel.TRANSLUCENT, DataBuffer.TYPE_BYTE);
        } else {
            int[] offsets = {0, 1, 2};
            sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, width, height, 3, 3 * width, offsets);
            cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[]{8, 8, 8, 0}, false, false, ComponentColorModel.OPAQUE, DataBuffer.TYPE_BYTE);

            // The pixel data still contains the alpha pixels, strip them out
            if (pixels.length > len) {
                pixels = stripAlphaBytes(len, pixels);
            }
        }

        DataBufferByte dataBuffer = new DataBufferByte(pixels, len);

        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        BufferedImage img = new BufferedImage(cm, raster, false, null);

        return img;
    }

    private static byte[] stripAlphaBytes(int len, byte[] pixels) {
        ByteBuffer buffer = ByteBuffer.allocate(len);
        int i = 0;
        for (byte pixel : pixels) {
            i++;
            if (i == 4) {
                i = 0;
                continue;
            }
            buffer.put(pixel);
        }
        pixels = buffer.array();

        return pixels;
    }

}
