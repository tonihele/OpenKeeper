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
package toniarts.openkeeper.gui.nifty.jme;

import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import de.lessvoid.nifty.render.batch.spi.BatchRenderBackend;
import de.lessvoid.nifty.spi.render.RenderImage;
import de.lessvoid.nifty.tools.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HdImageRenderDeviceTest {

    private static final String ICON = "Textures/GUI/Icons/icon.png";
    private static final float EPSILON = 1e-6f;

    @TempDir
    Path originals;

    private final Map<String, BatchRenderBackend.Image> loadableImages = new HashMap<>();
    private final List<String> uploads = new ArrayList<>();
    private final List<float[]> quads = new ArrayList<>();
    private int nextTextureId = 1000;
    private HdImageRenderDevice renderDevice;

    @BeforeEach
    void setUp() throws IOException {
        Path original = originals.resolve(ICON);
        Files.createDirectories(original.getParent());
        ImageIO.write(new BufferedImage(16, 8, BufferedImage.TYPE_INT_ARGB), "png", original.toFile());

        BatchRenderConfiguration config = new BatchRenderConfiguration();
        config.disposeImagesBetweenScreens = false;
        renderDevice = new HdImageRenderDevice(recordingBackend(), config, originals);
    }

    @Test
    void hdImageIsDrawnWholeAtTheOriginalSizeFromATextureOfItsOwn() {
        loadableImages.put(ICON, image(64, 32));

        RenderImage image = renderDevice.createImage(ICON, true);
        draw(image);

        assertEquals(16, image.getWidth());
        assertEquals(8, image.getHeight());
        assertEquals(List.of("own texture"), uploads);
        float[] quad = quads.get(0);
        assertEquals(16f, quad[2]);
        assertEquals(8f, quad[3]);
        assertEquals(1f, quad[4] + quad[6], EPSILON, "reaches the right edge of the texture");
        assertEquals(1f, quad[5] + quad[7], EPSILON, "reaches the bottom edge of the texture");
    }

    @Test
    void hdImagePartIsGivenInOriginalPixels() {
        loadableImages.put(ICON, image(64, 32));
        RenderImage image = renderDevice.createImage(ICON, true);

        renderDevice.beginFrame();
        renderDevice.renderImage(image, 0, 0, 8, 8, 8, 0, 8, 8, Color.WHITE, 1f, 0, 0);
        renderDevice.endFrame();

        float[] quad = quads.get(0);
        assertEquals(8.5f / 16, quad[4], EPSILON, "starts at the middle of the texture");
        assertEquals(1f, quad[4] + quad[6], EPSILON);
    }

    @Test
    void hdImageIsCreatedOnce() {
        loadableImages.put(ICON, image(64, 32));

        assertSame(renderDevice.createImage(ICON, true), renderDevice.createImage(ICON, false));
    }

    @Test
    void originalSizeImageGoesToTheAtlas() {
        loadableImages.put(ICON, image(16, 8));

        RenderImage image = renderDevice.createImage(ICON, true);
        draw(image);

        assertEquals(16, image.getWidth());
        assertEquals(List.of("atlas"), uploads);
    }

    @Test
    void imageWithoutAnOriginalKeepsItsSize() {
        loadableImages.put("Interface/Other.png", image(64, 32));

        RenderImage image = renderDevice.createImage("Interface/Other.png", true);

        assertEquals(64, image.getWidth());
        assertEquals(32, image.getHeight());
    }

    private void draw(RenderImage image) {
        renderDevice.beginFrame();
        renderDevice.renderImage(image, 10, 20, image.getWidth(), image.getHeight(), Color.WHITE, 1f);
        renderDevice.endFrame();
    }

    private static BatchRenderBackend.Image image(int width, int height) {
        return new BatchRenderBackend.ByteBufferedImage(null, width, height);
    }

    private BatchRenderBackend recordingBackend() {
        return (BatchRenderBackend) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{BatchRenderBackend.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getWidth":
                            return 640;
                        case "getHeight":
                            return 480;
                        case "createTextureAtlas":
                            return nextTextureId++;
                        case "createNonAtlasTexture":
                            uploads.add("own texture");
                            return nextTextureId++;
                        case "existsNonAtlasTexture":
                            return true;
                        case "loadImage":
                            return args[0] instanceof String filename ? loadableImages.get(filename) : null;
                        case "addImageToAtlas":
                            uploads.add("atlas");
                            return null;
                        case "addQuad":
                            quads.add(new float[]{(float) args[0], (float) args[1], (float) args[2], (float) args[3],
                                (float) args[8], (float) args[9], (float) args[10], (float) args[11]});
                            return null;
                        case "render":
                            return 0;
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        case "equals":
                            return proxy == args[0];
                        default:
                            return null;
                    }
                });
    }

}
