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
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityId;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link IRoomsInformation} test double. Public: also reused by
 * {@code view.minimap}'s rendering tests.
 */
public final class FakeRoomsInformation implements IRoomsInformation<FakeRoomInformation> {

    public final Map<EntityId, FakeRoomInformation> rooms = new HashMap<>();

    @Override
    public FakeRoomInformation getRoomInformation(EntityId entityId) {
        return rooms.get(entityId);
    }

    @Override
    public int getRoomCount(short ownerId, short roomId) {
        return 0;
    }

}
