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
package toniarts.openkeeper.benchmark;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageMetricsTest {

    @Test
    void identicalImagesPassExactly() {
        BufferedImage a = solid(16, 16, 0x224466);
        BufferedImage b = solid(16, 16, 0x224466);
        ImageMetrics.Result r = ImageMetrics.compare(a, b, true);
        assertTrue(r.sizeMatch);
        assertEquals(0, r.mismatchedPixels);
        assertEquals(1.0, r.ssim, 1e-6);
        assertTrue(r.passes(0.0, 1.0, 0.0));
    }

    @Test
    void differentImagesFailSoftThresholds() {
        BufferedImage a = solid(16, 16, 0xffffff);
        BufferedImage b = solid(16, 16, 0x000000);
        ImageMetrics.Result r = ImageMetrics.compare(a, b, true);
        assertTrue(r.mismatchedPixels > 0);
        assertTrue(r.ssim < 0.5);
        assertFalse(r.passes(0.001, 0.99, 1.0));
    }

    @Test
    void sizeMismatchIsDetected() {
        ImageMetrics.Result r = ImageMetrics.compare(solid(8, 8, 0), solid(8, 9, 0), false);
        assertFalse(r.sizeMatch);
        assertFalse(r.passes(1, 0, 255));
    }

    private static BufferedImage solid(int w, int h, int rgb) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }
}
