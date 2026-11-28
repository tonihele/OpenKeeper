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
package toniarts.openkeeper.performance;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/**
 * GPU timer-query workload for base-level versus mipmapped minification.
 * Run through the Gradle mipSamplingBenchmark task.
 */
public final class MipSamplingBenchmark extends SimpleApplication {

    private static final int TEXTURE_SIZE = Integer.getInteger("mipmap.textureSize", 2048);
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int TILES_X = 30;
    private static final int TILES_Y = 17;
    private static final int WARMUP_FRAMES = 40;
    private static final int MEASURED_FRAMES = 120;
    private static final long SEED = 824_171L;

    private final boolean useMipmaps;

    private MipSamplingBenchmark(boolean useMipmaps) {
        this.useMipmaps = useMipmaps;
    }

    public static void main(String[] args) {
        boolean useMipmaps = Boolean.getBoolean("mipmap.mode");
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        settings.setResolution(WIDTH, HEIGHT);
        settings.setVSync(false);
        settings.setFrameRate(0);
        settings.setUseJoysticks(false);
        settings.setAudioRenderer(null);
        settings.setTitle("OpenKeeper mip sampling benchmark");

        MipSamplingBenchmark app = new MipSamplingBenchmark(useMipmaps);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setDisplayFps(false);
        app.setDisplayStatView(false);
        app.start(JmeContext.Type.OffscreenSurface);
    }

    @Override
    public void simpleInitApp() {
        byte[] pixels = new byte[TEXTURE_SIZE * TEXTURE_SIZE * 4];
        new Random(SEED).nextBytes(pixels);
        ByteBuffer data = BufferUtils.createByteBuffer(pixels.length);
        data.put(pixels).flip();
        Image image = new Image(Image.Format.RGBA8, TEXTURE_SIZE, TEXTURE_SIZE, data, ColorSpace.Linear);

        Texture2D texture = new Texture2D(image);
        texture.setWrap(Texture.WrapMode.Repeat);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setMinFilter(useMipmaps ? Texture.MinFilter.Trilinear : Texture.MinFilter.BilinearNoMipMaps);
        System.out.printf("Mipmaps requested: %s; source mip levels: %d%n",
                useMipmaps, image.getMipMapSizes() == null ? 0 : image.getMipMapSizes().length);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", texture);
        material.getAdditionalRenderState().setDepthTest(false);
        material.getAdditionalRenderState().setDepthWrite(false);
        material.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);

        cam.setParallelProjection(true);
        cam.setFrustum(1, 100, -12, 12, 6.75f, -6.75f);
        cam.setLocation(new Vector3f(0, 0, 10));
        cam.lookAtDirection(new Vector3f(0, 0, -1), Vector3f.UNIT_Y);
        Geometry geometry = new Geometry("mip-sampling-tile-grid", createTileGridMesh());
        geometry.setQueueBucket(RenderQueue.Bucket.Opaque);
        geometry.setBatchHint(Spatial.BatchHint.Never);
        geometry.setMaterial(material);
        rootNode.attachChild(geometry);

