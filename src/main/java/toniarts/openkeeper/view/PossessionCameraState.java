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
import com.jme3.app.state.AppStateManager;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityId;
import java.lang.System.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.tools.convert.map.Creature;

/**
 * First person view of the possessed creature. Looks around locally and sends
 * the movement intent to the server, which moves the creature
 *
 * @author ArchDemon
 */
public final class PossessionCameraState extends AbstractPauseAwareState implements ActionListener, AnalogListener {

    private static final Logger logger = System.getLogger(PossessionCameraState.class.getName());

    /**
     * Minimum facing change (radians) worth telling the server about
     */
    private static final float ROTATION_SEND_THRESHOLD = 0.05f;
    /**
     * Minimum interval (seconds) between facing-only updates to the server
     */
    private static final float ROTATION_SEND_INTERVAL = 0.1f;

    private Main app;
    private AppStateManager stateManager;
    private InputManager inputManager;

    private EntityId target;
    private Creature creature;
    private Spatial targetSpatial;

    private PossessionCamera camera;
    private boolean inputRegistered = false;

    private boolean moveForward;
    private boolean moveBackward;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean run;
    private boolean creep;

    private final Vector2f sentDirection = new Vector2f();
    private float sentRotation;
    private byte sentSpeedMode;
    private float timeSinceSend;

    private static final String POSSESSION = "POSSESSION_";

    private static final String CAMERA_VIEW_LEFT = "CAMERA_VIEW_LEFT";
    private static final String CAMERA_VIEW_UP = "CAMERA_VIEW_UP";
    private static final String CAMERA_VIEW_RIGHT = "CAMERA_VIEW_RIGHT";
    private static final String CAMERA_VIEW_DOWN = "CAMERA_VIEW_DOWN";

    private static final String[] mappings = new String[]{
        // view
        CAMERA_VIEW_LEFT,
        CAMERA_VIEW_UP,
        CAMERA_VIEW_RIGHT,
        CAMERA_VIEW_DOWN,
        // movement
        POSSESSION + Settings.Setting.CAMERA_UP.name(),
        POSSESSION + Settings.Setting.CAMERA_DOWN.name(),
        POSSESSION + Settings.Setting.CAMERA_LEFT.name(),
        POSSESSION + Settings.Setting.CAMERA_RIGHT.name(),
        Settings.Setting.POSSESSED_RUN.name(),
        Settings.Setting.POSSESSED_CREEP.name()
    };

    public PossessionCameraState(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.stateManager = stateManager;
        inputManager = this.app.getInputManager();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (!isInitialized()) {
            return;
        }

        PlayerEntityViewState entityViewState = stateManager.getState(PlayerEntityViewState.class);
        if (enabled && creature != null) {
            targetSpatial = entityViewState != null ? entityViewState.getEntitySpatial(target) : null;
            camera = new PossessionCamera(app.getCamera(), creature.getAttributes().getSpeed(), creature.getFirstPersonOscillateScale());
            loadCameraStartLocation();
            if (entityViewState != null) {
                entityViewState.setHiddenEntity(target);
            }

            resetMovement();
            registerInput();
        } else {
            unregisterInput();
            resetMovement();
            if (entityViewState != null) {
                entityViewState.setHiddenEntity(null);
            }
            targetSpatial = null;
        }
    }

