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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.audio.AudioNode;
import toniarts.openkeeper.Main;

/**
 *
 * @author ArchDemon
 */


public class SoundState extends AbstractPauseAwareState {
    private Main app;
    private AppStateManager stateManager;

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);

        this.app = (Main) app;
        this.stateManager = stateManager;
    }

    @Override
    public boolean isPauseable() {
        return false;
    }

    public void attachSpeech(int speechId) {
        String file = String.format("Sounds/speech_%s/lvlspe%02d.mp2",
                stateManager.getState(GameState.class).getLevel().toLowerCase(), speechId);
        AudioNode speech = new AudioNode(app.getAssetManager(), file, false);
        speech.setName("speech");
        speech.setLooping(false);
        speech.setPositional(false);
        speech.play();
        app.getRootNode().attachChild(speech);
    }
}
