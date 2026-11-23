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
import de.lessvoid.nifty.render.batch.BatchRenderImage;
import de.lessvoid.nifty.render.batch.spi.BatchRenderBackend;
import de.lessvoid.nifty.spi.render.RenderImage;
import de.lessvoid.nifty.tools.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.gui.nifty.jme.HdImageNiftyJmeDisplay.HdImageRenderDevice;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HdImageNiftyJmeDisplayTest {

    private static final String HD = "Textures/GUI/Windows/Bar-00.png";
    private static final String OTHER = "Textures/GUI/Windows/Other.png";
    private static final int ATLAS = 256;

    private final List<float[]> quads = new ArrayList<>();
    private HdImageRenderDevice renderDevice;

    @BeforeEach
    void setUp() {
        BatchRenderConfiguration config = new BatchRenderConfiguration();
        config.atlasWidth = ATLAS;
        config.atlasHeight = ATLAS;
        config.disposeImagesBetweenScreens = false;
        renderDevice = new HdImageRenderDevice(recordingBackend(), config, Set.of(HD.toLowerCase()));
    }

    @Test
    void cutOfAnHdImageIsTakenAtTheHdScale() {
        RenderImage image = renderDevice.createImage(HD, true);

        float[] quad = draw(image, 4, 2, 8, 8);

        assertEquals(8f, quad[2], "drawn at the cut's size");
        assertEquals(((BatchRenderImage) image).getX() + 16, pixel(quad[4]));
        assertEquals(((BatchRenderImage) image).getY() + 8, pixel(quad[5]));
        assertEquals(32 - 1, pixel(quad[6]));
        assertEquals(32 - 1, pixel(quad[7]));
    }

    @Test
    void wholeHdImageIsTakenWhole() {
        RenderImage image = renderDevice.createImage(HD, true);

        float[] quad = draw(image, 0, 0, 64, 32);

        assertEquals(64 - 1, pixel(quad[6]));
        assertEquals(32 - 1, pixel(quad[7]));
    }

    @Test
    void cutOfAnotherImageIsUnchanged() {
        RenderImage image = renderDevice.createImage(OTHER, true);

        float[] quad = draw(image, 4, 2, 8, 8);

        assertEquals(((BatchRenderImage) image).getX() + 4, pixel(quad[4]));
        assertEquals(8 - 1, pixel(quad[6]));
    }

    private float[] draw(RenderImage image, int srcX, int srcY, int srcW, int srcH) {
        renderDevice.beginFrame();
        renderDevice.renderImage(image, 10, 20, 8, 8, srcX, srcY, srcW, srcH, Color.WHITE, 1f, 14, 24);
        renderDevice.endFrame();
        return quads.get(quads.size() - 1);
    }

    private static int pixel(float textureCoordinate) {
        return Math.round(textureCoordinate * ATLAS - 0.5f);
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
                            return 1;
                        case "loadImage":
                            return args[0] instanceof String ? new BatchRenderBackend.ByteBufferedImage(null, 64, 32) : null;
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
