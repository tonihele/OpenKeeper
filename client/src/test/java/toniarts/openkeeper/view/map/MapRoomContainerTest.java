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
package toniarts.openkeeper.view.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DungeonHeart;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.controller.room.FiveByFiveRotatedController;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRoomContainerTest {

    private EntityData entityData;
    private MapRoomContainer container;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        container = new MapRoomContainer(entityData, null);
    }

    @AfterEach
    void tearDown() {
        container.stop();
        entityData.close();
    }

    @Test
    void isDungeonHeartFollowsTheDungeonHeartComponent() {
        EntityId heart = createRoom((short) 1);
        entityData.setComponent(heart, new DungeonHeart());
        EntityId other = createRoom((short) 2);

        container.start();

        assertTrue(container.getRoomInformation(heart).isDungeonHeart());
        assertFalse(container.getRoomInformation(other).isDungeonHeart());

        entityData.setComponent(other, new DungeonHeart());
        container.update();
        assertTrue(container.getRoomInformation(other).isDungeonHeart());

        entityData.removeComponent(other, DungeonHeart.class);
        container.update();
        assertFalse(container.getRoomInformation(other).isDungeonHeart());
    }

    @Test
    void dungeonHeartHealthIsTheHeartObjectsHealth() {
        EntityId heart = createRoom((short) 5);
        entityData.setComponent(heart, new DungeonHeart());
        EntityId other = createRoom((short) 1);
        entityData.setComponent(other, new Health(50, 100));
        EntityId heartObject = entityData.createEntity();
        entityData.setComponents(heartObject,
                new ObjectComponent(FiveByFiveRotatedController.OBJECT_HEART_ID, null),
                new Position(0, WorldUtils.pointToVector3f(new Point(0, 0))),
                new Health(87550, 100000));

        container.start();

        assertEquals(87, container.getRoomInformation(heart).getHealthPercent());
        assertEquals(50, container.getRoomInformation(other).getHealthPercent());

        entityData.setComponent(heartObject, new Health(40000, 100000));
        container.update();
        assertEquals(40, container.getRoomInformation(heart).getHealthPercent());
    }

    private EntityId createRoom(short roomId) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id,
                new RoomComponent(roomId, false, new Point(0, 0)),
                new Owner((short) 3, (short) 3),
                new Health(100, 100));
        return id;
    }
}
