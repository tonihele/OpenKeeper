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
package toniarts.openkeeper.game.controller.room;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ImpGenerator;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FiveByFiveRotatedControllerTest {

    private final EntityData entityData = new DefaultEntityData();

    @Test
    void capturingADestroyedHeartDoesNotNeedASpawnPoint() {
        EntityId id = entityData.createEntity();
        FiveByFiveRotatedController heart = heart(id);

        heart.captured((short) 4);

        assertEquals(4, heart.getOwnerId());
        assertNull(entityData.getComponent(id, ImpGenerator.class));
    }

    @Test
    void capturingALiveHeartRestartsItsSpawnClock() {
        EntityId id = entityData.createEntity();
        FiveByFiveRotatedController heart = heart(id);
        entityData.setComponent(id, new ImpGenerator(new Point(2, 2), 30));

        heart.captured((short) 4);

        assertEquals(Double.MIN_VALUE, heart.getLastSpawnTime());
        assertEquals(new Point(2, 2), heart.getEntranceCoordinate());
    }

    private FiveByFiveRotatedController heart(EntityId id) {
        RoomInstance room = new RoomInstance(new Room());
        room.setOwnerId((short) 3);
        room.setDestroyed(true);
        room.addCoordinate(new Point(0, 0));
        return new FiveByFiveRotatedController(id, entityData, null, room, null,
                Map.of(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, CreatureSleepFixtures.variable(MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, 100)),
                null);
    }
}
