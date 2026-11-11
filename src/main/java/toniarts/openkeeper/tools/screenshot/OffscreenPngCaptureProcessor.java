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
package toniarts.openkeeper.tools.screenshot;

import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.system.JmeSystem;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Reads the final offscreen framebuffer and writes one PNG without requiring an input device.
 */
final class OffscreenPngCaptureProcessor implements SceneProcessor {

    private Renderer renderer;
    private Camera camera;
    private ByteBuffer imageBuffer;
    private int width;
    private int height;
    private volatile Path requestedFile;
    private volatile Path capturedFile;
    private volatile IOException failure;
    private boolean initialized;

    @Override
    public void initialize(RenderManager renderManager, ViewPort viewPort) {
        this.renderer = renderManager.getRenderer();
        this.camera = viewPort.getCamera();
        reshape(viewPort, camera.getWidth(), camera.getHeight());
        initialized = true;
    }

    @Override
    public void reshape(ViewPort viewPort, int width, int height) {
        this.width = width;
        this.height = height;
        imageBuffer = BufferUtils.createByteBuffer(Math.multiplyExact(Math.multiplyExact(width, height), 4));
    }

    void requestCapture(Path outputFile) {
        if (requestedFile != null) {
            throw new IllegalStateException("A screenshot readback is already pending");
        }
        if (Files.exists(outputFile)) {
            throw new IllegalStateException("Refusing to overwrite existing screenshot: " + outputFile);
        }
        requestedFile = outputFile;
    }

    Path getCapturedFile() {
        return capturedFile;
    }

    IOException getFailure() {
        return failure;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(com.jme3.renderer.queue.RenderQueue renderQueue) {
    }

    @Override
    public void postFrame(FrameBuffer out) {
        Path outputFile = requestedFile;
        if (outputFile == null || failure != null) {
            return;
        }
        requestedFile = null;

        int viewPortX = (int) (camera.getViewPortLeft() * camera.getWidth());
        int viewPortY = (int) (camera.getViewPortBottom() * camera.getHeight());
        int viewPortWidth = (int) ((camera.getViewPortRight() - camera.getViewPortLeft()) * camera.getWidth());
        int viewPortHeight = (int) ((camera.getViewPortTop() - camera.getViewPortBottom()) * camera.getHeight());

        boolean fileCreated = false;
        try {
            renderer.setViewPort(0, 0, width, height);
            renderer.readFrameBuffer(out, imageBuffer);
            renderer.setViewPort(viewPortX, viewPortY, viewPortWidth, viewPortHeight);
            try (OutputStream output = Files.newOutputStream(outputFile,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                fileCreated = true;
                JmeSystem.writeImageFile(output, "png", imageBuffer, width, height);
            }
            capturedFile = outputFile;
        } catch (IOException | RuntimeException ex) {
            failure = ex instanceof IOException ioException ? ioException : new IOException(ex);
            renderer.setViewPort(viewPortX, viewPortY, viewPortWidth, viewPortHeight);
            if (fileCreated) {
                try {
                    Files.deleteIfExists(outputFile);
                } catch (IOException cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        initialized = false;
        imageBuffer = null;
        renderer = null;
        camera = null;
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
    }
}
