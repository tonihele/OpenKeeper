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

import com.jme3.app.Application;
import com.jme3.util.BufferUtils;
import com.jme3.util.Screenshots;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Captures the default color buffer into a {@link BufferedImage} / PNG.
 */
public final class FrameGrabber {

    private FrameGrabber() {
    }

    public static BufferedImage capture(Application app) {
        int width = app.getCamera().getWidth();
        int height = app.getCamera().getHeight();
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        app.getRenderer().readFrameBuffer(app.getViewPort().getOutputFrameBuffer(), buffer);
        // Screenshots.convertScreenShot requires a byte-backed BufferedImage (ABGR).
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Screenshots.convertScreenShot(buffer, image);
        // Normalize to INT_RGB for ImageMetrics / PNG size stability
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(image, 0, 0, null);
        return rgb;
    }

    public static Path writePng(BufferedImage image, Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        ImageIO.write(image, "png", path.toFile());
        return path.toAbsolutePath();
    }

    public static BufferedImage readPng(Path path) throws IOException {
        return ImageIO.read(path.toFile());
    }
}
