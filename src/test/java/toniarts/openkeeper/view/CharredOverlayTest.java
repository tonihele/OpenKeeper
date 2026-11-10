/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.view;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CharredOverlayTest {

    @Test
    void reusableOverlaySampleMatchesOriginalBakeForScaledImages() {
        assertMatchesReference(Image.Format.RGBA8, ColorSpace.sRGB, 19, 13, 5, 7);
        assertMatchesReference(Image.Format.ABGR8, ColorSpace.Linear, 17, 11, 23, 3);
    }

    private static void assertMatchesReference(Image.Format format, ColorSpace colorSpace,
            int width, int height, int overlayWidth, int overlayHeight) {
        Image source = createImage(format, colorSpace, width, height, 17);
        Image overlay = createImage(format, colorSpace, overlayWidth, overlayHeight, 41);

        Image expected = bakeWithOriginalLoop(source, overlay);
        Texture baked = CharredOverlay.createBakedTexture(new Texture2D(source), new Texture2D(overlay));

        assertArrayEquals(bytes(expected), bytes(baked.getImage()));
    }

    private static Image bakeWithOriginalLoop(Image source, Image overlay) {
        int width = source.getWidth();
        int height = source.getHeight();
        Image result = new Image(Image.Format.RGBA8, width, height,
                BufferUtils.createByteBuffer(width * height * 4), source.getColorSpace());
        ImageRaster sourceRaster = ImageRaster.create(source);
        ImageRaster overlayRaster = ImageRaster.create(overlay);
        ImageRaster resultRaster = ImageRaster.create(result);
        ColorRGBA pixel = new ColorRGBA();
        for (int y = 0; y < height; y++) {
            int overlayY = y * overlay.getHeight() / height;
            for (int x = 0; x < width; x++) {
                sourceRaster.getPixel(x, y, pixel);
                ColorRGBA overlayPixel = overlayRaster.getPixel(x * overlay.getWidth() / width, overlayY);
                float alpha = overlayPixel.a;
                pixel.set(pixel.r * (1 - alpha) + overlayPixel.r * alpha,
                        pixel.g * (1 - alpha) + overlayPixel.g * alpha,
                        pixel.b * (1 - alpha) + overlayPixel.b * alpha,
                        pixel.a);
                resultRaster.setPixel(x, y, pixel);
            }
        }
        return result;
    }

    private static Image createImage(Image.Format format, ColorSpace colorSpace,
            int width, int height, long seed) {
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
        Random random = new Random(seed);
        for (int i = 0; i < width * height * 4; i++) {
            data.put((byte) random.nextInt(256));
        }
        data.flip();
        return new Image(format, width, height, data, colorSpace);
    }

    private static byte[] bytes(Image image) {
        ByteBuffer data = image.getData(0).duplicate();
        data.clear();
        byte[] result = new byte[data.remaining()];
        data.get(result);
        return result;
    }
}
