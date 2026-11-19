/* Copyright (C) 2026 OpenKeeper, licensed under GPL-3.0-or-later. */
package toniarts.openkeeper.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.app.state.AbstractAppState;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.simsilica.es.EntityData;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.game.state.GameServerState;
import toniarts.openkeeper.game.state.session.LocalGameSession;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.PathUtils;
import toniarts.openkeeper.view.PlayerEntityViewState;
import toniarts.openkeeper.view.PlayerMapViewState;

/** Installed-data opt-in repeated real level lifecycle; forced GC is diagnostic, never a gameplay policy. */
public final class AnimationLevelResidencyProbe extends AbstractAppState {
    private static final Pattern TRACKED_DIRECT_KIB = Pattern.compile("(?m)^Total direct memory held: (\\d+)kb$");
    private enum Phase { LOADING, PLAYING, RETIRING, SETTLING }
    private record Retired(WeakReference<GameClientState> client, WeakReference<PlayerMapViewState> map,
                           WeakReference<PlayerEntityViewState> entities, WeakReference<GameServerState> server,
                           WeakReference<EntityData> database) { }
    private final Main app;
    private final int cycles;
    private final Path output;
    private final boolean abortMapLoading = Boolean.getBoolean("animation.residency.abortMapLoading");
    private final CountDownLatch closed = new CountDownLatch(1);
    private volatile Throwable failure;
    private boolean passed;
    private final List<Retired> retired = new ArrayList<>();
    private final List<String> report = new ArrayList<>();
    private Phase phase = Phase.LOADING;
    private int cycle = 1, frames;
    private long observedStart, previousFrame, maxLoadingFrame, retirementStart;
    private double loadMs = Double.NaN, detachMs, retirementMs;
    private IKwdFile level;
    private CompletableFuture<Void> retirement;

    private AnimationLevelResidencyProbe(Main app, int cycles, Path output) {
        this.app = app; this.cycles = cycles; this.output = output;
        report.add("cycle,epoch_ms,pid,observed_client_to_ready_ms,max_loading_frame_ms,detach_call_ms,retirement_ms,heap_used_bytes,jvm_direct_used_bytes,jvm_direct_capacity_bytes,jme_tracked_direct_kib,renderer_shaders,renderer_textures,renderer_framebuffers,packed_primitive_bytes,retired_clients_alive,retired_maps_alive,retired_entity_views_alive,retired_servers_alive,retired_databases_alive");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Expected DK2 root, report root, cycle count and packed KiB");
        int cycles = Integer.parseInt(args[2]), packed = Integer.parseInt(args[3]);
        if (cycles < 1 || cycles > 20 || packed < 0 || packed > 262144) throw new IllegalArgumentException("Invalid cycles or packed budget");
        // Track BufferUtils/LWJGL allocations too: JVM direct-pool counters alone miss these.
        // Diagnostic tracking and forced GC make these load timings unsuitable as production FPS.
        BufferUtils.setTrackDirectMemoryEnabled(true);
        Path output = Path.of(args[1], "packed-" + packed
                + (Boolean.getBoolean("animation.residency.abortMapLoading") ? "-abort-map" : ""));
        Files.createDirectories(output);
        Files.writeString(output.resolve("probe.pid"), Long.toString(ProcessHandle.current().pid()));
        System.out.println("RESIDENCY_PROCESS pid=" + ProcessHandle.current().pid() + " epoch_ms=" + System.currentTimeMillis());
        PathUtils.setDKIIFolder(PathUtils.fixFilePath(args[0]));
        var params = Main.class.getDeclaredField("params"); params.setAccessible(true);
        params.set(null, new HashMap<>(Map.of("level", "Alcatraz", "nomovies", "")));
        var constructor = Main.class.getDeclaredConstructor(); constructor.setAccessible(true);
        var app = constructor.newInstance();
        var probe = new AnimationLevelResidencyProbe(app, cycles, output);
        var init = Main.class.getDeclaredMethod("initSettings", Main.class); init.setAccessible(true); init.invoke(null, app);
        Main.getUserSettings().setSetting(Settings.Setting.ANIMATION_INTERPOLATION, true);
        Main.getUserSettings().setSetting(Settings.Setting.ANIMATION_PHASE_VARIATION, false);
        Main.getUserSettings().setSetting(Settings.Setting.ANIMATION_PACKED_KIB, packed);
        var settings = new AppSettings(true); settings.setResolution(1280, 720);
        settings.setRenderer(AppSettings.LWJGL_OPENGL33); settings.setFrameRate(60); settings.setVSync(false);
        settings.setTitle("OpenKeeper level residency probe");
        app.setSettings(settings); app.setShowSettings(false); app.setPauseOnLostFocus(false);
        app.getStateManager().attach(probe); app.start();
        if (!probe.closed.await(10, TimeUnit.MINUTES)) { app.stop(); throw new AssertionError("Level residency probe timed out"); }
        app.stop(true);
        if (probe.failure != null) throw new AssertionError("Level residency probe failed", probe.failure);
        if (!probe.passed) throw new AssertionError("Incomplete level cycle report: cycle=" + probe.cycle + " phase=" + probe.phase);
    }

