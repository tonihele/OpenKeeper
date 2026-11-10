/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.lighting;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/** Compares legacy full-list lighting with conservative page-local candidates. */
public final class SceneLightsClusterBenchmark extends SimpleApplication {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP_FRAMES = 120;
    private static final int MEASURED_FRAMES = 120;
    private static final int QUERY_SLOTS = 32;
    private static final int MAX_LIGHTS = SceneLights.MAX_LIGHTS;
    private static final int MAP_SIZE = 85;
    private static final float CENTER_X = 42.5f;
    private static final float CENTER_Z = 42.5f;
    private static final float VIEW_HALF_WIDTH = 15.5f;
    private static final float VIEW_HALF_HEIGHT = 8.5f;
    private static final int GRID_CELLS_X = 280;
    private static final int GRID_CELLS_Z = 160;
    private static final float LIGHT_RADIUS = 4.5f;
    private static final int[] LIGHT_COUNTS = {64, 256, 1024, 2048, MAX_LIGHTS};
    private static final int[] MOVING_LIGHT_COUNTS = {1024, MAX_LIGHTS};

    private final List<Mode> modes = new ArrayList<>();
    private final List<PendingQuery> pendingQueries = new ArrayList<>();
    private final int[] queryIds = new int[QUERY_SLOTS];
    private final boolean[] queryInFlight = new boolean[QUERY_SLOTS];
    private final List<Long> cpuSamples = new ArrayList<>(MEASURED_FRAMES);
    private final List<Long> gpuSamples = new ArrayList<>(MEASURED_FRAMES);
    private final List<Long> updateSamples = new ArrayList<>(MEASURED_FRAMES);
    private final Map<String, ByteBuffer> legacyFrames = new HashMap<>();
    private final Map<String, ByteBuffer> clusteredFrames = new HashMap<>();
    private final Set<String> capturedFrames = new HashSet<>();

    private SceneLights legacyLights;
    private SceneLights clusteredLights;
    private List<LightSource> allLights;
    private List<Vector3f> baseLightPositions;
    private Map<Integer, List<LightSource>> lightSets;
    private Material legacyMaterial;
    private Material clusteredMaterial;
    private Geometry geometry;
    private int phaseIndex;
    private int warmupSubmitted;
    private Stage stage = Stage.WARMUP;
    private long frameStartNanos;
    private int activeQuerySlot = -1;
    private boolean activeQueryMeasured;
    private int movingFrame;
    private int renderedMovementState;
    private Mode activeMode;

    private SceneLightsClusterBenchmark() {
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        settings.setResolution(WIDTH, HEIGHT);
        settings.setVSync(false);
        settings.setFrameRate(0);
        settings.setUseJoysticks(false);
        settings.setAudioRenderer(null);
        settings.setTitle("OpenKeeper scene light cluster benchmark");

        SceneLightsClusterBenchmark app = new SceneLightsClusterBenchmark();
        app.setSettings(settings);
        app.setShowSettings(false);
        app.setDisplayFps(false);
        app.setDisplayStatView(false);
        app.start(JmeContext.Type.OffscreenSurface);
    }

    @Override
    public void simpleInitApp() {
        cam.setParallelProjection(true);
        cam.setFrustum(1f, 256f, -VIEW_HALF_WIDTH, VIEW_HALF_WIDTH,
                VIEW_HALF_HEIGHT, -VIEW_HALF_HEIGHT);
        cam.setLocation(new Vector3f(CENTER_X, -24f, CENTER_Z));
        cam.lookAtDirection(Vector3f.UNIT_Y, new Vector3f(0f, 0f, -1f));
        viewPort.setBackgroundColor(new ColorRGBA(0f, 0f, 0f, 1f));

        legacyLights = new SceneLights();
        clusteredLights = new SceneLights();
        clusteredLights.initAmbientMap(MAP_SIZE, MAP_SIZE);
        for (int y = 0; y < MAP_SIZE; y++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                clusteredLights.setTileAmbient(x, y, ColorRGBA.Black, true);
            }
        }
        allLights = createLights();
        lightSets = new HashMap<>();
        for (int count : LIGHT_COUNTS) {
            lightSets.put(count, List.copyOf(allLights.subList(0, count)));
        }
        legacyMaterial = createLightingMaterial();
        clusteredMaterial = createLightingMaterial();
        legacyLights.bindMaterials(List.of(legacyMaterial));
        clusteredLights.bindMaterials(List.of(clusteredMaterial));