    /**
     * Load the initial camera position, looking the way the creature faces
     */
    private void loadCameraStartLocation() {
        Camera cam = app.getCamera();
        cam.setFrustumPerspective(45, (float) cam.getWidth() / cam.getHeight(), 0.01f, 1000f);
        if (targetSpatial != null) {
            updateCameraLocation();
            Vector3f facing = targetSpatial.getWorldRotation().mult(Vector3f.UNIT_Z);
            facing.y = 0;
            if (facing.lengthSquared() > 0) {
                cam.lookAtDirection(facing.normalizeLocal(), Vector3f.UNIT_Y);
            }
        } else {
            cam.setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);
        }
        sentRotation = getFacing();
    }

    private void updateCameraLocation() {
        app.getCamera().setLocation(targetSpatial.getWorldTranslation().add(0, creature.getAttributes().getEyeHeight(), 0));
    }

    private void registerInput() {
        if (inputRegistered) {
            return;
        }

        // Add the keys
        Settings settings = Main.getUserSettings();
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_UP.name(), new KeyTrigger(settings.getInteger(Settings.Setting.CAMERA_UP)));
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_DOWN.name(), new KeyTrigger(settings.getInteger(Settings.Setting.CAMERA_DOWN)));
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_LEFT.name(), new KeyTrigger(settings.getInteger(Settings.Setting.CAMERA_LEFT)));
        inputManager.addMapping(POSSESSION + Settings.Setting.CAMERA_RIGHT.name(), new KeyTrigger(settings.getInteger(Settings.Setting.CAMERA_RIGHT)));

        inputManager.addMapping(Settings.Setting.POSSESSED_RUN.name(), new KeyTrigger(settings.getInteger(Settings.Setting.POSSESSED_RUN)));
        inputManager.addMapping(Settings.Setting.POSSESSED_CREEP.name(), new KeyTrigger(settings.getInteger(Settings.Setting.POSSESSED_CREEP)));

        inputManager.addMapping(CAMERA_VIEW_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping(CAMERA_VIEW_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        inputManager.addMapping(CAMERA_VIEW_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping(CAMERA_VIEW_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, false));

        inputManager.addListener(this, mappings);
        inputRegistered = true;
    }

    @Override
    public boolean isPauseable() {
        return false;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isEnabled()) {
            return;
        }

        if (name.equals(POSSESSION + Settings.Setting.CAMERA_UP.name())) {
            moveForward = isPressed;
        } else if (name.equals(POSSESSION + Settings.Setting.CAMERA_DOWN.name())) {
            moveBackward = isPressed;
        } else if (name.equals(POSSESSION + Settings.Setting.CAMERA_LEFT.name())) {
            moveLeft = isPressed;
        } else if (name.equals(POSSESSION + Settings.Setting.CAMERA_RIGHT.name())) {
            moveRight = isPressed;
        } else if (name.equals(Settings.Setting.POSSESSED_RUN.name())) {
            run = isPressed;
        } else if (name.equals(Settings.Setting.POSSESSED_CREEP.name())) {
            creep = isPressed;
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!isEnabled() || camera == null) {
            return;
        }

        switch (name) {
            case CAMERA_VIEW_LEFT -> camera.rotate(value, true);
            case CAMERA_VIEW_RIGHT -> camera.rotate(-value, true);
            case CAMERA_VIEW_UP -> camera.rotate(value, false);
            case CAMERA_VIEW_DOWN -> camera.rotate(-value, false);
        }
    }

    private void unregisterInput() {
        if (!inputRegistered) {
            return;
        }
        for (String s : mappings) {
            inputManager.deleteMapping(s);
        }
        inputManager.removeListener(this);
        inputRegistered = false;
    }

    @Override
    public void cleanup() {

        // Unregister controls
        unregisterInput();

        super.cleanup();
    }

    @Override
    public void update(float tpf) {
        if (targetSpatial != null) {
            updateCameraLocation();
            sendMovement(tpf);
        }

        // Update audio listener position
        app.getListener().setLocation(app.getCamera().getLocation());
        app.getListener().setRotation(app.getCamera().getRotation());
    }

    /**
     * Tell the server where we want to go, only when something changes
     */
    private void sendMovement(float tpf) {
        timeSinceSend += tpf;

        Vector2f direction = computeMovementDirection();
        float rotation = getFacing();
        byte speedMode = getSpeedMode();

        boolean changed = hasMovementChanged(direction, speedMode);
        boolean rotated = Math.abs(rotation - sentRotation) > ROTATION_SEND_THRESHOLD;
        if (changed || (rotated && timeSinceSend >= ROTATION_SEND_INTERVAL)) {
            stateManager.getState(GameClientState.class).getGameClientService().setPossessedMovement(direction, rotation, speedMode);
            sentDirection.set(direction);
            sentRotation = rotation;
            sentSpeedMode = speedMode;
            timeSinceSend = 0;
        }
    }

    /**
     * The camera-relative movement direction from the current input state,
     * normalized
     */
    private Vector2f computeMovementDirection() {
        Camera cam = app.getCamera();
        Vector2f forward = new Vector2f(cam.getDirection().x, cam.getDirection().z);
        Vector2f left = new Vector2f(cam.getLeft().x, cam.getLeft().z);
        if (forward.lengthSquared() > 0) {
            forward.normalizeLocal();
        }
        if (left.lengthSquared() > 0) {
            left.normalizeLocal();
        }

        Vector2f direction = new Vector2f();
        direction.addLocal(forward.mult(axisValue(moveForward, moveBackward)));
        direction.addLocal(left.mult(axisValue(moveLeft, moveRight)));
        if (direction.lengthSquared() > 0) {
            direction.normalizeLocal();
        }
        return direction;
    }

    private static float axisValue(boolean positive, boolean negative) {
        return (positive ? 1 : 0) - (negative ? 1 : 0);
    }

    private byte getSpeedMode() {
        if (run) {
            return PossessedMovement.SPEED_RUN;
        }
        return creep ? PossessedMovement.SPEED_CREEP : PossessedMovement.SPEED_WALK;
    }

    /**
     * Whether the direction or speed mode differ enough from what was last
     * sent to the server to warrant an update
     */
    private boolean hasMovementChanged(Vector2f direction, byte speedMode) {
        if (speedMode != sentSpeedMode) {
            return true;
        }

        boolean moving = direction.lengthSquared() > 0;
        if (moving != (sentDirection.lengthSquared() > 0)) {
            return true;
        }

        return moving && direction.distanceSquared(sentDirection) > ROTATION_SEND_THRESHOLD * ROTATION_SEND_THRESHOLD;
    }

    /**
     * The facing of the camera in the same convention as the entity position
     * rotation
     */
    private float getFacing() {
        Vector3f dir = app.getCamera().getDirection();
        return FastMath.atan2(dir.x, dir.z);
    }

    private void resetMovement() {
        moveForward = false;
        moveBackward = false;
        moveLeft = false;
        moveRight = false;
        run = false;
        creep = false;
        sentDirection.set(0, 0);
        sentSpeedMode = PossessedMovement.SPEED_WALK;
        timeSinceSend = 0;
    }

    public void setTarget(EntityId target) {
        this.target = target;
        creature = null;
        if (target != null) {
            GameClientState gameClientState = stateManager.getState(GameClientState.class);
            CreatureComponent creatureComponent = gameClientState.getGameClientService().getEntityData().getComponent(target, CreatureComponent.class);
            if (creatureComponent != null) {
                creature = gameClientState.getLevelData().getCreature(creatureComponent.creatureId);
            } else {
                logger.log(Logger.Level.WARNING, "Possession target {0} is not a creature!", target);
            }
        }
    }
}
