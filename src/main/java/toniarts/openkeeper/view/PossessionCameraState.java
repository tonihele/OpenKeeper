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

import com.jme3.anim.AnimComposer;
import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.simsilica.es.EntityId;
import com.simsilica.es.WatchedEntity;
import java.lang.System.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.GameClientState;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.control.CreatureViewControl;
import toniarts.openkeeper.view.control.IEntityViewControl;

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
    /**
     * How far the possessed creature can see, in tiles
     */
    private static final float VIEW_DISTANCE_TILES = 15f;
    /**
     * Tunable, the original's unit is unknown: how many degrees of turn roll a
     * unit of mouse analog input adds, the roll is clamped by the walk cycle
     */
    private static final float TURN_ROLL_DEGREES_PER_ANALOG = 60f;
    /**
     * The swipe meshes look down their negative z axis with x to the right,
     * the camera down its positive z axis with x to the left
     */
    private static final Quaternion SWIPE_ROTATION = new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y);
    /**
     * The field of view, in degrees, normally and in sniper mode. The zoom
     * isn't in the data, the sniper one is tunable.
     */
    private static final float FIELD_OF_VIEW = 45f;
    private static final float SNIPER_FIELD_OF_VIEW = 20f;

    private Main app;
    private AppStateManager stateManager;
    private InputManager inputManager;

    private EntityId target;
    private Creature creature;
    private Spatial targetSpatial;

    private PossessionCamera camera;
    private PossessionWalkCycle walkCycle;
    private boolean inputRegistered = false;
    private WatchedEntity watchedTarget;
    private Spatial swipe;
    private float swipeTimeLeft;
    private boolean secondSwipe;
    private float fieldOfView = FIELD_OF_VIEW;

    private boolean moveForward;
    private boolean moveBackward;
    private boolean moveLeft;
    private boolean moveRight;
    private boolean run;
    private boolean creep;

    private final Vector2f sentDirection = new Vector2f();
    private float sentRotation;
    private float sentPitch;
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

        PlayerEntityViewState entityViewState = stateManager.getState(GameClientState.class).getEntityViewState();
        if (enabled && creature != null) {
            targetSpatial = entityViewState != null ? entityViewState.getEntitySpatial(target) : null;
            camera = new PossessionCamera(app.getCamera(), creature.getAttributes().getSpeed(), creature.getFirstPersonOscillateScale());
            walkCycle = new PossessionWalkCycle(creature.getFirstPersonWalkCycleScale(),
                    creature.getFirstPersonWaddleScale(), creature.getFirstPersonOscillateScale(),
                    creature.getFlags().contains(Creature.CreatureFlag.CAN_FLY),
                    creature.getFlags().contains(Creature.CreatureFlag.CAMERA_ROLLS_WHEN_TURNING));
            loadCameraStartLocation();
            if (entityViewState != null) {
                entityViewState.setHiddenEntity(target);
            }
            watchedTarget = stateManager.getState(GameClientState.class).getGameClientService().getEntityData()
                    .watchEntity(target, CreatureViewState.class);

            resetMovement();
            registerInput();
        } else {
            if (camera != null) {
                camera.setRoll(0);
            }
            unregisterInput();
            resetMovement();
            if (entityViewState != null) {
                entityViewState.setHiddenEntity(null);
            }
            if (watchedTarget != null) {
                watchedTarget.release();
                watchedTarget = null;
            }
            removeSwipe();
            targetSpatial = null;
        }
    }

    /**
     * Load the initial camera position, looking the way the creature faces
     */
    private void loadCameraStartLocation() {
        Camera cam = app.getCamera();
        fieldOfView = FIELD_OF_VIEW;
        cam.setFrustumPerspective(fieldOfView, (float) cam.getWidth() / cam.getHeight(), 0.01f, VIEW_DISTANCE_TILES * WorldUtils.TILE_WIDTH);
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
        sentPitch = getPitch();
    }

    private void updateCameraLocation() {
        boolean movingBackward = axisValue(moveForward, moveBackward) < 0;
        float height = creature.getAttributes().getEyeHeight() + walkCycle.getBobHeight(movingBackward);
        app.getCamera().setLocation(targetSpatial.getWorldTranslation().add(0, height, 0));
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
            case CAMERA_VIEW_LEFT -> {
                camera.rotate(value, true);
                walkCycle.addTurnRoll(value * TURN_ROLL_DEGREES_PER_ANALOG);
            }
            case CAMERA_VIEW_RIGHT -> {
                camera.rotate(-value, true);
                walkCycle.addTurnRoll(-value * TURN_ROLL_DEGREES_PER_ANALOG);
            }
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
            boolean moving = axisValue(moveForward, moveBackward) != 0 || axisValue(moveLeft, moveRight) != 0;
            walkCycle.update(tpf, moving);
            updateCameraLocation();
            camera.setRoll(walkCycle.getRollDegrees());
            sendMovement(tpf);
            updateZoom();
        }
        updateSwipe(tpf);

        // Update audio listener position
        app.getListener().setLocation(app.getCamera().getLocation());
        app.getListener().setRotation(app.getCamera().getRotation());
    }

    /**
     * Shows the swipe of the melee attack in front of the camera when the
     * possessed creature swings. DKII.EXE (0x4100F0) takes turns between the
     * two melee swipes of the creature, if it has the second one.
     */
    private void updateSwipe(float tpf) {
        if (watchedTarget != null && watchedTarget.applyChanges()) {
            CreatureViewState viewState = watchedTarget.get(CreatureViewState.class);
            if (viewState != null && viewState.state == Creature.AnimationType.MELEE_ATTACK) {
                boolean second = secondSwipe && creature.getMelee2Swipe() != Creature.Swipe.NONE;
                secondSwipe = !second && creature.getMelee2Swipe() != Creature.Swipe.NONE;
                showSwipe(second ? creature.getMelee2Swipe() : creature.getMelee1Swipe());
            }
        }

        if (swipe != null) {
            swipeTimeLeft -= tpf;
            if (swipeTimeLeft <= 0) {
                removeSwipe();
            } else {
                Camera cam = app.getCamera();
                swipe.setLocalTranslation(cam.getLocation());
                swipe.setLocalRotation(cam.getRotation().mult(SWIPE_ROTATION));
            }
        }
    }

    private void showSwipe(Creature.Swipe type) {
        removeSwipe();
        if (type == null || type.getModelName() == null) {
            return;
        }

        swipe = AssetUtils.loadModel(app.getAssetManager(), type.getModelName(), null);
        swipeTimeLeft = 0;
        swipe.depthFirstTraversal(spatial -> {
            AnimComposer animComposer = spatial.getControl(AnimComposer.class);
            if (animComposer != null) {
                animComposer.setCurrentAction(KmfModelLoader.DUMMY_ANIM_CLIP_NAME);
                animComposer.setEnabled(true);
                swipeTimeLeft = Math.max(swipeTimeLeft, (float) animComposer.getAnimClip(KmfModelLoader.DUMMY_ANIM_CLIP_NAME).getLength());
            }
        });
        app.getRootNode().attachChild(swipe);
    }

    private void removeSwipe() {
        if (swipe != null) {
            swipe.removeFromParent();
            swipe = null;
        }
    }

    /**
     * Zooms the view in while the creature is in sniper mode, the server tells
     * whether it is
     */
    private void updateZoom() {
        PossessedAbility possessedAbility = stateManager.getState(GameClientState.class).getGameClientService().getEntityData()
                .getComponent(target, PossessedAbility.class);
        float wanted = possessedAbility != null && possessedAbility.ability == Creature.SpecialAbility.SNIPER_MODE
                ? SNIPER_FIELD_OF_VIEW : FIELD_OF_VIEW;
        if (wanted != fieldOfView) {
            fieldOfView = wanted;
            Camera cam = app.getCamera();
            cam.setFrustumPerspective(fieldOfView, (float) cam.getWidth() / cam.getHeight(), cam.getFrustumNear(), cam.getFrustumFar());
        }
    }

    /**
     * Tell the server where we want to go, only when something changes
     */
    private void sendMovement(float tpf) {
        timeSinceSend += tpf;

        Vector2f direction = computeMovementDirection();
        float rotation = getFacing();
        float pitch = getPitch();
        byte speedMode = getSpeedMode();

        boolean changed = hasMovementChanged(direction, speedMode);
        boolean rotated = Math.abs(rotation - sentRotation) > ROTATION_SEND_THRESHOLD
                || Math.abs(pitch - sentPitch) > ROTATION_SEND_THRESHOLD;
        if (changed || (rotated && timeSinceSend >= ROTATION_SEND_INTERVAL)) {
            stateManager.getState(GameClientState.class).getGameClientService().setPossessedMovement(direction, rotation, pitch, speedMode);
            sentDirection.set(direction);
            sentRotation = rotation;
            sentPitch = pitch;
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

    /**
     * The pitch of the camera, positive looking up, in radians
     */
    private float getPitch() {
        return FastMath.asin(FastMath.clamp(app.getCamera().getDirection().y, -1, 1));
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
        if (walkCycle != null) {
            walkCycle.reset();
        }
    }

    /**
     * Get the data of the possessed creature
     *
     * @return the possessed creature, {@code null} if there is none
     */
    public Creature getCreature() {
        return creature;
    }

    public void setTarget(EntityId target) {
        this.target = target;
        creature = null;
        if (target != null) {
            PlayerEntityViewState entityViewState = stateManager.getState(GameClientState.class).getEntityViewState();
            IEntityViewControl control = entityViewState != null ? entityViewState.getEntityViewControl(target) : null;
            if (control instanceof CreatureViewControl creatureViewControl) {
                creature = creatureViewControl.getDataObject();
            } else {
                logger.log(Logger.Level.WARNING, "Possession target {0} is not a creature! entityViewState={1}, control={2}", target, entityViewState, control);
            }
        }
    }
}
