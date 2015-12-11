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
import com.jme3.asset.AssetManager;
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
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.game.state.GameState;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.view.selection.SelectionHandler;

/**
 * State for managing player interactions in the world. Heavily drawn from
 * Philip Willuweit's AgentKeeper code <p.willuweit@gmx.de>.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 * @author ArchDemon
 */
// TODO: States, now only selection
public abstract class PossessionInteractionState extends AbstractPauseAwareState implements RawInputListener {
    
    public enum Action {

        MELEE, SPELL_1, SPELL_2, SPELL_3, ABILITY_1, ABILITY_2, GROUP
    }

    private Main app;
    private final GameState gameState;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private final Player player = null;
    private SelectionHandler handler;
    private boolean actionIsPressed = false;
    private boolean cancelIsPressed = false;
    private boolean startSet = false;
    public Vector2f mousePosition = Vector2f.ZERO;
    private Action action;
    private static final Logger logger = Logger.getLogger(PossessionInteractionState.class.getName());
    
    private Thing.KeeperCreature target;

    public PossessionInteractionState(GameState gameState) {
        this.gameState = gameState;
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
        
        inputManager.setCursorVisible(false);
        
        changeAction(Action.MELEE);
        // Add listener
        if (isEnabled()) {
            setEnabled(true);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (enabled) {
            app.getInputManager().addRawInputListener(this);
        } else {
            app.getInputManager().removeRawInputListener(this);            
        }
    }

    @Override
    public void cleanup() {
        app.getInputManager().removeRawInputListener(this);

        super.cleanup();
    }

    @Override
    public boolean isPauseable() {
        return true;
    }

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
            onExit();
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (evt.getKeyCode() == Settings.Setting.POSSESSED_SELECT_MELEE.getDefaultValue() && evt.isReleased()) {
            changeAction(Action.MELEE);           
        } else if (evt.getKeyCode() == Settings.Setting.POSSESSED_SELECT_SPELL_1.getDefaultValue() && evt.isReleased()) {
            changeAction(Action.SPELL_1);
        } else if (evt.getKeyCode() == Settings.Setting.POSSESSED_SELECT_SPELL_2.getDefaultValue() && evt.isReleased()) {
            changeAction(Action.SPELL_2);
        } else if (evt.getKeyCode() == Settings.Setting.POSSESSED_SELECT_SPELL_3.getDefaultValue() && evt.isReleased()) {
            changeAction(Action.SPELL_3);
        } else if (evt.getKeyCode() == Settings.Setting.POSSESSED_SELECT_ABILITY_1.getDefaultValue() && evt.isReleased()) {
            changeAction(Action.ABILITY_1);
        } else if (evt.getKeyCode() == Settings.Setting.POSSESSED_SELECT_ABILITY_2.getDefaultValue() && evt.isReleased()) {
            changeAction(Action.ABILITY_2);
        } else if (evt.getKeyCode() == Settings.Setting.POSSESSED_SELECT_GROUP.getDefaultValue() && evt.isReleased()) {
            changeAction(Action.GROUP);
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
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
        this.action = action;
        onActionChange(action);
    }
    
    public Thing.KeeperCreature getTarget() {
        return target;
    }
    
    public Creature getTargetCreature() {
        return gameState.getLevelData().getCreature(target.getCreatureId());
    }

    public void setTarget(Thing.KeeperCreature target) {
        this.target = target;
    }

    /**
     * A callback for changing the interaction state
     *
     * @param interactionState new state
     * @param id new id
     */
    protected void onExit() {
        inputManager.setCursorVisible(true);
    }
    protected abstract void onActionChange(Action action);
}
