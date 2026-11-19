/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.gui;

import static org.junit.jupiter.api.Assertions.*;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

class CursorTextureDataTest {
    @TempDir Path directory;

    @Test
    void concurrentCursorReadsLeaveTheCachedImageBufferPositionAlone() throws Exception {
        var folder = directory.resolve("Interface/Cursors");
        Files.createDirectories(folder);
        var pixels = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        pixels.setRGB(0, 0, 0x7f123456);
        ImageIO.write(pixels, "png", folder.resolve("test.png").toFile());
        var assets = new DesktopAssetManager(true);
        assets.registerLocator(directory.toString(), FileLocator.class);
        var image = assets.loadTexture("Interface/Cursors/test.png").getImage();
        image.getData(0).position(4);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Future<Cursor>>();
            for (int i = 0; i < 16; i++)
                tasks.add(
                        executor.submit(() -> {
                            assertTrue(start.await(5, TimeUnit.SECONDS));
                            return new Cursor(assets, "test.png", 1, 1);
                        }));
            start.countDown();
            for (var task : tasks) {
                var cursor = task.get(5, TimeUnit.SECONDS);
                assertEquals(16, cursor.getWidth());
                // The asset loader flips PNGs vertically; each cursor row is padded to 16.
                assertEquals(0x7f123456, cursor.getImagesData().get(3 * 16));
            }
        }
        assertEquals(4, image.getData(0).position());
    }
}
