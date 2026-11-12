/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.view.lighting.LightRules;

/**
 * CPU microbenchmark for the work formerly done on every stable entity
 * model on every frame. It deliberately measures only material traversal,
 * binding and the per-container array snapshots, not whole-game frame time.
 *
 * Run with {@code .\gradlew.bat entityMaterialSetupBenchmark}.
 */
public final class EntityMaterialSetupBenchmark {

    private static final int OBJECTS = 128;
    private static final int CREATURES = 256;
    private static final int DOORS = 32;
    private static final int TRAPS = 64;
    private static final int GEOMETRIES_PER_MODEL = 5;
    private static final int WARMUP_FRAMES = 20;
    private static final int MEASURED_FRAMES = 100;
    private static final int SAMPLES = 20;

    private static volatile int sink;

    private EntityMaterialSetupBenchmark() {
    }

    public static void main(String[] args) {
        AssetManager assetManager = new DesktopAssetManager(true);
        LightRules lightRules = new LightRules();
        lightRules.initAmbientMap(85, 85);

        Spatial[][] models = {
            createModels(assetManager, "Object", OBJECTS),
            createModels(assetManager, "Creature", CREATURES),
            createModels(assetManager, "Door", DOORS),
            createModels(assetManager, "Trap", TRAPS)
        };
        boolean[] tileAmbient = {true, false, true, true};
        Map<Spatial, Boolean> pendingModels = new IdentityHashMap<>();

        ThreadMXBean allocationBean = allocationBean();
        long threadId = Thread.currentThread().threadId();
        long setupAllocatedBefore = allocatedBytes(allocationBean, threadId);
        long setupStart = System.nanoTime();
        prepareAll(models, tileAmbient, assetManager, lightRules);
        long setupNanos = System.nanoTime() - setupStart;
        long setupAllocated = difference(setupAllocatedBefore, allocatedBytes(allocationBean, threadId));

        for (int frame = 0; frame < WARMUP_FRAMES; frame++) {
            legacyFrame(models, tileAmbient, assetManager, lightRules);
            if (changeDrivenFrame(lightRules, pendingModels)) {
                sink++;
            }
        }

        long[] legacyNanos = new long[SAMPLES];
        long[] changedNanos = new long[SAMPLES];
        long[] legacyAllocated = new long[SAMPLES];
        long[] changedAllocated = new long[SAMPLES];

        for (int sample = 0; sample < SAMPLES; sample++) {
            if ((sample & 1) == 0) {
                measureLegacy(sample, models, tileAmbient, assetManager, lightRules,
                        allocationBean, threadId, legacyNanos, legacyAllocated);
                measureChanged(sample, lightRules, pendingModels, allocationBean, threadId, changedNanos, changedAllocated);
            } else {
                measureChanged(sample, lightRules, pendingModels, allocationBean, threadId, changedNanos, changedAllocated);
                measureLegacy(sample, models, tileAmbient, assetManager, lightRules,
                        allocationBean, threadId, legacyNanos, legacyAllocated);
            }
        }

        int modelCount = Arrays.stream(models).mapToInt(array -> array.length).sum();
        int geometryCount = modelCount * GEOMETRIES_PER_MODEL;
        System.out.printf("java=%s os=%s/%s processors=%d%n",
                System.getProperty("java.version"), System.getProperty("os.name"),
                System.getProperty("os.arch"), Runtime.getRuntime().availableProcessors());
        System.out.printf("models=%d geometries=%d frames/sample=%d samples=%d%n",
                modelCount, geometryCount, MEASURED_FRAMES, SAMPLES);
        System.out.printf("one-time model setup: %.3f ms total %d bytes allocated%n",
                setupNanos / 1_000_000.0, setupAllocated);
        report("legacy recurring setup", legacyNanos, legacyAllocated);
        report("change-driven stable frame", changedNanos, changedAllocated);
        System.out.printf("legacy total for %d stable frames: %.3f ms%n",
                MEASURED_FRAMES, median(legacyNanos) / 1_000_000.0 * MEASURED_FRAMES);

        // Keep the measured model set reachable through the end of the run.
        sink = sink ^ models[0][0].getName().length();
    }

