/*
 * Copyright (C) 2014-2017 OpenKeeper
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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import toniarts.openkeeper.Main;

/**
 * A state that handles ended game actions, such as cinematics and stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EndGameState extends AbstractAppState implements RawInputListener {

    private Main app;
    private AppStateManager stateManager;
    private final PlayerState playerState;

    public EndGameState(PlayerState playerState) {
        this.playerState = playerState;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.stateManager = stateManager;

        // Add the listener
        app.getInputManager().addRawInputListener(this);

        // Add the texts
        playerState.setWideScreen(true);
        playerState.setGeneralText(2901);
    }

    @Override
    public void cleanup() {
        app.getInputManager().removeRawInputListener(this);
        super.cleanup();
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

    }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {

    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (evt.isPressed()) {
            if (evt.getKeyCode() == KeyInput.KEY_SPACE || evt.getKeyCode() == KeyInput.KEY_ESCAPE) {

                // FIXME: Space to debriefing & ESC to continue
                stateManager.detach(this);
                stateManager.getState(PlayerState.class).quitToMainMenu();
            }
        }
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {

    }

}