        viewPort.addProcessor(new GpuTimerProcessor(this, useMipmaps, image, pixels));
    }

    private static Mesh createTileGridMesh() {
        int tileCount = TILES_X * TILES_Y;
        float[] positions = new float[tileCount * 12];
        float[] texCoords = new float[tileCount * 8];
        int[] indices = new int[tileCount * 6];
        float tileWidth = 24f / TILES_X;
        float tileHeight = 13.5f / TILES_Y;
        int tile = 0;
        for (int row = 0; row < TILES_Y; row++) {
            for (int column = 0; column < TILES_X; column++) {
                int vertex = tile * 4;
                int position = vertex * 3;
                float x0 = -12 + column * tileWidth;
                float y0 = -6.75f + row * tileHeight;
                positions[position] = x0;
                positions[position + 1] = y0;
                positions[position + 3] = x0 + tileWidth;
                positions[position + 4] = y0;
                positions[position + 6] = x0 + tileWidth;
                positions[position + 7] = y0 + tileHeight;
                positions[position + 9] = x0;
                positions[position + 10] = y0 + tileHeight;

                int texCoord = vertex * 2;
                texCoords[texCoord] = 0;
                texCoords[texCoord + 1] = 0;
                texCoords[texCoord + 2] = 1;
                texCoords[texCoord + 3] = 0;
                texCoords[texCoord + 4] = 1;
                texCoords[texCoord + 5] = 1;
                texCoords[texCoord + 6] = 0;
                texCoords[texCoord + 7] = 1;

                int index = tile * 6;
                indices[index] = vertex;
                indices[index + 1] = vertex + 1;
                indices[index + 2] = vertex + 2;
                indices[index + 3] = vertex;
                indices[index + 4] = vertex + 2;
                indices[index + 5] = vertex + 3;
                tile++;
            }
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, positions);
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texCoords);
        mesh.setBuffer(VertexBuffer.Type.Index, 3, indices);
        mesh.updateBound();
        return mesh;
    }

    private static long fullMipChainBytes(int size) {
        long total = 0;
        for (int dimension = size; dimension >= 1; dimension >>= 1) {
            total += (long) dimension * dimension * 4;
            if (dimension == 1) {
                break;
            }
        }
        return total;
    }

    private static long median(List<Long> values) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    private static final class GpuTimerProcessor implements SceneProcessor {

        private final MipSamplingBenchmark app;
        private final boolean mipmapped;
        private final Image image;
        private final byte[] originalPixels;
        private final List<Long> frameNanos = new ArrayList<>(MEASURED_FRAMES);
        private final List<Long> renderSubmitNanos = new ArrayList<>(MEASURED_FRAMES);
        private boolean initialized;
        private int queryId;
        private int frame;
        private long renderStartNanos;

        private GpuTimerProcessor(MipSamplingBenchmark app, boolean mipmapped, Image image, byte[] originalPixels) {
            this.app = app;
            this.mipmapped = mipmapped;
            this.image = image;
            this.originalPixels = originalPixels;
        }

        @Override
        public void initialize(RenderManager renderManager, ViewPort viewPort) {
            queryId = GL15.glGenQueries();
            initialized = true;
            String mode = mipmapped ? "trilinear mips" : "base-level bilinear";
            System.out.printf("OpenGL renderer: %s%n", GL11.glGetString(GL11.GL_RENDERER));
            System.out.printf("Texture mode: %s; tiled surfaces: %d; draw calls/frame: 1; output: %dx%d%n",
                    mode, TILES_X * TILES_Y, WIDTH, HEIGHT);
        }

        @Override
        public void reshape(ViewPort viewPort, int width, int height) {
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void preFrame(float tpf) {
            renderStartNanos = System.nanoTime();
            GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queryId);
        }

        @Override
        public void postQueue(RenderQueue queue) {
        }

        @Override
        public void postFrame(FrameBuffer out) {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            long cpuSubmitNanos = System.nanoTime() - renderStartNanos;
            long elapsedNanos = GL33.glGetQueryObjectui64(queryId, GL15.GL_QUERY_RESULT);
            if (frame++ >= WARMUP_FRAMES) {
                frameNanos.add(elapsedNanos);
                renderSubmitNanos.add(cpuSubmitNanos);
                if (frameNanos.size() == MEASURED_FRAMES) {
                    ByteBuffer baseLevel = image.getData(0).duplicate();
                    baseLevel.rewind();
                    byte[] renderedBasePixels = new byte[baseLevel.remaining()];
                    baseLevel.get(renderedBasePixels);
                    if (!Arrays.equals(originalPixels, renderedBasePixels)) {
                        throw new IllegalStateException("Mip setup changed the base-level pixels");
                    }
                    long baseBytes = (long) TEXTURE_SIZE * TEXTURE_SIZE * 4;
                    long mipBytes = fullMipChainBytes(TEXTURE_SIZE);
                    long residentBytes = mipmapped ? mipBytes : baseBytes;
                    System.out.printf("GPU viewport median: %.3f ms/frame (%d samples)%n",
                            median(frameNanos) / 1_000_000.0, frameNanos.size());
                    System.out.printf("GPU viewport p95: %.3f ms/frame; generated mipmaps: %s%n",
                            percentile(frameNanos, 0.95) / 1_000_000.0, image.isMipmapsGenerated());
                    System.out.printf("CPU render-submit median: %.3f ms/frame; p95: %.3f ms/frame%n",
                            median(renderSubmitNanos) / 1_000_000.0,
                            percentile(renderSubmitNanos, 0.95) / 1_000_000.0);
                    System.out.printf("Estimated RGBA8 texture bytes: %d (%.1f MiB); full mip chain: %d (%.1f MiB, +%.1f%%)%n",
                            residentBytes, residentBytes / (1024.0 * 1024.0), mipBytes,
                            mipBytes / (1024.0 * 1024.0), 100.0 * (mipBytes - baseBytes) / baseBytes);
                    app.stop(false);
                }
            }
        }

        @Override
        public void cleanup() {
            if (initialized) {
                GL15.glDeleteQueries(queryId);
                initialized = false;
            }
        }

        @Override
        public void setProfiler(com.jme3.profile.AppProfiler profiler) {
        }
    }

    private static long percentile(List<Long> values, double percentile) {
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get((int) Math.ceil(percentile * sorted.size()) - 1);
    }
}
