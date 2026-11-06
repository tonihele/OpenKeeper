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
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import java.awt.image.BufferedImage;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.view.PlayerCameraState;

/**
 * Settle → warm-up → measure (with trends, mid-wave reinforce, visual
 * checkpoints) → export → optional baseline compare → exit.
 */
public final class BenchmarkAppState extends AbstractAppState {

    private static final Logger logger = System.getLogger(BenchmarkAppState.class.getName());

    private enum Phase {
        WAIT_READY,
        SETTLE,
        WARMUP,
        MEASURE,
        CAPTURE_HOLD,
        FINISHING
    }

    private final BenchmarkConfig config;
    private final BenchmarkScenario scenario;
    private final BenchmarkSampler sampler = new BenchmarkSampler();

    private Main app;
    private AppStateManager stateManager;
    private VisualRegression visual;
    private Phase phase = Phase.WAIT_READY;
    private double phaseElapsed;
    private double measureElapsed;
    private boolean finished;
    private boolean reinforced;
    private final List<Double> pendingCheckpoints = new ArrayList<>();
    private int captureHoldFrames;
    private int captureIndex;
    private String captureName;

    public BenchmarkAppState(BenchmarkConfig config) {
        this.config = config;
        this.scenario = new BenchmarkScenario(config);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
        this.app = (Main) app;
        Path runDir = config.getOutputPath().toAbsolutePath().getParent();
        if (runDir == null) {
            runDir = Path.of("build", "benchmark-results");
        }
        runDir = runDir.resolve(stripExtension(config.getOutputPath().getFileName().toString()));
        this.visual = config.isVisual() ? new VisualRegression(config, runDir) : null;
        for (double fraction : config.getVisualCheckpointFractions()) {
            pendingCheckpoints.add(Math.max(0.0, Math.min(1.0, fraction)) * config.getDurationSeconds());
        }
        pendingCheckpoints.sort(Double::compareTo);
        logger.log(Level.INFO,
                "Benchmark armed: suite={0} workload={1} level={2} seed={3} warmup={4}s duration={5}s visual={6} out={7}",
                config.isSuite(), config.getWorkloadId(), config.getLevel(), config.getSeed(),
                config.getWarmupSeconds(), config.getDurationSeconds(), config.isVisual(),
                config.getOutputPath());
    }

