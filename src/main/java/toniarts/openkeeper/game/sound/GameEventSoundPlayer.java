/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import com.simsilica.es.EntityId;
import com.simsilica.event.EventBus;
import com.simsilica.event.EventListener;
import com.simsilica.event.EventType;
import toniarts.openkeeper.game.event.ClientEvents;
import toniarts.openkeeper.game.event.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.utils.Point;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Plays the sounds of the {@link GameEvent}s published on the {@link EventBus}.
 */
public final class GameEventSoundPlayer implements EventListener<GameEvent> {

    private static final Logger logger = System.getLogger(GameEventSoundPlayer.class.getName());

    private static final int DISCO_FADE_TILES = 45;

    private final IKwdFile kwdFile;
    private final Map<ExclusiveKey, Integer> exclusiveSounds = new HashMap<>();

    /**
     * The disco music of each Casino that is in disco mode, by the Casino's centre
     */
    private final Map<Point, Integer> discoMusic = new ConcurrentHashMap<>();
    private final Predicate<Point> explored;

    /**
     * @param explored tells if the player has explored the tile, the disco is not heard from the fog
     */
    public GameEventSoundPlayer(IKwdFile kwdFile, Predicate<Point> explored) {
        this.kwdFile = kwdFile;
        this.explored = explored;
    }

    /**
     * Sets the volume of the disco music the closer the camera is to the Casino, as in DK II: it fades out over
     * {@link #DISCO_FADE_TILES} tiles of the larger of the x and y distances. Call this every frame.
     *
     * @return the loudest disco volume 0-100, the level music is turned down by as much
     */
    public int updateDiscoMusic() {
        SoundEngine engine = SoundManager.getEngine();
        int loudest = 0;
        if (engine == null) {
            return loudest;
        }
        for (var disco : discoMusic.entrySet()) {
            SoundVoice voice = engine.getVoice(disco.getValue());
            if (voice == null) {
                discoMusic.remove(disco.getKey());
                continue;
            }
            Point centre = disco.getKey();
            Point listener = engine.getListener();
            int volume = 0;
            if (listener != null && explored.test(centre)) {
                int distance = Math.min(DISCO_FADE_TILES, Math.max(Math.abs(listener.x - centre.x), Math.abs(listener.y - centre.y)));
                volume = 100 - 100 * distance / DISCO_FADE_TILES;
            }
            voice.setVolume(volume);
            loudest = Math.max(loudest, volume);
        }
        return loudest;
    }

    public void register() {
        EventBus.addListener(ClientEvents.GAME_EVENT, this);
    }

    public void unregister() {
        EventBus.removeListener(ClientEvents.GAME_EVENT, this);
        exclusiveSounds.clear();
        discoMusic.values().forEach(SoundManager::stopSound);
        discoMusic.clear();
    }

    @Override
    public void newEvent(EventType<GameEvent> type, GameEvent event) {
        exclusiveSounds.values().removeIf(handle -> !SoundManager.isPlaying(handle));

        if (event.type == GameEventType.CASINO_DISCO_END) {
            Integer music = discoMusic.remove(event.position);
            if (music != null) {
                SoundManager.stopSound(music);
            }
        }

        ExclusiveKey key = null;
        if (GameEventSounds.isExclusive(event.type) && event.entity != null) {
            key = new ExclusiveKey(event.entity, event.type);
            if (exclusiveSounds.containsKey(key)) {
                return; // Still playing
            }
        }

        List<GameEventSounds.Sound> sounds = GameEventSounds.getSounds(event, kwdFile);
        if (sounds.isEmpty()) {
            logger.log(Level.DEBUG, "Event {0} makes no sound (subject {1} {2})",
                    event.type, event.subjectKind, event.subjectId);
        }
        for (GameEventSounds.Sound sound : sounds) {
            int handle = sound.position() != null
                    ? SoundManager.playSound(sound.category(), sound.eventId(), sound.position())
                    : SoundManager.playSound(sound.category(), sound.eventId());
            if (handle == SoundEngine.NO_HANDLE) {
                logger.log(Level.DEBUG, "Sound {0} 0x{1} of event {2} was not accepted by the sound engine",
                        sound.category(), Integer.toHexString(sound.eventId()), event.type);
            }
            if (key != null && handle != SoundEngine.NO_HANDLE) {
                exclusiveSounds.put(key, handle);
            }
            if (event.type == GameEventType.CASINO_DISCO_START && handle != SoundEngine.NO_HANDLE) {
                discoMusic.put(event.position, handle);
            }
        }
    }

    private record ExclusiveKey(EntityId entity, GameEventType type) {

    }
}
