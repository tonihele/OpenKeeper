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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import toniarts.openkeeper.tools.convert.textures.ImageUtil;

/**
 * Loading and title screens carry the .444 file extension. They are packed with
 * some sort of TQI packing (but our movie decoder doesn't unpack them). Again,
 * little endian.<br>
 * Texture extraction & info code by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class LoadingScreenFile {

    private final BufferedImage image;

    public LoadingScreenFile(byte[] fileData) {

        ByteBuffer buf = ByteBuffer.wrap(fileData);
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

        // Decompress the texture
        byte[] pixels = new LoadingScreenTextureDecoder().dd_texture(data, width * (32 / 8)/*(bpp / 8 = bytes per pixel)*/, width, height, alphaFlag);

        return ImageUtil.createImage(width, height, alphaFlag, pixels);
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
