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
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Matched microbenchmark for the charred overlay's per-pixel scratch object.
 * Run with {@code gradlew charredOverlayBenchmark}.
 */
public final class CharredOverlayAllocationBenchmark {

    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private static final int OVERLAY_WIDTH = 128;
    private static final int OVERLAY_HEIGHT = 128;
    private static final int WARMUP_BAKES = 3;
    private static final int BAKES_PER_SAMPLE = 2;
    private static final int SAMPLES = 7;
    private static volatile long checksumSink;

    private CharredOverlayAllocationBenchmark() {
    }

    public static void main(String[] args) {
        System.out.printf("Runtime: Java %s (%s)%n", System.getProperty("java.version"),
                System.getProperty("java.vm.name"));
        Image source = createImage(WIDTH, HEIGHT, 17);
        Image overlay = createImage(OVERLAY_WIDTH, OVERLAY_HEIGHT, 41);
        Texture diffuse = new Texture2D(source);
        Texture charred = new Texture2D(overlay);
        com.sun.management.ThreadMXBean allocationBean = allocationBean();

        for (int i = 0; i < WARMUP_BAKES; i++) {
            consume(bakeReference(diffuse, charred));
            consume(CharredOverlay.createBakedTexture(diffuse, charred));
        }

        List<Long> referenceNanos = new ArrayList<>(SAMPLES);
        List<Long> optimizedNanos = new ArrayList<>(SAMPLES);
        List<Long> referenceBytes = new ArrayList<>(SAMPLES);
        List<Long> optimizedBytes = new ArrayList<>(SAMPLES);
        Bake referenceBake = CharredOverlayAllocationBenchmark::bakeReference;
        Bake optimizedBake = CharredOverlay::createBakedTexture;
        for (int i = 0; i < SAMPLES; i++) {
            boolean optimizedFirst = (i & 1) != 0;
            Sample first = measure(optimizedFirst ? optimizedBake : referenceBake, diffuse, charred, allocationBean);
            Sample second = measure(optimizedFirst ? referenceBake : optimizedBake, diffuse, charred, allocationBean);
            if (optimizedFirst) {
                add(optimizedNanos, optimizedBytes, first);
                add(referenceNanos, referenceBytes, second);
            } else {
                add(referenceNanos, referenceBytes, first);
                add(optimizedNanos, optimizedBytes, second);
            }
        }

        System.out.printf("Charred overlay bake: %dx%d source, %dx%d overlay, %d bakes/sample, %d samples%n",
                WIDTH, HEIGHT, OVERLAY_WIDTH, OVERLAY_HEIGHT, BAKES_PER_SAMPLE, SAMPLES);
        report("Original allocating read", referenceNanos, referenceBytes);
        report("Reusable overlay color", optimizedNanos, optimizedBytes);
        System.out.printf("Allocation reduction: %.2f%%; median bake-time reduction: %.1f%%%n",
                percentReduction(median(referenceBytes), median(optimizedBytes)),
                percentReduction(median(referenceNanos), median(optimizedNanos)));
        System.out.printf("Output checksum: %d%n", checksumSink);
    }

    private static Sample measure(Bake bake, Texture diffuse, Texture overlay,
            com.sun.management.ThreadMXBean allocationBean) {
        List<Texture> retainedOutputs = new ArrayList<>(BAKES_PER_SAMPLE);
        long threadId = Thread.currentThread().threadId();
        long allocatedBefore = allocationBean.getThreadAllocatedBytes(threadId);
        long start = System.nanoTime();
        for (int i = 0; i < BAKES_PER_SAMPLE; i++) {
            retainedOutputs.add(bake.run(diffuse, overlay));
        }
        long elapsed = System.nanoTime() - start;
        long allocated = allocationBean.getThreadAllocatedBytes(threadId) - allocatedBefore;
        for (Texture texture : retainedOutputs) {
            consume(texture);
        }
        return new Sample(elapsed / BAKES_PER_SAMPLE, allocated / BAKES_PER_SAMPLE);
    }

    private static void report(String label, List<Long> nanos, List<Long> bytes) {
        System.out.printf("%-26s median %8.3f ms/bake, %8.1f KiB allocated/bake%n", label,
                median(nanos) / 1_000_000.0, median(bytes) / 1024.0);
    }

    private static void add(List<Long> nanos, List<Long> bytes, Sample sample) {
        nanos.add(sample.nanosPerBake());
        bytes.add(sample.bytesPerBake());
    }

    private static long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        return sorted.get(sorted.size() / 2);
    }

    private static double percentReduction(long baseline, long candidate) {
        return 100.0 * (baseline - candidate) / baseline;
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        var bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof com.sun.management.ThreadMXBean threadBean
                && threadBean.isThreadAllocatedMemorySupported()) {
            threadBean.setThreadAllocatedMemoryEnabled(true);
            if (!threadBean.isThreadAllocatedMemoryEnabled()) {
                throw new IllegalStateException("Thread allocation tracking is disabled");
            }
            return threadBean;
        }
        throw new IllegalStateException("Thread allocation tracking is unsupported on this runtime");
    }

    private static Texture bakeReference(Texture diffuse, Texture charred) {
        Image source = diffuse.getImage();
        Image overlay = charred.getImage();
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
        Texture texture = new Texture2D(result);
        texture.setWrap(Texture.WrapMode.Repeat);
        texture.setMinFilter(diffuse.getMinFilter());
        texture.setMagFilter(diffuse.getMagFilter());
        return texture;
    }

    private static Image createImage(int width, int height, long seed) {
        ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
        Random random = new Random(seed);
        for (int i = 0; i < width * height * 4; i++) {
            data.put((byte) random.nextInt(256));
        }
        data.flip();
        return new Image(Image.Format.RGBA8, width, height, data, ColorSpace.sRGB);
    }

    private static void consume(Texture texture) {
        ByteBuffer data = texture.getImage().getData(0).duplicate();
        data.clear();
        long checksum = 0;
        while (data.hasRemaining()) {
            checksum += Byte.toUnsignedInt(data.get());
        }
        checksumSink = checksum;
    }

    @FunctionalInterface
    private interface Bake {

        Texture run(Texture diffuse, Texture charred);
    }

    private record Sample(long nanosPerBake, long bytesPerBake) {
    }
}
