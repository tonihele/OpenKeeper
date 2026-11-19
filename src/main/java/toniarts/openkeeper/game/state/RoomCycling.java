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
package toniarts.openkeeper.game.state;

import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.utils.Point;

/** Client-only camera targets from the keeper's replicated room entities. */
final class RoomCycling implements AutoCloseable {

    private final EntitySet rooms;
    private EntityId lastRoom;
    private boolean closed;

    RoomCycling(EntityData entityData, short playerId) {
        rooms = entityData.getEntities(new FieldFilter<>(Owner.class, "ownerId", playerId),
                RoomComponent.class, Owner.class);
    }

    /** Return the next live room centre, or null when there is no matching room. */
    Point next(short roomId) {
        if (closed) {
            return null;
        }
        rooms.applyChanges();
        List<Entity> matching = new ArrayList<>();
        for (Entity entity : rooms) {
            RoomComponent room = entity.get(RoomComponent.class);
            if (room.roomId == roomId && !room.destroyed && room.location != null) {
                matching.add(entity);
            }
        }
        // EntitySet iteration order can change after replication updates.
        matching.sort(Comparator.comparingLong(entity -> entity.getId().getId()));
        if (matching.isEmpty()) {
            lastRoom = null;
            return null;
        }
        int next = 0;
        for (int i = 0; i < matching.size(); i++) {
            if (matching.get(i).getId().equals(lastRoom)) {
                next = (i + 1) % matching.size();
                break;
            }
        }
        Entity selected = matching.get(next);
        lastRoom = selected.getId();
        return selected.get(RoomComponent.class).location;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            rooms.release();
            lastRoom = null;
        }
    }
}
