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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.storage.IRoomObjectControl;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractRoomControllerTest {

    @Test
    void reconstructingRoomKeepsStoredObjectsTrackedForRemoval() {
        EntityData entityData = new DefaultEntityData();
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinate(new Point(1, 1));
        TestRoomController room = new TestRoomController(entityData.createEntity(), entityData, roomInstance);
        TrackingObjectControl originalControl = new TrackingObjectControl(ObjectType.LAIR);
        TrackingObjectControl replacementControl = new TrackingObjectControl(ObjectType.LAIR);

        room.register(originalControl);
        IRoomObjectControl<?> registeredControl = room.register(replacementControl);

        assertSame(originalControl, registeredControl);
        assertTrue(replacementControl.destroyed);
        assertFalse(originalControl.destroyed);
        assertTrue(originalControl.capacityUpdated);

        room.remove();

        assertTrue(originalControl.destroyed);
    }

    @Test
    void creaturesCanWalkOnTheCornersOfTheHeartsCentreButNothingIsStoredThere() {
        EntityData entityData = new DefaultEntityData();
        RoomInstance roomInstance = new RoomInstance(new Room());
        for (int y = 10; y < 15; y++) {
            for (int x = 20; x < 25; x++) {
                roomInstance.addCoordinate(new Point(x, y));
            }
        }
        FiveByFiveRotatedController heart = new FiveByFiveRotatedController(entityData.createEntity(), entityData, null,
                roomInstance, null, Map.of(Variable.MiscVariable.MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE,
                        CreatureSleepFixtures.variable(Variable.MiscVariable.MiscType.MAX_GOLD_PER_DUNGEON_HEART_TILE, 1000)), null);

        assertTrue(heart.isTileAccessible(new Point(20, 11), new Point(21, 11)));
        assertTrue(heart.isTileAccessible(new Point(23, 14), new Point(23, 13)));
        assertFalse(heart.isTileAccessible(new Point(22, 10), new Point(22, 11)));
        assertFalse(heart.isTileAccessible(new Point(21, 11), new Point(22, 12)));
        assertFalse(heart.isTileAccessible(null, new Point(21, 11)));
        assertTrue(heart.isTileAccessible(null, new Point(20, 12)));
    }

    private static final class TestRoomController extends AbstractRoomController {

        private TestRoomController(EntityId entityId, EntityData entityData, RoomInstance roomInstance) {
            super(entityId, entityData, null, roomInstance, null, ObjectType.LAIR);
        }

        private <T extends IRoomObjectControl> T register(T control) {
            return addObjectControl(control);
        }
    }

    private static final class TrackingObjectControl implements IRoomObjectControl<Object> {

        private final ObjectType objectType;
        private boolean destroyed;
        private boolean capacityUpdated;

        private TrackingObjectControl(ObjectType objectType) {
            this.objectType = objectType;
        }

        @Override
        public Object addItem(Object value, Point p) {
            return value;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public int getCurrentCapacity() {
            return 0;
        }

        @Override
        public Collection<EntityId> getItems(Point p) {
            return Collections.emptyList();
        }

        @Override
        public int getMaxCapacity() {
            return 0;
        }

        @Override
        public void updateMaxCapacity() {
            capacityUpdated = true;
        }

        @Override
        public ObjectType getObjectType() {
            return objectType;
        }

        @Override
        public boolean isFullCapacity() {
            return false;
        }

        @Override
        public void removeItem(EntityId object) {
        }

        @Override
        public Collection<Point> getAvailableCoordinates() {
            return Collections.emptyList();
        }

        @Override
        public void captured(short playerId) {
        }
    }
}
