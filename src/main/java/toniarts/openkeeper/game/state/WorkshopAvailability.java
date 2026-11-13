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
import com.simsilica.es.EntitySet;
import com.simsilica.es.filter.FieldFilter;
import java.util.function.Consumer;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;

/** Observes whether the keeper owns any live Workshop, regardless of its size. */
final class WorkshopAvailability implements AutoCloseable {

    private static final short WORKSHOP_ROOM_ID = 10;
    private final EntitySet rooms;
    private final short playerId;
    private final Consumer<Boolean> onChange;
    private Boolean available;
    private boolean closed;

    WorkshopAvailability(EntityData entityData, short playerId, Consumer<Boolean> onChange) {
        this.playerId = playerId;
        this.onChange = onChange;
        rooms = entityData.getEntities(new FieldFilter<>(RoomComponent.class, "roomId", WORKSHOP_ROOM_ID),
                RoomComponent.class, Owner.class);
    }

    void update() {
        if (closed) {
            return;
        }
        boolean changed = rooms.applyChanges();
        if (!changed && available != null) {
            return;
        }
        boolean ownsWorkshop = false;
        for (Entity room : rooms) {
            if (!room.get(RoomComponent.class).destroyed && room.get(Owner.class).ownerId == playerId) {
                ownsWorkshop = true;
                break;
            }
        }
        if (available == null || available != ownsWorkshop) {
            available = ownsWorkshop;
            onChange.accept(ownsWorkshop);
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            rooms.release();
        }
    }
}
