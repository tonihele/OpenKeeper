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
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.GameEvent;
import toniarts.openkeeper.game.event.GameEventType;
import toniarts.openkeeper.game.sound.GameEventSounds;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;
import toniarts.openkeeper.tools.convert.map.IKwdFile;

/**
 * Reacts to the {@link GameEvent}s the game logic creates. What happened is the logic's business, what to make
 * of it is a client matter. For now the client plays the sounds.
 */
public final class GameEventState extends AbstractAppState {

    private final EntityData entityData;
    private final IKwdFile kwdFile;
    private final short playerId;
    private final Map<ExclusiveKey, Integer> exclusiveSounds = new HashMap<>();

    private EntitySet eventEntities;

    /**
     * @param entityData the entity data of the game
     * @param kwdFile the map data, the events refer to its thing types
     * @param playerId the player we play for, the events meant for the other players are left out
     */
    public GameEventState(EntityData entityData, IKwdFile kwdFile, short playerId) {
        this.entityData = entityData;
        this.kwdFile = kwdFile;
        this.playerId = playerId;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        // The events that are already there are old news, only the ones created from now on are handled
        eventEntities = entityData.getEntities(GameEvent.class);
    }

    @Override
    public void update(float tpf) {
        if (!eventEntities.applyChanges()) {
            return;
        }
        for (Entity entity : eventEntities.getAddedEntities()) {
            handle(entity.get(GameEvent.class));
        }
        exclusiveSounds.values().removeIf(handle -> !SoundManager.isPlaying(handle));
    }

    private void handle(GameEvent event) {
        if (event.playerId != GameEvent.ALL_PLAYERS && event.playerId != playerId) {
            return;
        }
        playSounds(event);
    }

    private void playSounds(GameEvent event) {
        ExclusiveKey key = null;
        if (GameEventSounds.isExclusive(event.type) && event.entity != null) {
            key = new ExclusiveKey(event.entity, event.type);
            if (exclusiveSounds.containsKey(key)) {
                return; // Still playing, the stale ones are cleared on each update
            }
        }

        for (GameEventSounds.Sound sound : GameEventSounds.getSounds(event, kwdFile)) {
            int handle = sound.position() != null
                    ? SoundManager.playSound(sound.category(), sound.eventId(), sound.position())
                    : SoundManager.playSound(sound.category(), sound.eventId());
            if (key != null && handle != SoundEngine.NO_HANDLE) {
                exclusiveSounds.put(key, handle);
            }
        }
    }

    @Override
    public void cleanup() {
        if (eventEntities != null) {
            eventEntities.release();
            eventEntities = null;
        }
        exclusiveSounds.clear();
        super.cleanup();
    }

    private record ExclusiveKey(EntityId entity, GameEventType type) {

    }
}
