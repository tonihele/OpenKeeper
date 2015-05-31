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
package toniarts.openkeeper.cinematics;

import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.events.MotionEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;
import java.awt.Point;
import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.world.MapLoader;

/**
 * Our wrapper on JME cinematic class, produces ready cinematics from camera
 * sweep files.<br>
 * This extends the JME's own Cinematic, so adding effects etc. is as easy as
 * possible.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Cinematic extends com.jme3.cinematic.Cinematic {

    private final AssetManager assetManager;
    private static final Logger logger = Logger.getLogger(Cinematic.class.getName());
    private static final boolean IS_DEBUG = false;
    private static final String CAMERA_NAME = "Motion cam";
    private final CameraSweepData cameraSweepData;

    /**
     * Creates a new cinematic ready for consumption
     *
     * @param assetManager asset manager instance
     * @param cam the camera to use
     * @param start starting map coordinates, zero based
     * @param cameraSweepFile the camera sweep file name that is the basis for
     * this animation (without the extension)
     * @param scene scene node to attach to
     */
    public Cinematic(AssetManager assetManager, Camera cam, Point start, String cameraSweepFile, Node scene) {
        this(assetManager, cam, MapLoader.getCameraPositionOnMapPoint(start.x, start.y), cameraSweepFile, scene);
    }

    /**
     * Creates a new cinematic ready for consumption
     *
     * @param assetManager asset manager instance
     * @param cam the camera to use
     * @param start starting location, zero based
     * @param cameraSweepFile the camera sweep file name that is the basis for
     * this animation (without the extension)
     * @param scene scene node to attach to
     */
    public Cinematic(AssetManager assetManager, final Camera cam, final Vector3f start, String cameraSweepFile, Node scene) {
        super(scene);
        this.assetManager = assetManager;

        // Load up the camera sweep file
        Object obj = assetManager.loadAsset(AssetsConverter.PATHS_FOLDER.concat(File.separator).replaceAll(Pattern.quote("\\"), "/").concat(cameraSweepFile.concat(".").concat(CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION)));
        if (obj == null || !(obj instanceof CameraSweepData)) {
            String msg = "Failed to load the camera sweep file " + cameraSweepFile + "!";
            logger.severe(msg);
            throw new RuntimeException(msg);
        }
        cameraSweepData = (CameraSweepData) obj;

        // Initialize
        initializeCinematic(scene, cam, start);

        // Set the camera as a first step
        activateCamera(0, CAMERA_NAME);
    }

    /**
     * Creates the actual cinematic
     *
     * @param scene the scene to attach to
     */
    private void initializeCinematic(Node scene, final Camera cam, final Vector3f startLocation) {
        final CameraNode camNode = bindCamera(CAMERA_NAME, cam);
        camNode.setControlDir(ControlDirection.SpatialToCamera);
        final MotionPath path = new MotionPath();
        path.setCycle(false);

        // The waypoints
        for (CameraSweepDataEntry entry : cameraSweepData.getEntries()) {
            path.addWayPoint(entry.getPosition().mult(MapLoader.TILE_WIDTH).addLocal(startLocation));
        }
        path.setCurveTension(0);
        if (IS_DEBUG) {
            path.enableDebugShape(assetManager, scene);
        }

        final MotionEvent cameraMotionControl = new MotionEvent(camNode, path) {
            @Override
            public void update(float tpf) {
                super.update(tpf);

                // Rotate
                float progress = getCurrentValue();
                int startIndex = getCurrentWayPoint();

                // Get the rotation at previous (or current) waypoint
                CameraSweepDataEntry entry = cameraSweepData.getEntries().get(startIndex);
                Quaternion q1 = new Quaternion(entry.getRotation());

                // If we are not on the last waypoint, interpolate the rotation between waypoints
                CameraSweepDataEntry entryNext = cameraSweepData.getEntries().get(startIndex + 1);
                Quaternion q2 = new Quaternion(entryNext.getRotation());

                q1.slerp(q2, progress);

                // Set the rotation
                setRotation(q1);

                // Set the near
                cam.setFrustumNear(FastMath.interpolateLinear(progress, entry.getNear(), entryNext.getNear()) / 4096f);
                cam.setFrustumPerspective(60f, (float) cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
                // cam.setFrustumPerspective(FastMath.RAD_TO_DEG * FastMath.interpolateLinear(progress, cameraSweepData.getEntries().get(startIndex).getFov(), cameraSweepData.getEntries().get(endIndex).getFov()), (float) cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
            }

            @Override
            public void onStop() {
                super.onStop();

                // We never reach the final point
                // FIXME: Also this is not quaranteed to be run, so the camera might be in a funny location
                CameraSweepDataEntry entry = cameraSweepData.getEntries().get(cameraSweepData.getEntries().size() - 1);

                // Set Position
                cam.setLocation(startLocation.add(entry.getPosition()));

                // Set the rotation
                Quaternion q = new Quaternion(entry.getRotation());
                setRotation(q);

//                cam.setFrustumPerspective(FastMath.RAD_TO_DEG * entry.getFov(), (float) cam.getWidth() / cam.getHeight(), 0.1f, 1000f);
            }
        };
        cameraMotionControl.setLoopMode(LoopMode.DontLoop);
        cameraMotionControl.setInitialDuration(cameraSweepData.getEntries().size() / getFramesPerSecond());
        cameraMotionControl.setDirectionType(MotionEvent.Direction.Rotation);

        // Add us
        addCinematicEvent(0, cameraMotionControl);

        // Set duration of the whole animation
        setInitialDuration(cameraSweepData.getEntries().size() / getFramesPerSecond());
    }

    /**
     * Animation speed, FPS
     *
     * @return FPS
     */
    protected float getFramesPerSecond() {
        return 30f;
    }
}
