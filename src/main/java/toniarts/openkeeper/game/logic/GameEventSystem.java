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
import toniarts.openkeeper.game.component.GameEvent;
import toniarts.openkeeper.utils.GameTimeCounter;

/**
 * Removes the {@link GameEvent}s once the clients have had the time to pick them up.
 */
public final class GameEventSystem extends GameTimeCounter {

    /**
     * Seconds, an event created and removed between two network updates would never reach a client
     */
    private static final double EVENT_LIFETIME = 2.0;

    private final EntityData entityData;
    private final EntitySet eventEntities;
    private final Map<EntityId, Double> created = new HashMap<>();

    public GameEventSystem(EntityData entityData) {
        this.entityData = entityData;
        eventEntities = entityData.getEntities(GameEvent.class);
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);

        if (eventEntities.applyChanges()) {
            for (Entity entity : eventEntities.getAddedEntities()) {
                created.put(entity.getId(), timeElapsed);
            }
            for (Entity entity : eventEntities.getRemovedEntities()) {
                created.remove(entity.getId());
            }
        }

        created.entrySet().removeIf(entry -> {
            if (timeElapsed - entry.getValue() < EVENT_LIFETIME) {
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
        eventEntities.release();
        created.clear();
    }
}
