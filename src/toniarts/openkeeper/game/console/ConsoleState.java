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
package toniarts.openkeeper.game.console;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.KeyInput;
import de.lessvoid.nifty.controls.Console;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.state.AbstractPauseAwareState;
import toniarts.openkeeper.view.PlayerInteractionState;

/**
 *
 * @author ArchDemon
 */
public class ConsoleState extends AbstractPauseAwareState {

    private Main app;
    private GameConsole console;
    private ConsoleInputListener inputListener;

    public final static int KEY = KeyInput.KEY_F11;

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        console = new GameConsole(stateManager);
        inputListener = new ConsoleInputListener(this);
        setEnabled(false);
    }

    @Override
    public void cleanup() {
        app.getInputManager().removeRawInputListener(inputListener);

        super.cleanup();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        PlayerInteractionState pis = app.getStateManager().getState(PlayerInteractionState.class);
        if (pis != null && pis.isInitialized()) {
            app.getStateManager().getState(PlayerInteractionState.class).setEnabled(!enabled);
            console.setVisible(enabled);
        }

        if (enabled) {
            app.getInputManager().addRawInputListener(inputListener);
        } else {
            app.getInputManager().removeRawInputListener(inputListener);
        }
    }

    public Console getConsole() {
        return console.getConsole();
    }

    @Override
    public boolean isPauseable() {
        return false;
    }
}
