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
import java.awt.image.DataBufferInt;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exact pixel + perceptual (SSIM) comparison for visual regression.
 */
public final class ImageMetrics {

    private ImageMetrics() {
    }

    public static final class Result {
        public final boolean sizeMatch;
        public final long mismatchedPixels;
        public final long totalPixels;
        public final double mismatchRatio;
        public final double meanAbsError;
        public final double ssim;
        public final BufferedImage diffImage;

        Result(boolean sizeMatch, long mismatchedPixels, long totalPixels,
                double mismatchRatio, double meanAbsError, double ssim, BufferedImage diffImage) {
            this.sizeMatch = sizeMatch;
            this.mismatchedPixels = mismatchedPixels;
            this.totalPixels = totalPixels;
            this.mismatchRatio = mismatchRatio;
            this.meanAbsError = meanAbsError;
            this.ssim = ssim;
            this.diffImage = diffImage;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sizeMatch", sizeMatch);
            map.put("mismatchedPixels", mismatchedPixels);
            map.put("totalPixels", totalPixels);
            map.put("mismatchRatio", round(mismatchRatio));
            map.put("meanAbsError", round(meanAbsError));
            map.put("ssim", round(ssim));
            return map;
        }

        /**
         * Pass if sizes match and either pixels match exactly or perceptual
         * metrics (SSIM + MAE) stay within thresholds. Pixel mismatch ratio is
         * expected to be high for live-sim checkpoints (moving creatures); it
         * is recorded for diagnostics but not required when SSIM/MAE pass.
         */
        public boolean passes(double maxMismatchRatio, double minSsim, double maxMae) {
            if (!sizeMatch) {
                return false;
            }
            if (mismatchedPixels == 0) {
                return true;
            }
            boolean perceptual = ssim >= minSsim && meanAbsError <= maxMae;
            boolean nearExact = mismatchRatio <= maxMismatchRatio && meanAbsError <= maxMae;
            return perceptual || nearExact;
        }
    }

    public static Result compare(BufferedImage reference, BufferedImage candidate, boolean buildDiff) {
        if (reference.getWidth() != candidate.getWidth() || reference.getHeight() != candidate.getHeight()) {
            return new Result(false, -1, -1, 1.0, 255.0, 0.0, null);
        }

        int w = reference.getWidth();
        int h = reference.getHeight();
        int[] ref = toIntRgb(reference);
        int[] cand = toIntRgb(candidate);
        long mismatched = 0;
        double absSum = 0.0;
        BufferedImage diff = buildDiff ? new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB) : null;
        int[] diffPix = diff != null ? ((DataBufferInt) diff.getRaster().getDataBuffer()).getData() : null;

        for (int i = 0; i < ref.length; i++) {
            int r1 = (ref[i] >> 16) & 0xff;
            int g1 = (ref[i] >> 8) & 0xff;
            int b1 = ref[i] & 0xff;
            int r2 = (cand[i] >> 16) & 0xff;
            int g2 = (cand[i] >> 8) & 0xff;
            int b2 = cand[i] & 0xff;
            int dr = Math.abs(r1 - r2);
            int dg = Math.abs(g1 - g2);
            int db = Math.abs(b1 - b2);
            absSum += (dr + dg + db) / 3.0;
            if (dr != 0 || dg != 0 || db != 0) {
                mismatched++;
            }
            if (diffPix != null) {
                // Amplify differences for diagnostics
                int heat = Math.min(255, (dr + dg + db) * 3);
                diffPix[i] = (heat << 16) | (heat << 8) | heat;
            }
        }

        double mae = absSum / ref.length;
        double ssim = ssim(ref, cand, w, h);
        return new Result(true, mismatched, ref.length, mismatched / (double) ref.length, mae, ssim, diff);
    }

    /**
     * Windowed SSIM on luma, 8×8 blocks (classic simplified form).
     */
    static double ssim(int[] a, int[] b, int width, int height) {
        final int win = 8;
        final double c1 = 6.5025;  // (0.01*255)^2
        final double c2 = 58.5225; // (0.03*255)^2
        double sum = 0.0;
        int windows = 0;
        for (int y = 0; y + win <= height; y += win) {
            for (int x = 0; x + win <= width; x += win) {
                double meanA = 0, meanB = 0;
                for (int j = 0; j < win; j++) {
                    for (int i = 0; i < win; i++) {
                        int idx = (y + j) * width + (x + i);
                        meanA += luma(a[idx]);
                        meanB += luma(b[idx]);
                    }
                }
                double n = win * win;
                meanA /= n;
                meanB /= n;
                double varA = 0, varB = 0, cov = 0;
                for (int j = 0; j < win; j++) {
                    for (int i = 0; i < win; i++) {
                        int idx = (y + j) * width + (x + i);
                        double da = luma(a[idx]) - meanA;
                        double db = luma(b[idx]) - meanB;
                        varA += da * da;
                        varB += db * db;
                        cov += da * db;
                    }
                }
                varA /= (n - 1);
                varB /= (n - 1);
                cov /= (n - 1);
                double numerator = (2 * meanA * meanB + c1) * (2 * cov + c2);
                double denominator = (meanA * meanA + meanB * meanB + c1) * (varA + varB + c2);
                sum += numerator / denominator;
                windows++;
            }
        }
        return windows == 0 ? 0.0 : sum / windows;
    }

    private static double luma(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static int[] toIntRgb(BufferedImage image) {
        BufferedImage rgb = image.getType() == BufferedImage.TYPE_INT_RGB
                ? image
                : new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        if (rgb != image) {
            rgb.getGraphics().drawImage(image, 0, 0, null);
        }
        return ((DataBufferInt) rgb.getRaster().getDataBuffer()).getData().clone();
    }

    private static double round(double v) {
        return Math.round(v * 1_000_000.0) / 1_000_000.0;
    }
}