    @Override public void update(float tpf) {
        try {
            app.getRenderer().getStatistics().setEnabled(true);
            long now = System.nanoTime();
            GameClientState client = app.getStateManager().getState(GameClientState.class);
            switch (phase) {
                case LOADING -> {
                    if (client != null) {
                        level = client.getLevelData();
                        if (observedStart == 0) observedStart = now;
                    }
                    if (observedStart != 0 && previousFrame != 0) maxLoadingFrame = Math.max(maxLoadingFrame, now - previousFrame);
                    var map = app.getStateManager().getState(PlayerMapViewState.class);
                    if (abortMapLoading && client != null && map != null && !map.isMapLoaded()) {
                        System.out.println("RESIDENCY_ABORT_MAP cycle=" + cycle);
                        retire(client);
                    } else if (!abortMapLoading && client != null && client.isInitialized() && client.getEntityViewState() != null
                            && client.getEntityViewState().isInitialized()) {
                        level = client.getLevelData();
                        loadMs = (now - observedStart) / 1e6;
                        phase = Phase.PLAYING; frames = 0;
                        System.out.printf(Locale.ROOT, "RESIDENCY_READY cycle=%d observed_load_ms=%.3f max_loading_frame_ms=%.3f%n",
                                cycle, loadMs, maxLoadingFrame / 1e6);
                    }
                }
                case PLAYING -> {
                    if (++frames == 120) {
                        validateCurrentPose(client.getEntityViewState().getRoot());
                        if (cycle == 1 || cycle == cycles) {
                            var capture = app.getStateManager().getState(com.jme3.app.state.ScreenshotAppState.class);
                            if (capture != null) capture.takeScreenshot();
                        }
                        retire(client);
                    }
                }
                case RETIRING -> {
                    if (retirement.isDone() && client == null && app.getStateManager().getState(GameServerState.class) == null) {
                        retirement.join(); retirement = null;
                        retirementMs = (now - retirementStart) / 1e6;
                        phase = Phase.SETTLING; frames = 0;
                        System.gc(); // Controlled post-retirement observation; not timed as application work.
                    }
                }
                case SETTLING -> {
                    if (++frames == 60) System.gc();
                    if (frames == 180) {
                        record();
                        if ((cycle == 1 || cycle == cycles) && Boolean.getBoolean("animation.residency.heapDumpSamples")) {
                            dumpHeap("cycle-" + cycle);
                        }
                        if (cycle == cycles) {
                            if (retired.stream().anyMatch(r -> r.client.get() != null || r.map.get() != null
                                    || r.entities.get() != null || r.server.get() != null || r.database.get() != null)) {
                                if (Boolean.getBoolean("animation.residency.heapDumpOnFailure")) {
                                    dumpHeap("retained-worlds");
                                }
                                throw new AssertionError("Retired world states or entity databases remain reachable after forced GC and settling; inspect CSV");
                            }
                            System.out.println("LEVEL_RESIDENCY_PASS cycles=" + cycles + " retired_owner_references=" + retired.size() * 5
                                    + " (heap/JVM/jME buffer accounting and renderer object counts; not total native/VRAM bytes)");
                            passed = true;
                            app.stop();
                        } else {
                            cycle++; loadMs = Double.NaN; maxLoadingFrame = 0; observedStart = System.nanoTime();
                            LocalGameSession.createLocalGame(level, false, app.getStateManager(), app, null);
                            phase = Phase.LOADING;
                        }
                    }
                }
            }
            previousFrame = now;
        } catch (Throwable error) { failure = error; error.printStackTrace(); app.stop(); }
    }

