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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import java.util.HashMap;
import java.util.Map;
import toniarts.openkeeper.game.component.SoundCue;
import toniarts.openkeeper.utils.GameTimeCounter;

/**
 * Removes the {@link SoundCue}s once the clients have had the time to pick them up.
 */
public final class SoundCueSystem extends GameTimeCounter {

    /**
     * Seconds, a cue created and removed between two network updates would never reach a client
     */
    private static final double CUE_LIFETIME = 2.0;

    private final EntityData entityData;
    private final EntitySet cueEntities;
    private final Map<EntityId, Double> created = new HashMap<>();

    public SoundCueSystem(EntityData entityData) {
        this.entityData = entityData;
        cueEntities = entityData.getEntities(SoundCue.class);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        if (cueEntities.applyChanges()) {
            for (Entity entity : cueEntities.getAddedEntities()) {
                created.put(entity.getId(), timeElapsed);
            }
            for (Entity entity : cueEntities.getRemovedEntities()) {
                created.remove(entity.getId());
            }
        }

        created.entrySet().removeIf(entry -> {
            if (timeElapsed - entry.getValue() < CUE_LIFETIME) {
                return false;
            }
            entityData.removeEntity(entry.getKey());
            return true;
        });
    }

    @Override
    public void start() {
        // Nothing to start
    }

    @Override
    public void stop() {
        cueEntities.release();
        created.clear();
    }
}
