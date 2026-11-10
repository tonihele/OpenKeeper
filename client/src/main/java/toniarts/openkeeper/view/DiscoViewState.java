/*
 * Copyright (C) 2014-2026 OpenKeeper
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
import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import com.simsilica.event.EventType;
import toniarts.openkeeper.game.event.ClientEvents;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.sound.GameEventSounds;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.game.sound.SoundVoice;
import toniarts.openkeeper.game.state.SoundState;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Plays the disco music of the Casinos that are in disco mode. It is enabled when a disco starts and disabled when
 * the last one is over, so it only updates the music while there is a disco.
 */
public final class DiscoViewState extends AbstractAppState {

    private static final int DISCO_FADE_TILES = 45;

    private final IKwdFile kwdFile;
    private final Predicate<Point> explored;
    private final EventListener<GameEvent> discoListener = this::onDiscoEvent;

    /**
     * The disco music of each Casino that is in disco mode, by the Casino's centre. Render thread only
     */
    private final Map<Point, Integer> discoMusic = new HashMap<>();
    private Application app;
    private AppStateManager stateManager;

    /**
     * @param explored tells if the player has explored the tile, the disco is not heard from the fog
     */
    public DiscoViewState(IKwdFile kwdFile, Predicate<Point> explored) {
        this.kwdFile = kwdFile;
        this.explored = explored;

        // Nothing to update until a disco starts
        setEnabled(false);
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = app;
        this.stateManager = stateManager;
        EventBus.addListener(ClientEvents.GAME_EVENT, discoListener);
    }

    @Override
    public void cleanup() {
        EventBus.removeListener(ClientEvents.GAME_EVENT, discoListener);
        discoMusic.values().forEach(SoundManager::stopSound);
        discoMusic.clear();
        setMusicVolumeFactor(1f);

        super.cleanup();
    }

    /**
     * Sets the volume of the disco music the closer the camera is to the Casino, as in DK II: it fades out over
     * {@link #DISCO_FADE_TILES} tiles of the larger of the x and y distances. The level music is turned down as much
     * as the loudest disco is heard.
     */
    @Override
    public void update(float tpf) {
        int loudest = 0;
        SoundEngine engine = SoundManager.getEngine();
        if (engine != null) {
            Point listener = engine.getListener();
            for (Iterator<Map.Entry<Point, Integer>> iter = discoMusic.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<Point, Integer> disco = iter.next();
                SoundVoice voice = engine.getVoice(disco.getValue());
                if (voice == null) {
                    iter.remove();
                    continue;
                }
                Point centre = disco.getKey();
                int volume = 0;
                if (listener != null && explored.test(centre)) {
                    int distance = Math.min(DISCO_FADE_TILES, Math.max(Math.abs(listener.x - centre.x), Math.abs(listener.y - centre.y)));
                    volume = 100 - 100 * distance / DISCO_FADE_TILES;
                }
                voice.setVolume(volume);
                loudest = Math.max(loudest, volume);
            }
        }
        setMusicVolumeFactor(1f - loudest / 100f);
        setEnabled(!discoMusic.isEmpty());
    }

    private void onDiscoEvent(EventType<GameEvent> type, GameEvent event) {
        if (event.type == GameEventType.CASINO_DISCO_START) {
            app.enqueue(() -> startDisco(event));
        } else if (event.type == GameEventType.CASINO_DISCO_END) {
            app.enqueue(() -> stopDisco(event.position));
        }
    }

    private void startDisco(GameEvent event) {
        if (!isInitialized()) {
            return;
        }
        for (GameEventSounds.Sound sound : GameEventSounds.getSounds(event, kwdFile)) {
            int handle = SoundManager.playSound(sound.category(), sound.eventId());
            if (handle != SoundEngine.NO_HANDLE) {
                discoMusic.put(event.position, handle);
                setEnabled(true);
            }
        }
    }

    private void stopDisco(Point centre) {
        Integer music = discoMusic.remove(centre);
        if (music != null) {
            SoundManager.stopSound(music);
        }
    }

    private void setMusicVolumeFactor(float factor) {
        SoundState soundState = stateManager.getState(SoundState.class);
        if (soundState != null) {
            soundState.setMusicVolumeFactor(factor);
        }
    }
}
