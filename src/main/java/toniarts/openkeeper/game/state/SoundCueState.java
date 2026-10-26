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
import toniarts.openkeeper.game.component.SoundCue;
import toniarts.openkeeper.game.sound.SoundEngine;
import toniarts.openkeeper.game.sound.SoundManager;

/**
 * Plays the {@link SoundCue}s the game logic creates. The sound is a client matter, the logic only tells what
 * happened.
 */
public final class SoundCueState extends AbstractAppState {

    private final EntityData entityData;
    private final short playerId;
    private final Map<ExclusiveKey, Integer> exclusiveSounds = new HashMap<>();

    private EntitySet cueEntities;

    /**
     * @param entityData the entity data of the game
     * @param playerId the player we play for, the sounds meant for the other players are left out
     */
    public SoundCueState(EntityData entityData, short playerId) {
        this.entityData = entityData;
        this.playerId = playerId;
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);

        // The cues that are already there are old news, only the ones created from now on are played
        cueEntities = entityData.getEntities(SoundCue.class);
    }

    @Override
    public void update(float tpf) {
        if (!cueEntities.applyChanges()) {
            return;
        }
        for (Entity entity : cueEntities.getAddedEntities()) {
            play(entity.get(SoundCue.class));
        }
        exclusiveSounds.values().removeIf(handle -> !SoundManager.isPlaying(handle));
    }

    private void play(SoundCue cue) {
        if (cue.playerId != SoundCue.ALL_PLAYERS && cue.playerId != playerId) {
            return;
        }

        ExclusiveKey key = null;
        if (cue.exclusive && cue.source != null) {
            key = new ExclusiveKey(cue.source, cue.eventId);
            if (exclusiveSounds.containsKey(key)) {
                return; // Still playing, the stale ones are cleared on each update
            }
        }

        int handle = cue.position != null
                ? SoundManager.playSound(cue.category, cue.eventId, cue.position)
                : SoundManager.playSound(cue.category, cue.eventId);
        if (key != null && handle != SoundEngine.NO_HANDLE) {
            exclusiveSounds.put(key, handle);
        }
    }

    @Override
    public void cleanup() {
        if (cueEntities != null) {
            cueEntities.release();
            cueEntities = null;
        }
        exclusiveSounds.clear();
        super.cleanup();
    }

    private record ExclusiveKey(EntityId source, int eventId) {

    }
}