        geometry = new Geometry("lighting grid", createGridMesh());
        geometry.setMaterial(legacyMaterial);
        geometry.setQueueBucket(RenderQueue.Bucket.Opaque);
        geometry.setBatchHint(com.jme3.scene.Spatial.BatchHint.Never);
        geometry.setShadowMode(RenderQueue.ShadowMode.Off);
        rootNode.attachChild(geometry);

        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < LIGHT_COUNTS.length; i++) {
                int countIndex = pass == 0 ? i : LIGHT_COUNTS.length - 1 - i;
                int count = LIGHT_COUNTS[countIndex];
                boolean clusteredFirst = pass == 1;
                addModePair(pass, count, clusteredFirst, false);
            }
            for (int i = 0; i < MOVING_LIGHT_COUNTS.length; i++) {
                int countIndex = pass == 0 ? i : MOVING_LIGHT_COUNTS.length - 1 - i;
                int count = MOVING_LIGHT_COUNTS[countIndex];
                addModePair(pass, count, pass == 1, true);
            }
        }
        setModeMaterial(modes.get(0));
        System.out.printf(Locale.ROOT,
                "scene-light-cluster renderer=initializing resolution=%dx%d grid=%dx%d radius=%.2f counts=%s warmup=%d samples=%d%n",
                WIDTH, HEIGHT, GRID_CELLS_X + 1, GRID_CELLS_Z + 1, LIGHT_RADIUS,
                Arrays.toString(LIGHT_COUNTS), WARMUP_FRAMES, MEASURED_FRAMES);
        viewPort.addProcessor(new TimerProcessor());
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (phaseIndex >= modes.size()) {
            return;
        }
        Mode mode = modes.get(phaseIndex);
        if (mode.moving()) {
            renderedMovementState = movingFrame++ & 1;
            float offset = renderedMovementState == 0 ? 0f : SceneLights.LIGHT_CLUSTER_SIZE_TILES;
            for (int i = 0; i < mode.lightCount(); i++) {
                Vector3f base = baseLightPositions.get(i);
                allLights.get(i).getPosition().set(base.x + offset, base.y, base.z + offset);
            }
        } else {
            renderedMovementState = 0;
        }
        long start = System.nanoTime();
        if (mode.clustered()) {
            clusteredLights.update(lightSets.get(mode.lightCount()), null);
        } else {
            legacyLights.update(lightSets.get(mode.lightCount()), null);
        }
        if (stage == Stage.MEASURE) {
            updateSamples.add(System.nanoTime() - start);
        }
    }

    private void addModePair(int pass, int count, boolean clusteredFirst, boolean moving) {
        modes.add(new Mode(label(pass, count, clusteredFirst, moving), count, clusteredFirst, moving));
        modes.add(new Mode(label(pass, count, !clusteredFirst, moving), count, !clusteredFirst, moving));
    }

    private String label(int pass, int count, boolean clustered, boolean moving) {
        return "pass-" + (pass + 1) + "/lights-" + count + "/" + (moving ? "moving" : "static")
                + "/" + (clustered ? "clustered" : "legacy");
    }

    private void setModeMaterial(Mode mode) {
        if (activeMode != null && activeMode.moving() && !mode.moving()) {
            for (int i = 0; i < allLights.size(); i++) {
                Vector3f base = baseLightPositions.get(i);
                allLights.get(i).getPosition().set(base);
            }
        }
        activeMode = mode;
        movingFrame = 0;
        geometry.setMaterial(mode.clustered() ? clusteredMaterial : legacyMaterial);
    }

    private Material createLightingMaterial() {
        Material material = new Material(assetManager, "MatDefs/DkLighting/Vertex.j3md");
        material.setTexture("DiffuseMap", whiteTexture());
        material.setVector2("AmbientMapSize", new Vector2f(MAP_SIZE, MAP_SIZE));
        material.setColor("Ambient", ColorRGBA.Black);
        material.setFloat("MinLight", 0f);
        material.setFloat("AmbientScale", 0f);
        material.setFloat("RockFloor", 0f);
        material.setBoolean("IgnoreObjectLights", false);
        return material;
    }

    private List<LightSource> createLights() {
        List<LightSource> lights = new ArrayList<>(MAX_LIGHTS);
        baseLightPositions = new ArrayList<>(MAX_LIGHTS);
        for (int light = 0; light < MAX_LIGHTS; light++) {
            int x = (light * 37 + 11) % 509;
            int z = (light * 83 + 29) % 509;
            Vector3f position = new Vector3f(
                    CENTER_X - VIEW_HALF_WIDTH + 30f * x / 509f,
                    1.5f + (light % 7) * 0.15f,
                    CENTER_Z - VIEW_HALF_HEIGHT + 16f * z / 509f);
            baseLightPositions.add(position.clone());
            lights.add(new LightSource(LightSource.Kind.THING, null, position,
                    230, 184, 122, 0.8f, LIGHT_RADIUS));
        }
        return List.copyOf(lights);
    }

    private static Mesh createGridMesh() {
        int vertexCount = (GRID_CELLS_X + 1) * (GRID_CELLS_Z + 1);
        int indexCount = GRID_CELLS_X * GRID_CELLS_Z * 6;
        FloatBuffer positions = BufferUtils.createFloatBuffer(vertexCount * 3);
        FloatBuffer normals = BufferUtils.createFloatBuffer(vertexCount * 3);
        FloatBuffer texCoords = BufferUtils.createFloatBuffer(vertexCount * 2);
        IntBuffer indices = BufferUtils.createIntBuffer(indexCount);
        for (int z = 0; z <= GRID_CELLS_Z; z++) {
            float worldZ = CENTER_Z - VIEW_HALF_HEIGHT + 2f * VIEW_HALF_HEIGHT * z / GRID_CELLS_Z;
            for (int x = 0; x <= GRID_CELLS_X; x++) {
                float worldX = CENTER_X - VIEW_HALF_WIDTH + 2f * VIEW_HALF_WIDTH * x / GRID_CELLS_X;
                positions.put(worldX).put(0f).put(worldZ);
                normals.put(0f).put(1f).put(0f);
                texCoords.put(x / (float) GRID_CELLS_X).put(z / (float) GRID_CELLS_Z);
            }
        }
        for (int z = 0; z < GRID_CELLS_Z; z++) {
            for (int x = 0; x < GRID_CELLS_X; x++) {
                int bottomLeft = z * (GRID_CELLS_X + 1) + x;
                int bottomRight = bottomLeft + 1;
                int topLeft = bottomLeft + GRID_CELLS_X + 1;
                int topRight = topLeft + 1;
                indices.put(bottomLeft).put(topLeft).put(bottomRight);
                indices.put(bottomRight).put(topLeft).put(topRight);
            }
        }
        positions.flip();
        normals.flip();
        texCoords.flip();
        indices.flip();

        Mesh mesh = new Mesh();
        mesh.setMode(Mesh.Mode.Triangles);
        mesh.setBuffer(Type.Position, 3, positions);
        mesh.setBuffer(Type.Normal, 3, normals);
        mesh.setBuffer(Type.TexCoord, 2, texCoords);
        mesh.setBuffer(Type.Index, 3, indices);
        mesh.updateBound();
        mesh.setStatic();
        return mesh;
    }

    private static Texture2D whiteTexture() {
        ByteBuffer data = BufferUtils.createByteBuffer(4);
        data.put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).flip();
        Image image = new Image(Image.Format.RGBA8, 1, 1, data, ColorSpace.Linear);
        Texture2D texture = new Texture2D(image);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        return texture;
    }

    private enum Stage {
        WARMUP,
        DRAIN_WARMUP,
        MEASURE,
        DRAIN_MEASURE
    }

    private record Mode(String label, int lightCount, boolean clustered, boolean moving) {
    }

    private record PendingQuery(int slot, boolean measured) {
    }

    private final class TimerProcessor implements SceneProcessor {

        private boolean initialized;

        @Override
        public void initialize(RenderManager renderManager, ViewPort viewPort) {
            for (int i = 0; i < QUERY_SLOTS; i++) {
                queryIds[i] = GL15.glGenQueries();
            }
            initialized = true;
            System.out.printf("OpenGL renderer: %s%n", GL11.glGetString(GL11.GL_RENDERER));
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
            frameStartNanos = System.nanoTime();
            activeQuerySlot = freeQuerySlot();
            activeQueryMeasured = stage == Stage.MEASURE;
            if (activeQuerySlot >= 0 && (stage == Stage.WARMUP || stage == Stage.MEASURE)) {
                GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queryIds[activeQuerySlot]);
            } else {
                activeQuerySlot = -1;
            }
        }

        @Override
        public void postQueue(RenderQueue queue) {
        }

        @Override
        public void postFrame(FrameBuffer out) {
            long cpuNanos = System.nanoTime() - frameStartNanos;
            if (activeQuerySlot >= 0) {
                GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
                queryInFlight[activeQuerySlot] = true;
                pendingQueries.add(new PendingQuery(activeQuerySlot, activeQueryMeasured));
                if (stage == Stage.WARMUP) {
                    warmupSubmitted++;
                } else if (stage == Stage.MEASURE && activeQueryMeasured) {
                    cpuSamples.add(cpuNanos);
                }
            }

            if (stage == Stage.WARMUP) {
                String captureId = phaseIndex + ":" + renderedMovementState;
                if (capturedFrames.add(captureId)) {
                    captureFrame(modes.get(phaseIndex), renderedMovementState);
                }
            }
            pollQueries();
            if (stage == Stage.WARMUP && warmupSubmitted >= WARMUP_FRAMES) {
                stage = Stage.DRAIN_WARMUP;
            }
            if (stage == Stage.DRAIN_WARMUP && pendingQueries.isEmpty()) {
                stage = Stage.MEASURE;
                cpuSamples.clear();
                gpuSamples.clear();
                updateSamples.clear();
            } else if (stage == Stage.MEASURE && cpuSamples.size() >= MEASURED_FRAMES) {
                stage = Stage.DRAIN_MEASURE;
            }
            if (stage == Stage.DRAIN_MEASURE && pendingQueries.isEmpty()) {
                reportAndAdvance();
            }
        }

        private void captureFrame(Mode mode, int movementState) {
            ByteBuffer pixels = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
            GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            pixels.rewind();
            String key = "pair-" + (phaseIndex / 2) + "/" + (mode.moving() ? "moving" : "static")
                    + "-lights-" + mode.lightCount() + "-phase-" + movementState;
            Map<String, ByteBuffer> target = mode.clustered() ? clusteredFrames : legacyFrames;
            target.put(key, pixels);
            ByteBuffer legacy = legacyFrames.get(key);
            ByteBuffer clustered = clusteredFrames.get(key);
            if (legacy != null && clustered != null) {
                long different = 0;
                int maxDelta = 0;
                for (int i = 0; i < legacy.capacity(); i++) {
                    int a = Byte.toUnsignedInt(legacy.get(i));
                    int b = Byte.toUnsignedInt(clustered.get(i));
                    if (a != b) {
                        different++;
                        maxDelta = Math.max(maxDelta, Math.abs(a - b));
                    }
                }
                System.out.printf(Locale.ROOT,
                        "parity=%s differingChannels=%d totalChannels=%d maxChannelDelta=%d exact=%s%n",
                        key, different, legacy.capacity(), maxDelta, different == 0);
            }
        }

        private int freeQuerySlot() {
            for (int i = 0; i < QUERY_SLOTS; i++) {
                if (!queryInFlight[i]) {
                    return i;
                }
            }
            return -1;
        }

        private void pollQueries() {
            Iterator<PendingQuery> iterator = pendingQueries.iterator();
            while (iterator.hasNext()) {
                PendingQuery pending = iterator.next();
                int queryId = queryIds[pending.slot()];
                if (GL15.glGetQueryObjecti(queryId, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
                    long elapsed = GL33.glGetQueryObjectui64(queryId, GL15.GL_QUERY_RESULT);
                    if (pending.measured()) {
                        gpuSamples.add(elapsed);
                    }
                    queryInFlight[pending.slot()] = false;
                    iterator.remove();
                }
            }
        }

        private void reportAndAdvance() {
            Mode mode = modes.get(phaseIndex);
            System.out.printf(Locale.ROOT,
                    "result=%s updateMedianMs=%.3f updateP95Ms=%.3f renderCpuMedianMs=%.3f renderCpuP95Ms=%.3f gpuMedianMs=%.3f gpuP95Ms=%.3f updateSamples=%d gpuSamples=%d%n",
                    mode.label(), medianMillis(updateSamples), percentileMillis(updateSamples, 0.95),
                    medianMillis(cpuSamples), percentileMillis(cpuSamples, 0.95),
                    medianMillis(gpuSamples), percentileMillis(gpuSamples, 0.95),
                    updateSamples.size(), gpuSamples.size());
            phaseIndex++;
            if (phaseIndex >= modes.size()) {
                System.out.println("scene light cluster benchmark complete");
                stop(false);
                return;
            }
            Mode next = modes.get(phaseIndex);
            setModeMaterial(next);
            warmupSubmitted = 0;
            stage = Stage.WARMUP;
            cpuSamples.clear();
            gpuSamples.clear();
            updateSamples.clear();
        }

        @Override
        public void cleanup() {
            if (initialized) {
                for (int queryId : queryIds) {
                    GL15.glDeleteQueries(queryId);
                }
                initialized = false;
            }
        }

        @Override
        public void setProfiler(com.jme3.profile.AppProfiler profiler) {
        }
    }

    private static double medianMillis(List<Long> values) {
        return percentileMillis(values, 0.5);
    }

    private static double percentileMillis(List<Long> values, double percentile) {
        long[] sorted = values.stream().mapToLong(Long::longValue).toArray();
        Arrays.sort(sorted);
        if (sorted.length == 0) {
            return Double.NaN;
        }
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index] / 1_000_000d;
    }
}
