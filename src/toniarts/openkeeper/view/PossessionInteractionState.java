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
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector2f;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.view.PossessionCameraControl.Direction;
import toniarts.openkeeper.world.creature.CreatureControl;

/**
 * State for managing player interactions in the world while possessing a
 * creature
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author ArchDemon
 */
// TODO: States, now only selection
public abstract class PossessionInteractionState extends AbstractPauseAwareState {

    public enum Action {

        MELEE, SPELL_1, SPELL_2, SPELL_3, ABILITY_1, ABILITY_2, GROUP
    }

    private static final Logger logger = Logger.getLogger(PossessionInteractionState.class.getName());
    
    private Main app;

    private AppStateManager stateManager;
    private InputManager inputManager;

    private CreatureControl target;
    private RawInputListener inputListener;
    public Vector2f mousePosition = Vector2f.ZERO;
    private Action action;

    public PossessionInteractionState(boolean enabled) {
        super.setEnabled(enabled);
    }

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();

        initializeInput();

        // Add listener
        if (isEnabled()) {
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        inputManager.setCursorVisible(!enabled);

        if (enabled) {
            changeAction(Action.MELEE);
            app.getInputManager().addRawInputListener(inputListener);

            PossessionCameraControl pcc = new PossessionCameraControl(app.getCamera(), Direction.ENTRANCE) {
                @Override
                public void onExit() {
                    stateManager.getState(PossessionCameraState.class).setEnabled(true);
                }
            };
            target.getSpatial().addControl(pcc);
        } else {
            stateManager.getState(PossessionCameraState.class).setEnabled(false);
            app.getInputManager().removeRawInputListener(inputListener);

            PlayerCamera pc = stateManager.getState(PlayerCameraState.class).getCamera();
            pc.initialize();
            pc.setLookAt(target.getSpatial().getLocalTranslation());
            PossessionCameraControl pcc = new PossessionCameraControl(app.getCamera(), Direction.EXIT) {
                @Override
                public void onExit() {
                    PossessionInteractionState.this.onExit();
                }
            };
            target.getSpatial().addControl(pcc);
            target = null;
        }
    }

    @Override
    public void cleanup() {
        app.getInputManager().removeRawInputListener(inputListener);

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return false;
    }

    /**
     * Get the current interaction state item id
     *
     * @return current interaction state item id
     */
    public Action getAction() {
        return action;
    }

    private void changeAction(Action action) {
        if (this.action != action) {
            this.action = action;
            onActionChange(action);
        }
    }

    public CreatureControl getTarget() {
        return target;
    }

    public void setTarget(CreatureControl target) {
        this.target = target;
        stateManager.getState(PossessionCameraState.class).setTarget(target);
    }

    private void initializeInput() {
        inputListener = new RawInputListener() {
            @Override
            public void beginInput() {
            }

            @Override
            public void endInput() {
            }

            @Override
            public void onJoyAxisEvent(JoyAxisEvent evt) {
            }

            @Override
            public void onJoyButtonEvent(JoyButtonEvent evt) {
            }

            @Override
            public void onMouseMotionEvent(MouseMotionEvent evt) {

                mousePosition.set(evt.getX(), evt.getY());
            }

            @Override
            public void onMouseButtonEvent(MouseButtonEvent evt) {
                if (evt.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                    // attack
                } else if (evt.getButtonIndex() == MouseInput.BUTTON_RIGHT && evt.isReleased()) {
                    setEnabled(false);
                }
            }

            @Override
            public void onKeyEvent(KeyInputEvent evt) {
                if (evt.getKeyCode() == (int) Settings.Setting.POSSESSED_SELECT_MELEE.getDefaultValue() && evt.isReleased()) {
                    changeAction(Action.MELEE);
                } else if (evt.getKeyCode() == (int) Settings.Setting.POSSESSED_SELECT_SPELL_1.getDefaultValue() && evt.isReleased()) {
                    changeAction(Action.SPELL_1);
                } else if (evt.getKeyCode() == (int) Settings.Setting.POSSESSED_SELECT_SPELL_2.getDefaultValue() && evt.isReleased()) {
                    changeAction(Action.SPELL_2);
                } else if (evt.getKeyCode() == (int) Settings.Setting.POSSESSED_SELECT_SPELL_3.getDefaultValue() && evt.isReleased()) {
                    changeAction(Action.SPELL_3);
                } else if (evt.getKeyCode() == (int) Settings.Setting.POSSESSED_SELECT_ABILITY_1.getDefaultValue() && evt.isReleased()) {
                    changeAction(Action.ABILITY_1);
                } else if (evt.getKeyCode() == (int) Settings.Setting.POSSESSED_SELECT_ABILITY_2.getDefaultValue() && evt.isReleased()) {
                    changeAction(Action.ABILITY_2);
                } else if (evt.getKeyCode() == (int) Settings.Setting.POSSESSED_SELECT_GROUP.getDefaultValue() && evt.isReleased()) {
                    changeAction(Action.GROUP);
                }
            }

            @Override
            public void onTouchEvent(TouchEvent evt) {
            }
        };
    }

    /**
     * A callback for changing the interaction state
     *
     */
    protected abstract void onExit();

    protected abstract void onActionChange(Action action);
}
