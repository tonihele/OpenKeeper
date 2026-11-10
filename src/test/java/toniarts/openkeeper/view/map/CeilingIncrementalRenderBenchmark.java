/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

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
import com.jme3.scene.Spatial;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/**
 * Matched offscreen renderer benchmark. It measures the production ceiling
 * meshes, with a bounded overhead view around one edited tile, and compares
 * GPU elapsed time and render-thread time for full and spatially chunked meshes.
 */
public final class CeilingIncrementalRenderBenchmark extends SimpleApplication {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int WARMUP_FRAMES = 120;
    private static final int MEASURED_FRAMES = 120;
    private static final int QUERY_SLOTS = 32;
    private static final int LIGHT_COUNT = 256;
    private static final int MAX_LIGHTS = 3084;
    private static final int LIGHT_TEXELS = 3;
    private static final int MAP_SIZE = 85;
    private static final float CENTER = 31.5f;
    private static final float VIEW_HALF_WIDTH = 15.5f;
    private static final float VIEW_HALF_HEIGHT = 8.5f;

    private final List<Mode> modes = new ArrayList<>();
    private final List<PendingQuery> pendingQueries = new ArrayList<>();
    private final int[] queryIds = new int[QUERY_SLOTS];
    private final boolean[] queryInFlight = new boolean[QUERY_SLOTS];
    private final List<Long> cpuSamples = new ArrayList<>(MEASURED_FRAMES);
    private final List<Long> gpuSamples = new ArrayList<>(MEASURED_FRAMES);
    private int phaseIndex;
    private int warmupSubmitted;
    private Stage stage = Stage.WARMUP;
    private long frameStartNanos;
    private int activeQuerySlot = -1;
    private boolean activeQueryMeasured;

    private CeilingIncrementalRenderBenchmark() {
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        settings.setResolution(WIDTH, HEIGHT);
        settings.setVSync(false);
        settings.setFrameRate(0);
        settings.setUseJoysticks(false);
        settings.setAudioRenderer(null);
        settings.setTitle("OpenKeeper ceiling chunk benchmark");

        CeilingIncrementalRenderBenchmark app = new CeilingIncrementalRenderBenchmark();
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
        cam.setLocation(new Vector3f(CENTER, -24f, CENTER));
        cam.lookAtDirection(Vector3f.UNIT_Y, new Vector3f(0f, 0f, -1f));

        Material material = createLightingMaterial();

        CeilingIncrementalBenchmark.MapState map = CeilingIncrementalBenchmark.visibleBenchmarkMap(MAP_SIZE, MAP_SIZE);
        addMode("full", 0, material, map);
        addMode("chunk-32", 32, material, map);
        addMode("chunk-48", 48, material, map);
        addMode("chunk-64", 64, material, map);

        System.out.printf(Locale.ROOT,
                "renderer setup=%dx%d viewport=%.0fx%.0f centered=(%.1f,%.1f) map=85x85 shader=DkLighting lights=%d warmup=%d samples=%d%n",
                WIDTH, HEIGHT, VIEW_HALF_WIDTH * 2f, VIEW_HALF_HEIGHT * 2f, CENTER, CENTER,
                LIGHT_COUNT, WARMUP_FRAMES, MEASURED_FRAMES);
        List<Mode> sourceModes = new ArrayList<>(modes);
        modes.clear();
        for (int pass = 0; pass < 2; pass++) {
            for (int offset = 0; offset < sourceModes.size(); offset++) {
                int index = pass == 0 ? offset : sourceModes.size() - 1 - offset;
                Mode mode = sourceModes.get(index);
                modes.add(mode.withLabel("pass-" + (pass + 1) + "/" + mode.label()));
            }
        }
        attachMode(modes.get(0));

        viewPort.addProcessor(new TimerProcessor());
    }

