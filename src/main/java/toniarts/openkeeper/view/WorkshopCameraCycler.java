/*
 * Copyright (C) 2026 OpenKeeper
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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.ToIntFunction;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.TrapViewState;

/** Camera navigation among owned Workshop instances, including their blueprints. */
public final class WorkshopCameraCycler implements AutoCloseable {
    private final EntitySet doors;
    private final EntitySet traps;
    private final Map<Short, EntityId> lastDoors = new HashMap<>();
    private final Map<Short, EntityId> lastTraps = new HashMap<>();

    public WorkshopCameraCycler(EntityData entityData, short playerId) {
        var owner = new FieldFilter(Owner.class, "ownerId", playerId);
        doors = entityData.getEntities(owner, DoorViewState.class, Owner.class, Position.class);
        traps = entityData.getEntities(owner, TrapViewState.class, Owner.class, Position.class);
    }

    public Vector3f nextDoor(short doorId) {
        return next(doors, lastDoors, doorId, entity -> entity.get(DoorViewState.class).doorId);
    }

    public Vector3f nextTrap(short trapId) {
        return next(traps, lastTraps, trapId, entity -> entity.get(TrapViewState.class).trapId);
    }

    private static Vector3f next(EntitySet entities, Map<Short, EntityId> previous, short typeId,
            ToIntFunction<Entity> type) {
        // Refresh on every click, including while the game is paused.
        entities.applyChanges();
        EntityId last = previous.get(typeId);
        Entity first = null;
        Entity following = null;
        for (Entity entity : entities) {
            if (type.applyAsInt(entity) != typeId) {
                continue;
            }
            long id = entity.getId().getId();
            if (first == null || id < first.getId().getId()) {
                first = entity;
            }
            if (last != null && id > last.getId()
                    && (following == null || id < following.getId().getId())) {
                following = entity;
            }
        }
        Entity selected = following != null ? following : first;
        if (selected == null) {
            previous.remove(typeId);
            return null;
        }
        previous.put(typeId, selected.getId());
        // Camera movement must not modify the replicated component's vector.
        return selected.get(Position.class).position.clone();
    }

    @Override
    public void close() {
        doors.release();
        traps.release();
        lastDoors.clear();
        lastTraps.clear();
    }
}