    private void retire(GameClientState client) {
        var server = app.getStateManager().getState(GameServerState.class);
        if (server == null) throw new AssertionError("Missing actual local server");
        retired.add(new Retired(new WeakReference<>(client),
                new WeakReference<>(app.getStateManager().getState(PlayerMapViewState.class)),
                new WeakReference<>(client.getEntityViewState()), new WeakReference<>(server),
                new WeakReference<>(((LocalGameSession) client.getGameClientService()).getServerEntityData())));
        retirement = CompletableFuture.allOf(client.whenLoadingStopped().toCompletableFuture(), server.whenStopped().toCompletableFuture());
        retirementStart = System.nanoTime();
        client.detach(); detachMs = (System.nanoTime() - retirementStart) / 1e6;
        phase = Phase.RETIRING;
    }

    private void dumpHeap(String label) throws java.io.IOException {
        Path dump = output.resolve(label + "-" + System.currentTimeMillis() + ".hprof");
        ManagementFactory.getPlatformMXBean(com.sun.management.HotSpotDiagnosticMXBean.class)
                .dumpHeap(dump.toString(), true);
        System.out.println("RESIDENCY_HEAP_DUMP " + dump);
    }

    private static void validateCurrentPose(Spatial root) {
        long[] checked = {0};
        var point = new Vector3f();
        root.depthFirstTraversal(node -> {
            var composer = node.getControl(AnimComposer.class);
            if (composer == null) return;
            for (var clip : composer.getAnimClips()) for (var track : clip.getTracks()) if (track instanceof PoseTrack pose) {
                var target = pose.getTarget();
                var positions = (FloatBuffer) target.getMesh().getBuffer(VertexBuffer.Type.Position).getData();
                for (int i = 0; i < positions.limit(); i += 3) {
                    point.set(positions.get(i), positions.get(i + 1), positions.get(i + 2));
                    if (!Vector3f.isValidVector(point) || !target.getModelBound().contains(point))
                        throw new AssertionError("Invalid actual level pose: " + target.getName());
                    checked[0]++;
                }
            }
        });
        if (checked[0] < 100) throw new AssertionError("Actual level animation was not exercised");
    }

    private void record() throws Exception {
        long epoch = System.currentTimeMillis();
        var tracked = new StringBuilder();
        BufferUtils.printCurrentDirectMemory(tracked);
        Files.writeString(output.resolve("buffers-cycle-" + cycle + ".txt"), tracked);
        var match = TRACKED_DIRECT_KIB.matcher(tracked);
        if (!match.find()) throw new AssertionError("Pinned jME buffer tracking unavailable: " + tracked);
        long trackedKiB = Long.parseLong(match.group(1));
        long directUsed = 0, directCapacity = 0;
        for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) if (pool.getName().equals("direct")) {
            directUsed = pool.getMemoryUsed(); directCapacity = pool.getTotalCapacity();
        }
        var stats = app.getRenderer().getStatistics(); var values = new int[stats.getLabels().length]; stats.getData(values);
        String row = String.format(Locale.ROOT, "%d,%d,%d,%.6f,%.6f,%.6f,%.6f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d",
                cycle, epoch, ProcessHandle.current().pid(), loadMs, maxLoadingFrame / 1e6, detachMs, retirementMs,
                ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(), directUsed, directCapacity,
                trackedKiB, values[6], values[9], values[12], AssetUtils.getAnimationDataPrimitiveBytes(app.getAssetManager()),
                retired.stream().filter(r -> r.client.get() != null).count(), retired.stream().filter(r -> r.map.get() != null).count(),
                retired.stream().filter(r -> r.entities.get() != null).count(), retired.stream().filter(r -> r.server.get() != null).count(),
                retired.stream().filter(r -> r.database.get() != null).count());
        report.add(row); Files.write(output.resolve("level-residency.csv"), report);
        System.out.println("RESIDENCY_CYCLE " + row);
    }

    @Override public void cleanup() { try { super.cleanup(); } finally { closed.countDown(); } }
}
