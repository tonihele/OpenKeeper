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
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.utils.Point;

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

    private EntityId createRoom(short roomId) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id,
                new RoomComponent(roomId, false, new Point(0, 0)),
                new Owner((short) 3, (short) 3),
                new Health(100, 100));
        return id;
    }
}