    private Material createLightingMaterial() {
        Material material = new Material(assetManager, "MatDefs/DkLighting/Vertex.j3md");
        material.setTexture("DiffuseMap", whiteTexture());
        material.setTexture("LightData", lightDataTexture());
        material.setTexture("AmbientMap", ambientTexture());
        material.setVector2("AmbientMapSize", new Vector2f(MAP_SIZE, MAP_SIZE));
        material.setColor("Ambient", ColorRGBA.Black);
        material.setFloat("MinLight", 1f);
        material.setFloat("AmbientScale", 0f);
        material.setFloat("RockFloor", 0f);
        return material;
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

    private static Texture2D ambientTexture() {
        ByteBuffer data = BufferUtils.createByteBuffer(MAP_SIZE * MAP_SIZE * 4);
        for (int tile = 0; tile < MAP_SIZE * MAP_SIZE; tile++) {
            data.put((byte) 38).put((byte) 38).put((byte) 38).put((byte) 0xFF);
        }
        data.flip();
        Image image = new Image(Image.Format.RGBA8, MAP_SIZE, MAP_SIZE, data, ColorSpace.Linear);
        Texture2D texture = new Texture2D(image);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        texture.setWrap(Texture.WrapMode.EdgeClamp);
        return texture;
    }

    private static Texture2D lightDataTexture() {
        int width = 1 + LIGHT_TEXELS * MAX_LIGHTS;
        ByteBuffer data = BufferUtils.createByteBuffer(width * 4 * Float.BYTES);
        FloatBuffer values = data.asFloatBuffer();
        values.put(0, LIGHT_COUNT);
        values.put(1, 0f);
        values.put(2, 0f);
        values.put(3, 0f);
        for (int light = 0; light < LIGHT_COUNT; light++) {
            int gridX = light % 16;
            int gridY = light / 16;
            int base = (1 + LIGHT_TEXELS * light) * 4;
            values.put(base, 2.5f + gridX * 5.25f);
            values.put(base + 1, 0f);
            values.put(base + 2, 2.5f + gridY * 5.25f);
            values.put(base + 3, 12f);
            values.put(base + 4, 0.65f);
            values.put(base + 5, 0.75f);
            values.put(base + 6, 0.9f);
            values.put(base + 7, 0.8f);
            values.put(base + 8, 0f);
            values.put(base + 9, 0f);
            values.put(base + 10, 0f);
            values.put(base + 11, 0f);
        }
        Image image = new Image(Image.Format.RGBA32F, width, 1, data, ColorSpace.Linear);
        Texture2D texture = new Texture2D(image);
        texture.setMagFilter(Texture.MagFilter.Nearest);
        texture.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texture.setWrap(Texture.WrapMode.EdgeClamp);
        return texture;
    }

    private void addMode(String label, int chunkSize, Material material,
            CeilingIncrementalBenchmark.MapState map) {
        List<Mesh> meshes = CeilingIncrementalBenchmark.buildBenchmarkMeshes(map, chunkSize);
        List<Geometry> geometries = new ArrayList<>(meshes.size());
        int vertices = 0;
        int triangles = 0;
        int roiMeshes = 0;
        int roiVertices = 0;
        int roiTriangles = 0;
        for (int i = 0; i < meshes.size(); i++) {
            Mesh mesh = meshes.get(i);
            Geometry geometry = new Geometry(label + "-" + i, mesh);
            geometry.setMaterial(material);
            geometry.setQueueBucket(RenderQueue.Bucket.Opaque);
            geometry.setBatchHint(Spatial.BatchHint.Never);
            geometry.setShadowMode(RenderQueue.ShadowMode.Off);
            geometries.add(geometry);
            vertices += mesh.getVertexCount();
            triangles += mesh.getTriangleCount();
            if (intersectsView(mesh)) {
                roiMeshes++;
                roiVertices += mesh.getVertexCount();
                roiTriangles += mesh.getTriangleCount();
            }
        }
        modes.add(new Mode(label, chunkSize, geometries, meshes.size(), roiMeshes,
                vertices, triangles, roiVertices, roiTriangles));
    }

    private boolean intersectsView(Mesh mesh) {
        if (!(mesh.getBound() instanceof com.jme3.bounding.BoundingBox bound)) {
            return true;
        }
        Vector3f center = bound.getCenter();
        return center.x + bound.getXExtent() >= CENTER - VIEW_HALF_WIDTH
                && center.x - bound.getXExtent() <= CENTER + VIEW_HALF_WIDTH
                && center.z + bound.getZExtent() >= CENTER - VIEW_HALF_HEIGHT
                && center.z - bound.getZExtent() <= CENTER + VIEW_HALF_HEIGHT;
    }

    private void attachMode(Mode mode) {
        rootNode.detachAllChildren();
        for (Geometry geometry : mode.geometries()) {
            rootNode.attachChild(geometry);
        }
        System.out.printf(Locale.ROOT,
                "mode=%s chunk=%d geometryCount=%d roiBounds=%d vertices=%d triangles=%d roiVertices=%d roiTriangles=%d%n",
                mode.label(), mode.chunkSize(), mode.geometryCount(), mode.roiGeometryCount(),
                mode.vertices(), mode.triangles(), mode.roiVertices(), mode.roiTriangles());
    }

    private enum Stage {
        WARMUP,
        DRAIN_WARMUP,
        MEASURE,
        DRAIN_MEASURE
    }

    private record Mode(String label, int chunkSize, List<Geometry> geometries,
            int geometryCount, int roiGeometryCount, int vertices, int triangles,
            int roiVertices, int roiTriangles) {

        Mode withLabel(String newLabel) {
            List<Geometry> relabeled = geometries.stream().map(geometry -> {
                Geometry copy = new Geometry(newLabel + "-" + geometry.getName(), geometry.getMesh());
                copy.setMaterial(geometry.getMaterial());
                copy.setQueueBucket(geometry.getQueueBucket());
                copy.setBatchHint(Spatial.BatchHint.Never);
                copy.setShadowMode(RenderQueue.ShadowMode.Off);
                return copy;
            }).toList();
            return new Mode(newLabel, chunkSize, relabeled, geometryCount, roiGeometryCount,
                    vertices, triangles, roiVertices, roiTriangles);
        }
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

            pollQueries();
            if (stage == Stage.WARMUP && warmupSubmitted >= WARMUP_FRAMES) {
                stage = Stage.DRAIN_WARMUP;
            }
            if (stage == Stage.DRAIN_WARMUP && pendingQueries.isEmpty()) {
                stage = Stage.MEASURE;
                cpuSamples.clear();
                gpuSamples.clear();
            } else if (stage == Stage.MEASURE && cpuSamples.size() >= MEASURED_FRAMES) {
                stage = Stage.DRAIN_MEASURE;
            }
            if (stage == Stage.DRAIN_MEASURE && pendingQueries.isEmpty()) {
                reportAndAdvance();
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
                    "result=%s cpuSubmitMedianMs=%.3f cpuSubmitP95Ms=%.3f gpuMedianMs=%.3f gpuP95Ms=%.3f cpuSamples=%d gpuSamples=%d%n",
                    mode.label(), medianMillis(cpuSamples), percentileMillis(cpuSamples, 0.95),
                    medianMillis(gpuSamples), percentileMillis(gpuSamples, 0.95),
                    cpuSamples.size(), gpuSamples.size());
            phaseIndex++;
            if (phaseIndex >= modes.size()) {
                System.out.println("ceiling render benchmark complete");
                stop(false);
                return;
            }
            attachMode(modes.get(phaseIndex));
            warmupSubmitted = 0;
            stage = Stage.WARMUP;
            cpuSamples.clear();
            gpuSamples.clear();
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
