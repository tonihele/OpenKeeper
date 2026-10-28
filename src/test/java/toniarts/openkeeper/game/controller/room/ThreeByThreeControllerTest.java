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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.CreatureGenerator;
import toniarts.openkeeper.game.controller.GameTimeController;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreeByThreeControllerTest {

    private final EntityData entityData = new DefaultEntityData();
    private final GameTimeController timer = new GameTimeController();

    @Test
    void initialPortalStartsItsCooldownAtLevelStart() {
        assertEquals(0, portal().getLastSpawnTime());
    }

    @Test
    void latePortalStartsItsCooldownWhenCreated() {
        timer.processTick(109.5f);
        ThreeByThreeController portal = portal();

        assertEquals(109.5, portal.getLastSpawnTime());
        timer.processTick(1);
        assertEquals(109.5, portal.getLastSpawnTime());
    }

    @Test
    void captureRestartsCooldownAtCaptureTime() {
        ThreeByThreeController portal = portal();
        portal.onSpawn(30, entityData.createEntity());
        timer.processTick(109.5f);

        portal.captured((short) 4);

        assertEquals(109.5, portal.getLastSpawnTime());
        assertEquals(4, portal.getOwnerId());
    }

    @Test
    void portalsKeepIndependentSpawnTimers() {
        ThreeByThreeController first = portal();
        timer.processTick(100);
        ThreeByThreeController second = portal();
        first.onSpawn(105, entityData.createEntity());

        assertEquals(105, first.getLastSpawnTime());
        assertEquals(100, second.getLastSpawnTime());
    }

    private ThreeByThreeController portal() {
        RoomInstance room = new RoomInstance(new Room());
        room.setOwnerId((short) 3);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                room.addCoordinate(new Point(x, y));
            }
        }
        EntityId id = entityData.createEntity();
        ThreeByThreeController controller = new ThreeByThreeController(
                id, entityData, null, room, null, timer);
        assertEquals(new Point(1, 1), entityData.getComponent(id, CreatureGenerator.class).entrance);
        return controller;
    }
}
