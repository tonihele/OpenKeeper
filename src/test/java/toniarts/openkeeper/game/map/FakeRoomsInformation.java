/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.map;

import com.simsilica.es.EntityId;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link IRoomsInformation} test double.
 */
final class FakeRoomsInformation implements IRoomsInformation<FakeRoomInformation> {

    final Map<EntityId, FakeRoomInformation> rooms = new HashMap<>();

    @Override
    public FakeRoomInformation getRoomInformation(EntityId entityId) {
        return rooms.get(entityId);
    }

    @Override
    public int getRoomCount(short ownerId, short roomId) {
        return 0;
    }

}