    @Override
    public void update(float tpf) {
        if (finished || !isEnabled()) {
            return;
        }

        GameClientState client = stateManager.getState(GameClientState.class);
        PlayerCameraState camera = stateManager.getState(PlayerCameraState.class);

        switch (phase) {
            case WAIT_READY -> {
                phaseElapsed += tpf;
                if (phaseElapsed >= 5.0) {
                    logger.log(Level.INFO,
                            "Benchmark waiting: client={0} started={1} mapView={2} mapLoaded={3} camera={4}",
                            client != null, client != null && client.isGameStarted(),
                            client != null && client.getPlayerMapViewState() != null,
                            client != null && client.getPlayerMapViewState() != null
                                    && client.getPlayerMapViewState().isMapLoaded(),
                            camera != null && camera.isInitialized());
                    phaseElapsed = 0.0;
                }
                if (client == null || !client.isGameStarted()) {
                    return;
                }
                if (client.getPlayerMapViewState() == null || !client.getPlayerMapViewState().isMapLoaded()) {
                    return;
                }
                // Camera may initialize a frame or two after game start
                if (camera == null || !camera.isInitialized()) {
                    return;
                }
                scenario.prepare(client, camera);
                if (!scenario.isPrepared()) {
                    return;
                }
                enter(Phase.SETTLE);
            }
            case SETTLE -> {
                scenario.updateCamera(camera, phaseElapsed);
                phaseElapsed += tpf;
                if (phaseElapsed >= config.getSettleSeconds()) {
                    enter(Phase.WARMUP);
                }
            }
            case WARMUP -> {
                scenario.updateCamera(camera, config.getSettleSeconds() + phaseElapsed);
                phaseElapsed += tpf;
                if (phaseElapsed >= config.getWarmupSeconds()) {
                    sampler.begin(app);
                    sampler.beginRebuildWindow(client);
                    enter(Phase.MEASURE);
                }
            }
            case MEASURE -> {
                scenario.updateCamera(camera, config.getSettleSeconds() + config.getWarmupSeconds() + measureElapsed);
                sampler.sample(app, client, measureElapsed);
                measureElapsed += tpf;
                phaseElapsed += tpf;

                if (!reinforced && measureElapsed >= config.getDurationSeconds() * 0.5) {
                    scenario.reinforce(client, Math.max(20, config.getCreatureCount() / 3),
                            Math.max(10, config.getImpCount() / 3));
                    reinforced = true;
                    logger.log(Level.INFO, "Benchmark mid-run reinforce wave applied");
                }

                if (config.isVisual() && !pendingCheckpoints.isEmpty()
                        && measureElapsed + 1e-6 >= pendingCheckpoints.get(0)) {
                    pendingCheckpoints.remove(0);
                    captureIndex = config.getVisualCheckpointFractions().size() - pendingCheckpoints.size() - 1;
                    captureName = String.format("cp%02d", captureIndex);
                    scenario.applyCheckpointCamera(camera, captureIndex);
                    captureHoldFrames = 2; // settle one present + one capture frame
                    enter(Phase.CAPTURE_HOLD);
                    return;
                }

                if (measureElapsed >= config.getDurationSeconds() && pendingCheckpoints.isEmpty()) {
                    finish();
                }
            }
            case CAPTURE_HOLD -> {
                scenario.applyCheckpointCamera(camera, captureIndex);
                captureHoldFrames--;
                if (captureHoldFrames <= 0) {
                    try {
                        if (visual != null && app.getRenderer() != null) {
                            BufferedImage image = FrameGrabber.capture(app);
                            Map<String, Object> result = visual.recordCheckpoint(captureName, image);
                            logger.log(Level.INFO, "Visual checkpoint {0}: {1}", captureName, result.get("status"));
                        }
                    } catch (Exception ex) {
                        logger.log(Level.ERROR, "Visual checkpoint failed: " + captureName, ex);
                    }
                    enter(Phase.MEASURE);
                    if (measureElapsed >= config.getDurationSeconds() && pendingCheckpoints.isEmpty()) {
                        finish();
                    }
                }
            }
            case FINISHING -> {
            }
        }
    }

    private void enter(Phase next) {
        phase = next;
        phaseElapsed = 0.0;
        logger.log(Level.INFO, "Benchmark phase → {0}", next);
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        phase = Phase.FINISHING;
        try {
            GameClientState client = stateManager.getState(GameClientState.class);
            Map<String, Object> metrics = sampler.summarize(measureElapsed, client);
            Map<String, Object> scenarioReport = new LinkedHashMap<>(scenario.getReport());
            scenarioReport.put("settleSeconds", config.getSettleSeconds());
            scenarioReport.put("warmupSeconds", config.getWarmupSeconds());
            scenarioReport.put("measureSeconds", measureElapsed);
            scenarioReport.put("reinforced", reinforced);

            Map<String, Object> document = BenchmarkResultWriter.buildDocument(
                    config, scenarioReport, metrics, app,
                    "Suite framework v" + BenchmarkConfig.FRAMEWORK_VERSION
                            + ". Wall-clock frame times; CapFrameX-style 1%/0.1% lows. "
                            + "GPU timer queries not instrumented (gpuFrameTimeMs=null). "
                            + "Visual checkpoints use fixed camera poses; soft SSIM/MAE thresholds "
                            + "absorb hardware numerical variance.");
            if (visual != null) {
                document.put("visual", visual.summarize());
            }

            if (config.getCompareBaselinePath() != null) {
                document.put("regression", RegressionComparer.compareFile(
                        config.getCompareBaselinePath(), document));
            }

            Path written = BenchmarkResultWriter.write(config.getOutputPath(), document);
            logger.log(Level.INFO, "Benchmark results written to {0}", written);
            logger.log(Level.INFO, "Benchmark summary: avgFps={0} onePercentLowFps={1} frames={2} visualPass={3}",
                    metrics.get("avgFps"), metrics.get("onePercentLowFps"), metrics.get("frames"),
                    visual == null ? "n/a" : visual.summarize().get("passed"));
        } catch (Exception ex) {
            logger.log(Level.ERROR, "Failed to write benchmark results", ex);
        } finally {
            app.stop();
        }
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
