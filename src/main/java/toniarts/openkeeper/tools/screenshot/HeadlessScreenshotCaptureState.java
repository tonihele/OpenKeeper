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

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.modelviewer.MapLoaderAppState;

/**
 * Applies camera poses and requests ordered framebuffer captures in the offscreen renderer.
 */
final class HeadlessScreenshotCaptureState extends AbstractAppState {

    private static final Logger LOGGER = Logger.getLogger(HeadlessScreenshotCaptureState.class.getName());
    private static final int MAX_WAIT_FRAMES = 600;

    private final IKwdFile map;
    private final MapLoaderAppState mapLoader;
    private final ScreenshotCaptureConfig config;
    private final OffscreenPngCaptureProcessor captureProcessor;
    private final List<ScreenshotCaptureConfig.Shot> shots;
    private final HeadlessScreenshotCapture.OffscreenApplication application;
    private AppStateManager stateManager;
    private Camera camera;
    private int startupFrames;
    private int shotIndex;
    private int settleFrames;
    private int waitFrames;
    private boolean overlaysHidden;
    private boolean cameraSet;
    private Path pendingFile;

    HeadlessScreenshotCaptureState(
            IKwdFile map,
            MapLoaderAppState mapLoader,
            ScreenshotCaptureConfig config,
            OffscreenPngCaptureProcessor captureProcessor,
            HeadlessScreenshotCapture.OffscreenApplication application) {
        this.map = map;
        this.mapLoader = mapLoader;
        this.config = config;
        this.captureProcessor = captureProcessor;
        this.application = application;
        this.shots = config.shots();
        this.startupFrames = config.startupFrames();
    }

    @Override
    public void initialize(AppStateManager stateManager, com.jme3.app.Application app) {
        super.initialize(stateManager, app);
        this.stateManager = stateManager;
        camera = app.getCamera();
        try {
            mapLoader.setCeilingSubdivisions(config.ceilingSubdivisions());
            Node mapNode = mapLoader.loadMap(map, true, true, true);
            ((SimpleApplication) app).getRootNode().attachChild(mapNode);
        } catch (RuntimeException ex) {
            fail("Could not build the map scene", ex);
        }
    }

    @Override
    public void update(float tpf) {
        if (!overlaysHidden) {
            StatsAppState stats = stateManager.getState(StatsAppState.class);
            if (stats != null) {
                stats.setEnabled(false);
            }
            overlaysHidden = true;
        }

        IOException captureFailure = captureProcessor.getFailure();
        if (captureFailure != null) {
            fail("Offscreen screenshot failed", captureFailure);
            return;
        }

        if (startupFrames > 0) {
            startupFrames--;
            return;
        }

        if (pendingFile != null) {
            if (pendingFile.equals(captureProcessor.getCapturedFile()) && Files.isRegularFile(pendingFile)) {
                LOGGER.info(() -> "Captured " + pendingFile);
                pendingFile = null;
                cameraSet = false;
                shotIndex++;
            } else if (++waitFrames > MAX_WAIT_FRAMES) {
                fail("Timed out waiting for screenshot " + pendingFile, null);
            }
            return;
        }

        if (shotIndex >= shots.size()) {
            LOGGER.info("Headless screenshot capture complete");
            application.stop();
            return;
        }

        ScreenshotCaptureConfig.Shot shot = shots.get(shotIndex);
        if (!cameraSet) {
            mapLoader.setCeilingVisible(shot.ceilingVisible());
            setCamera(shot);
            settleFrames = shot.settleFrames();
            cameraSet = true;
            return;
        }

        if (settleFrames > 0) {
            settleFrames--;
            return;
        }

        pendingFile = config.outputDirectory().resolve(shot.fileName());
        try {
            captureProcessor.requestCapture(pendingFile);
            waitFrames = 0;
        } catch (RuntimeException ex) {
            fail("Could not request screenshot " + pendingFile, ex);
        }
    }

    private void setCamera(ScreenshotCaptureConfig.Shot shot) {
        camera.setLocation(shot.position());
        Vector3f lookAt = shot.lookAt();
        if (lookAt != null) {
            camera.lookAt(lookAt, Vector3f.UNIT_Y);
        } else {
            Vector3f rotation = shot.rotationDegrees();
            Quaternion orientation = new Quaternion().fromAngles(
                    rotation.y * FastMath.DEG_TO_RAD,
                    rotation.x * FastMath.DEG_TO_RAD,
                    rotation.z * FastMath.DEG_TO_RAD);
            camera.setRotation(orientation);
        }
        camera.setFrustumPerspective(shot.fovY(), camera.getWidth() / (float) camera.getHeight(),
                config.nearClip(), camera.getFrustumFar());
    }

    private void fail(String message, Throwable cause) {
        if (cause == null) {
            LOGGER.log(Level.SEVERE, message);
            application.setFailure(new IOException(message));
        } else {
            LOGGER.log(Level.SEVERE, message, cause);
            application.setFailure(cause);
        }
        application.stop();
    }
}
