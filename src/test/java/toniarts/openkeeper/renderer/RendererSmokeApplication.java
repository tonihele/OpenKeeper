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
package toniarts.openkeeper.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.SceneProcessor;
import com.jme3.post.filters.TranslucentBucketFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengles.GLES20;
import org.lwjgl.opengl.GL11;
import toniarts.openkeeper.utils.GraphicsBackend;

/**
 * A deterministic scene smoke test used to validate both graphics backends.
 * This is launched explicitly through the renderer smoke Gradle tasks and is
 * not part of the normal unit test suite.
 */
public final class RendererSmokeApplication extends SimpleApplication {

    private static final Logger LOGGER = Logger.getLogger(RendererSmokeApplication.class.getName());
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private static final int WARMUP_FRAMES = 120;
    private static final int MEASURED_FRAMES = 300;
    private static final Duration TIMEOUT = Duration.ofSeconds(90);

    private final String backend;
    private final Path capturePath;
    private final CountDownLatch finished = new CountDownLatch(1);
    private CaptureProcessor captureProcessor;
    private Throwable failure;

    private RendererSmokeApplication(String backend) {
        this.backend = backend;
        this.capturePath = Path.of("build", "renderer-smoke", backend + ".rgba");
    }

    /**
     * Runs the smoke test for the backend named on the command line.
     *
     * @param args one argument: {@code opengl} or {@code vulkan}
     * @throws Exception if the context, capture, or frame validation fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !("opengl".equalsIgnoreCase(args[0]) || "vulkan".equalsIgnoreCase(args[0]))) {
            throw new IllegalArgumentException("Expected one backend argument: opengl or vulkan");
        }

        String backend = args[0].toLowerCase(java.util.Locale.ROOT);
        RendererSmokeApplication app = new RendererSmokeApplication(backend);
        AppSettings settings = new AppSettings(true);
        settings.setRenderer("vulkan".equals(backend) ? AppSettings.ANGLE_GLES3 : AppSettings.LWJGL_OPENGL33);
        settings.setTitle("OpenKeeper renderer smoke - " + backend);
        settings.setResolution(WIDTH, HEIGHT);
        settings.setVSync(false);
        settings.setSamples(0);
        settings.setFrameRate(0);
        settings.setUseJoysticks(false);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setDisplayStatView(false);
        app.setDisplayFps(false);
        GraphicsBackend.prepareForStartup(settings);

        app.start();
        if (!app.finished.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
            app.stop(true);
            throw new IllegalStateException("Renderer smoke timed out after " + TIMEOUT);
        }
        if (app.failure != null) {
            throw new IllegalStateException("Renderer smoke failed for " + backend, app.failure);
        }
    }

    @Override
    public void simpleInitApp() {
        try {
            GraphicsBackend.logAndVerifyActiveRenderer(settings);
            viewPort.setBackgroundColor(new ColorRGBA(0.05f, 0.10f, 0.15f, 1f));

            Geometry cube = new Geometry("Vulkan/OpenGL smoke cube", new Box(1f, 1f, 1f));
            Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            material.setColor("Color", ColorRGBA.Red);
            cube.setMaterial(material);
            rootNode.attachChild(cube);

            DirectionalLight keyLight = new DirectionalLight();
            keyLight.setDirection(new Vector3f(0f, 0f, -1f));
            keyLight.setColor(ColorRGBA.White);
            rootNode.addLight(keyLight);

            attachMaterialProbe("DK flat terrain shader", "MatDefs/DkLighting/Flat.j3md",
                    new Vector3f(-1.9f, 0f, 0f), new ColorRGBA(0f, 1f, 0f, 1f));
            attachMaterialProbe("DK vertex terrain shader", "MatDefs/DkLighting/Vertex.j3md",
                    new Vector3f(1.9f, 0f, 0f), new ColorRGBA(0f, 0f, 1f, 1f));
            attachCreatureLightingProbe();
            attachUnitFlowerProbe();
            attachProgressIndicatorProbe();
            attachMovieMaterialProbe();

            cam.setLocation(new Vector3f(0f, 0f, 5f));
            cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

            FrameBuffer captureBuffer = new FrameBuffer(WIDTH, HEIGHT, 0);
            captureBuffer.setColorBuffer(Image.Format.RGBA8);
            captureBuffer.setDepthBuffer(Image.Format.Depth);
            ViewPort captureViewPort = renderManager.createPreView("Renderer smoke capture", cam);
            captureViewPort.setClearFlags(true, true, true);
            captureViewPort.setBackgroundColor(new ColorRGBA(0.05f, 0.10f, 0.15f, 1f));
            captureViewPort.setOutputFrameBuffer(captureBuffer);
            captureViewPort.attachScene(rootNode);

            FilterPostProcessor filterProcessor = new FilterPostProcessor(assetManager);
            filterProcessor.addFilter(new SSAOFilter(5.94f, 3.92f, 0.33f, 0.1f));
            filterProcessor.addFilter(new TranslucentBucketFilter());
            viewPort.addProcessor(filterProcessor);

            captureProcessor = new CaptureProcessor();
            captureViewPort.addProcessor(captureProcessor);
        } catch (Throwable ex) {
            failure = ex;
            finished.countDown();
            stop(false);
        }
    }

    private void attachMaterialProbe(String name, String materialDefinition, Vector3f location, ColorRGBA color) {
        Material material = new Material(assetManager, materialDefinition);
        material.setTexture("DiffuseMap", createSolidTexture(color));
        if (materialDefinition.endsWith("Flat.j3md")) {
            material.setColor("FlatLight", ColorRGBA.White);
        } else {
            material.setColor("Ambient", ColorRGBA.White);
        }
        Geometry probe = new Geometry(name, new Box(0.35f, 0.35f, 0.35f));
        probe.setMaterial(material);
        probe.setLocalTranslation(location);
        rootNode.attachChild(probe);
    }

    private void attachCreatureLightingProbe() {
        Material material = new Material(assetManager, "MatDefs/LightingSprite.j3md");
        material.setTexture("DiffuseMap", createSolidTexture(new ColorRGBA(1f, 1f, 0f, 1f)));
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.2f, 1f));
        material.setColor("Diffuse", ColorRGBA.White);
        material.setColor("Specular", ColorRGBA.White);
        material.setFloat("Shininess", 8f);
        Geometry probe = new Geometry("Creature lighting shader", new Box(0.35f, 0.35f, 0.35f));
        probe.setMaterial(material);
        probe.setLocalTranslation(0f, 1.35f, 0f);
        rootNode.attachChild(probe);
    }

    private void attachUnitFlowerProbe() {
        Texture2D texture = createSolidTexture(ColorRGBA.White);
        Material material = new Material(assetManager, "MatDefs/UnitFlower.j3md");
        material.setTexture("CenterTexture", texture);
        material.setTexture("HealthTexture", texture);
        material.setColor("Color", ColorRGBA.White);
        material.setFloat("Experience", 0.35f);
        Geometry probe = new Geometry("Unit flower shader", new Box(0.28f, 0.28f, 0.28f));
        probe.setMaterial(material);
        probe.setLocalTranslation(-1.25f, -1.5f, 0f);
        rootNode.attachChild(probe);
    }

    private void attachProgressIndicatorProbe() {
        Material material = new Material(assetManager, "MatDefs/Gui/ProgressIndicator.j3md");
        material.setColor("Color", new ColorRGBA(1f, 0f, 1f, 1f));
        material.setFloat("Progress", 0.75f);
        Geometry probe = new Geometry("Progress indicator shader", new Box(0.28f, 0.28f, 0.28f));
        probe.setMaterial(material);
        probe.setLocalTranslation(0f, -1.5f, 0f);
        rootNode.attachChild(probe);
    }

    private void attachMovieMaterialProbe() {
        Material material = new Material(assetManager, "MatDefs/Video/MovieMaterial.j3md");
        material.setTexture("TexLuma", createSolidTexture(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f)));
        material.setTexture("TexCr", createSolidTexture(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f)));
        material.setTexture("TexCb", createSolidTexture(new ColorRGBA(0.5f, 0.5f, 0.5f, 1f)));
        material.setVector2("AspectValues", new Vector2f(1f, 1f));
        material.setVector2("ValidRange", new Vector2f(1f, 1f));
        material.setColor("LetterboxColor", ColorRGBA.Black);
        material.setBoolean("NoFrame", false);
        material.setBoolean("GammaCorrection", false);
        Geometry probe = new Geometry("Video material shader", new Box(0.28f, 0.28f, 0.28f));
        probe.setMaterial(material);
        probe.setLocalTranslation(1.25f, -1.5f, 0f);
        rootNode.attachChild(probe);
    }

    private Texture2D createSolidTexture(ColorRGBA color) {
        ByteBuffer data = BufferUtils.createByteBuffer(4);
        data.put((byte) Math.round(color.r * 255f));
        data.put((byte) Math.round(color.g * 255f));
        data.put((byte) Math.round(color.b * 255f));
        data.put((byte) Math.round(color.a * 255f));
        data.flip();
        Image image = new Image(Image.Format.RGBA8, 1, 1, data);
        Texture2D texture = new Texture2D(image);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        return texture;
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (captureProcessor != null && captureProcessor.isComplete()) {
            try {
                captureProcessor.writeCapture(capturePath);
                captureProcessor.logSummary();
            } catch (Throwable ex) {
                failure = ex;
            } finally {
                finished.countDown();
                stop(false);
            }
        }
    }

    @Override
    public void handleError(String errorMessage, Throwable throwable) {
        failure = throwable == null ? new IllegalStateException(errorMessage) : throwable;
        finished.countDown();
        super.handleError(errorMessage, throwable);
    }

    private final class CaptureProcessor implements SceneProcessor {

        private final List<Long> frameIntervalsNanos = new ArrayList<>(MEASURED_FRAMES);
        private boolean initialized;
        private int frameCount;
        private long lastMeasuredFrameNanos;
        private ByteBuffer pixels;
        private int red;
        private int green;
        private int blue;
        private String renderer;

        @Override
        public void initialize(RenderManager rm, ViewPort vp) {
            initialized = true;
        }

        @Override
        public void reshape(ViewPort vp, int w, int h) {
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

        @Override
        public void preFrame(float tpf) {
        }

        @Override
        public void postQueue(RenderQueue rq) {
        }

        @Override
        public void postFrame(FrameBuffer out) {
            try {
                if (frameCount == 0) {
                    renderer = GraphicsBackend.isVulkan(settings)
                            ? GLES20.glGetString(GLES20.GL_RENDERER)
                            : GL11.glGetString(GL11.GL_RENDERER);
                    pixels = ByteBuffer.allocateDirect(WIDTH * HEIGHT * 4);
                    renderManager.getRenderer().readFrameBuffer(out, pixels);
                    pixels.position(0);
                    int centerPixel = ((HEIGHT / 2) * WIDTH + (WIDTH / 2)) * 4;
                    red = Byte.toUnsignedInt(pixels.get(centerPixel));
                    green = Byte.toUnsignedInt(pixels.get(centerPixel + 1));
                    blue = Byte.toUnsignedInt(pixels.get(centerPixel + 2));
                } else if (frameCount >= WARMUP_FRAMES) {
                    long now = System.nanoTime();
                    if (lastMeasuredFrameNanos != 0) {
                        frameIntervalsNanos.add(now - lastMeasuredFrameNanos);
                    }
                    lastMeasuredFrameNanos = now;
                }
                frameCount++;
            } catch (Throwable ex) {
                failure = ex;
                frameCount = WARMUP_FRAMES + MEASURED_FRAMES + 1;
            }
        }

        @Override
        public void cleanup() {
            initialized = false;
        }

        @Override
        public void setProfiler(com.jme3.profile.AppProfiler profiler) {
        }

        private boolean isComplete() {
            return frameIntervalsNanos.size() >= MEASURED_FRAMES || failure != null;
        }

        private void writeCapture(Path path) throws IOException {
            if (pixels == null) {
                throw new IllegalStateException("No framebuffer was captured");
            }
            Files.createDirectories(path.getParent());
            pixels.position(0);
            byte[] capture = new byte[pixels.capacity()];
            pixels.get(capture);
            Files.write(path, capture);
            if (red < 200 || green > 80 || blue > 80) {
                throw new IllegalStateException("Center pixel is not the expected red cube: rgb("
                        + red + ", " + green + ", " + blue + ")");
            }
            if (GraphicsBackend.isVulkan(settings) && (renderer == null
                    || !renderer.toLowerCase(java.util.Locale.ROOT).contains("vulkan"))) {
                throw new IllegalStateException("ANGLE did not report a Vulkan renderer: " + renderer);
            }
        }

        private void logSummary() throws NoSuchAlgorithmException {
            List<Long> sortedIntervals = new ArrayList<>(frameIntervalsNanos);
            sortedIntervals.sort(Long::compareTo);
            long totalNanos = sortedIntervals.stream().mapToLong(Long::longValue).sum();
            double averageMs = totalNanos / (double) sortedIntervals.size() / 1_000_000d;
            double p95Ms = sortedIntervals.get((int) Math.ceil(sortedIntervals.size() * 0.95) - 1)
                    / 1_000_000d;
            byte[] capture = new byte[pixels.capacity()];
            pixels.position(0);
            pixels.get(capture);
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(capture));
            LOGGER.info(() -> "Smoke backend=" + backend + ", renderer=" + renderer
                    + ", centerRgb=" + red + "," + green + "," + blue
                    + ", captureSha256=" + checksum + ", frameIntervalAvgMs=" + averageMs
                    + ", frameIntervalP95Ms=" + p95Ms + " (host pacing, includes driver scheduling)");
        }
    }
}
