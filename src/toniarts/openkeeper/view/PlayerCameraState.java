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
package toniarts.openkeeper.view;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.cinematics.CameraSweepData;
import toniarts.openkeeper.cinematics.CameraSweepDataEntry;
import toniarts.openkeeper.cinematics.CameraSweepDataLoader;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.world.MapLoader;

/**
 * The player camera state. Listens for camera movement inputs.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerCameraState extends AbstractAppState implements ActionListener, AnalogListener {

    private Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private PlayerCamera camera;
    private final Player player;
    private Vector3f startLocation;
    private boolean rotate = false;
    private static final Logger logger = Logger.getLogger(PlayerCameraState.class.getName());
    // Extra keys
    private static final String CAMERA_MOUSE_ZOOM_IN = "CAMERA_MOUSE_ZOOM_IN";
    private static final String CAMERA_MOUSE_ZOOM_OUT = "CAMERA_MOUSE_ZOOM_OUT";
    // User set keys
    private static String[] mappings = new String[]{
        Settings.Setting.CAMERA_DOWN.name(),
        Settings.Setting.CAMERA_LEFT.name(),
        Settings.Setting.CAMERA_RIGHT.name(),
        Settings.Setting.CAMERA_ROTATE.name(),
        Settings.Setting.CAMERA_ROTATE_LEFT.name(),
        Settings.Setting.CAMERA_ROTATE_RIGHT.name(),
        Settings.Setting.CAMERA_UP.name(),
        Settings.Setting.CAMERA_ZOOM_IN.name(),
        Settings.Setting.CAMERA_ZOOM_OUT.name(),
        CAMERA_MOUSE_ZOOM_IN,
        CAMERA_MOUSE_ZOOM_OUT
    };

    public PlayerCameraState(Player player) {
        this.player = player;
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();

        // The camera
        if (camera == null) {
            camera = new PlayerCamera(app.getCamera());
        }
        loadCameraStartLocation();

        // The controls
        rotate = false;
        registerInput();
    }

    /**
     * Load the initial main menu camera position
     */
    private void loadCameraStartLocation() {
        startLocation = new Vector3f(MapLoader.getCameraPositionOnMapPoint(player.getStartingCameraX(), player.getStartingCameraY()));

        // Set the actual camera location
        CameraSweepData csd = (CameraSweepData) assetManager.loadAsset(AssetsConverter.PATHS_FOLDER.concat(File.separator).replaceAll(Pattern.quote("\\"), "/").concat("EnginePath201".concat(".").concat(CameraSweepDataLoader.CAMERA_SWEEP_DATA_FILE_EXTENSION)));
        CameraSweepDataEntry lastEntry = csd.getEntries().get(csd.getEntries().size() - 1);
        app.getCamera().setRotation(lastEntry.getRotation().clone());
        app.getCamera().setLocation(startLocation.addLocal(csd.getEntries().get(csd.getEntries().size() - 1).getPosition()).clone());
    }

    @Override
    public void cleanup() {

        // Unregister controls
        unregisterInput();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
    }

    private void registerInput() {

        // Add the keys
        Settings settings = app.getUserSettings();
        inputManager.addMapping(Settings.Setting.CAMERA_DOWN.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_DOWN)));
        inputManager.addMapping(Settings.Setting.CAMERA_LEFT.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_LEFT)));
        inputManager.addMapping(Settings.Setting.CAMERA_RIGHT.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_RIGHT)));
        inputManager.addMapping(Settings.Setting.CAMERA_ROTATE.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_ROTATE)));
        inputManager.addMapping(Settings.Setting.CAMERA_ROTATE_LEFT.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_ROTATE_LEFT)));
        inputManager.addMapping(Settings.Setting.CAMERA_ROTATE_RIGHT.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_ROTATE_RIGHT)));
        inputManager.addMapping(Settings.Setting.CAMERA_UP.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_UP)));
        inputManager.addMapping(Settings.Setting.CAMERA_ZOOM_IN.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_ZOOM_IN)));
        inputManager.addMapping(Settings.Setting.CAMERA_ZOOM_OUT.name(), new KeyTrigger(settings.getSettingInteger(Settings.Setting.CAMERA_ZOOM_OUT)));

        // Extra
        inputManager.addMapping(CAMERA_MOUSE_ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(CAMERA_MOUSE_ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        inputManager.addListener(this, mappings);
    }

    private void unregisterInput() {
        for (String s : mappings) {
            inputManager.deleteMapping(s);
        }

        inputManager.removeListener(this);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isEnabled()) {
            return;
        }

        if (name.equals(Settings.Setting.CAMERA_ROTATE.name())) {
            rotate = isPressed;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!isEnabled()) {
            return;
        }

        if (name.equals(CAMERA_MOUSE_ZOOM_IN)) {
            camera.zoomCamera(value, true);
        } else if (name.equals(CAMERA_MOUSE_ZOOM_OUT)) {
            camera.zoomCamera(-value, true);
        } else if (name.equals(Settings.Setting.CAMERA_ZOOM_IN.name()) || (rotate && name.equals(Settings.Setting.CAMERA_UP.name()))) {
            camera.zoomCamera(value, false);
        } else if (name.equals(CAMERA_MOUSE_ZOOM_OUT) || name.equals(Settings.Setting.CAMERA_ZOOM_OUT.name()) || (rotate && name.equals(Settings.Setting.CAMERA_DOWN.name()))) {
            camera.zoomCamera(-value, false);
        } else if (name.equals(Settings.Setting.CAMERA_ROTATE_LEFT.name()) || (rotate && name.equals(Settings.Setting.CAMERA_LEFT.name()))) {
            camera.rotateCamera(-value);
        } else if (name.equals(Settings.Setting.CAMERA_ROTATE_RIGHT.name()) || (rotate && name.equals(Settings.Setting.CAMERA_RIGHT.name()))) {
            camera.rotateCamera(value);
        } else if (name.equals(Settings.Setting.CAMERA_DOWN.name())) {
            camera.moveCamera(-value, false);
        } else if (name.equals(Settings.Setting.CAMERA_LEFT.name())) {
            camera.moveCamera(value, true);
        } else if (name.equals(Settings.Setting.CAMERA_RIGHT.name())) {
            camera.moveCamera(-value, true);
        } else if (name.equals(Settings.Setting.CAMERA_UP.name())) {
            camera.moveCamera(value, false);
        }
    }
}
