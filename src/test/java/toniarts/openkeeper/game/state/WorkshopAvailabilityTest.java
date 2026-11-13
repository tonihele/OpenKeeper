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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.ClientEntityData;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkshopAvailabilityTest {
    private static final short PLAYER = 3;
    private static final short ENEMY = 4;

    @Test
    void followsBuildingSellingDestructionAndOwnershipThroughClientSubscriptions() {
        var data = new DefaultEntityData();
        try {
            List<Boolean> updates = new ArrayList<>();
            try (var observer = new WorkshopAvailability(new ClientEntityData(data), PLAYER, updates::add)) {
                observer.update();
                EntityId enemy = room(data, (short) 10, ENEMY);
                room(data, (short) 2, PLAYER);
                observer.update();
                assertEquals(List.of(false), updates);

                EntityId first = room(data, (short) 10, PLAYER);
                observer.update();
                EntityId second = room(data, (short) 10, PLAYER);
                observer.update();
                data.removeEntity(first);
                observer.update();
                assertEquals(List.of(false, true), updates);

                data.setComponent(second, new RoomComponent((short) 10, true, new Point(1, 1)));
                observer.update();
                data.setComponent(enemy, new Owner(PLAYER, PLAYER));
                observer.update();
                data.setComponent(enemy, new Owner(ENEMY, ENEMY));
                observer.update();
                assertEquals(List.of(false, true, false, true, false), updates);
            }
        } finally {
            data.close();
        }
    }

    @Test
    void detectsAnExistingWorkshopAndStopsAfterCleanup() {
        var data = new DefaultEntityData();
        try {
            EntityId workshop = room(data, (short) 10, PLAYER);
            List<Boolean> updates = new ArrayList<>();
            var observer = new WorkshopAvailability(new ClientEntityData(data), PLAYER, updates::add);
            observer.update();
            observer.close();
            observer.close();
            data.removeEntity(workshop);
            observer.update();
            assertEquals(List.of(true), updates);
            try (var reopened = new WorkshopAvailability(new ClientEntityData(data), PLAYER, updates::add)) {
                reopened.update();
                assertEquals(List.of(true, false), updates);
            }
        } finally {
            data.close();
        }
    }

    private EntityId room(DefaultEntityData data, short roomId, short owner) {
        EntityId id = data.createEntity();
        data.setComponents(id, new RoomComponent(roomId, false, new Point(1, 1)), new Owner(owner, owner));
        return id;
    }
}