    private static Spatial[] createModels(AssetManager assetManager, String type, int count) {
        Spatial[] models = new Spatial[count];
        for (int modelIndex = 0; modelIndex < count; modelIndex++) {
            Node model = new Node(type + modelIndex);
            for (int geometryIndex = 0; geometryIndex < GEOMETRIES_PER_MODEL; geometryIndex++) {
                Geometry geometry = new Geometry(type + modelIndex + "-" + geometryIndex, new Quad(1, 1));
                Material material = new Material(assetManager, "MatDefs/DkLighting/Flat.j3md");
                material.setName(type + " material");
                geometry.setMaterial(material);
                model.attachChild(geometry);
            }
            models[modelIndex] = model;
        }
        return models;
    }

    private static void prepareAll(Spatial[][] models, boolean[] tileAmbient,
            AssetManager assetManager, LightRules lightRules) {
        for (int kind = 0; kind < models.length; kind++) {
            for (Spatial model : models[kind]) {
                AssetUtils.applyVertexLight(assetManager, model, lightRules, false, tileAmbient[kind]);
            }
        }
    }

    private static void legacyFrame(Spatial[][] models, boolean[] tileAmbient,
            AssetManager assetManager, LightRules lightRules) {
        for (int kind = 0; kind < models.length; kind++) {
            // EntityContainer.getArray() returns a snapshot, matching the
            // four array copies made by the original PlayerEntityViewState.
            Spatial[] snapshot = models[kind].clone();
            for (Spatial model : snapshot) {
                AssetUtils.applyVertexLight(assetManager, model, lightRules, false, tileAmbient[kind]);
            }
        }
    }

    private static boolean changeDrivenFrame(LightRules lightRules, Map<Spatial, Boolean> pendingModels) {
        // This is the stable path left in PlayerEntityViewState.update():
        // pending setup is checked, but no entity material is traversed.
        return lightRules.isAmbientMapInitialized() && pendingModels.isEmpty();
    }

    private static void measureLegacy(int sample, Spatial[][] models, boolean[] tileAmbient,
            AssetManager assetManager, LightRules lightRules, ThreadMXBean bean, long threadId,
            long[] nanos, long[] allocated) {
        long allocatedBefore = allocatedBytes(bean, threadId);
        long start = System.nanoTime();
        for (int frame = 0; frame < MEASURED_FRAMES; frame++) {
            legacyFrame(models, tileAmbient, assetManager, lightRules);
        }
        nanos[sample] = System.nanoTime() - start;
        allocated[sample] = difference(allocatedBefore, allocatedBytes(bean, threadId));
    }

    private static void measureChanged(int sample, LightRules lightRules, Map<Spatial, Boolean> pendingModels,
            ThreadMXBean bean, long threadId,
            long[] nanos, long[] allocated) {
        long allocatedBefore = allocatedBytes(bean, threadId);
        long start = System.nanoTime();
        int framesWithNoPendingSetup = 0;
        for (int frame = 0; frame < MEASURED_FRAMES; frame++) {
            if (changeDrivenFrame(lightRules, pendingModels)) {
                framesWithNoPendingSetup++;
            }
        }
        sink = framesWithNoPendingSetup;
        nanos[sample] = System.nanoTime() - start;
        allocated[sample] = difference(allocatedBefore, allocatedBytes(bean, threadId));
    }

    private static ThreadMXBean allocationBean() {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        if (bean instanceof ThreadMXBean extended && extended.isThreadAllocatedMemorySupported()) {
            if (!extended.isThreadAllocatedMemoryEnabled()) {
                extended.setThreadAllocatedMemoryEnabled(true);
            }
            return extended;
        }
        return null;
    }

    private static long allocatedBytes(ThreadMXBean bean, long threadId) {
        return bean != null ? bean.getThreadAllocatedBytes(threadId) : -1;
    }

    private static long difference(long before, long after) {
        return before >= 0 && after >= before ? after - before : -1;
    }

    private static void report(String label, long[] nanos, long[] allocated) {
        double medianMillisPerFrame = median(nanos) / 1_000_000.0 / MEASURED_FRAMES;
        double p95MillisPerFrame = percentile(nanos, 0.95) / 1_000_000.0 / MEASURED_FRAMES;
        long allocationMedian = median(allocated) / MEASURED_FRAMES;
        System.out.printf("%s: median=%.4f ms/frame p95_batch_avg=%.4f ms/frame allocation=%d bytes/frame%n",
                label, medianMillisPerFrame, p95MillisPerFrame, allocationMedian);
    }

    private static long median(long[] samples) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    private static long percentile(long[] samples, double percentile) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index];
    }
}
