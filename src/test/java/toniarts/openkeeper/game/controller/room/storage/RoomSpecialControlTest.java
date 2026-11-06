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
package toniarts.openkeeper.game.controller.room.storage;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RoomSpecialControlTest {

    private static final Point FLOOR = new Point(3, 4);
    private static final Point OTHER_FLOOR = new Point(3, 5);
    private static final Point BOOKCASE = new Point(4, 4);

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId bookcase = entityData.createEntity();

    @Test
    void specialIsStoredOnTheFloorNotOnTheFurniture() {
        RoomSpecialControl control = createControl(FLOOR);
        EntityId special = addSpecial(BOOKCASE);

        assertNull(control.addItem(special, BOOKCASE));

        assertEquals(FLOOR, tileOf(special));
        assertNotNull(entityData.getComponent(special, Stored.class));
        assertEquals(1, control.getCurrentCapacity());
    }

    @Test
    void oneSpecialATile() {
        RoomSpecialControl control = createControl(FLOOR, OTHER_FLOOR);
        EntityId first = addSpecial(BOOKCASE);
        EntityId second = addSpecial(BOOKCASE);
        EntityId third = addSpecial(BOOKCASE);

        assertNull(control.addItem(first, null));
        assertNull(control.addItem(second, null));

        assertEquals(Set.of(FLOOR, OTHER_FLOOR), Set.of(tileOf(first), tileOf(second)));
        assertSame(third, control.addItem(third, null));
        assertEquals(2, control.getCurrentCapacity());
    }

    @Test
    void takenSpecialFreesItsTile() {
        RoomSpecialControl control = createControl(FLOOR);
        EntityId special = addSpecial(BOOKCASE);
        control.addItem(special, null);

        control.removeItem(special);

        assertNull(entityData.getComponent(special, Stored.class));
        assertEquals(0, control.getCurrentCapacity());
        assertNull(control.addItem(addSpecial(BOOKCASE), null));
    }

    @Test
    void destroyingTheLibraryLeavesTheSpecialsLying() {
        RoomSpecialControl control = createControl(FLOOR);
        EntityId special = addSpecial(BOOKCASE);
        control.addItem(special, null);

        control.destroy();

        assertNull(entityData.getComponent(special, Stored.class));
        assertEquals(FLOOR, tileOf(special));
    }

    private EntityId addSpecial(Point tile) {
        EntityId special = entityData.createEntity();
        entityData.setComponent(special, new Position(0, new Vector3f(tile.x, WorldUtils.FLOOR_HEIGHT, tile.y)));
        return special;
    }

    private Point tileOf(EntityId special) {
        return WorldUtils.vectorToPoint(entityData.getComponent(special, Position.class).position);
    }

    private RoomSpecialControl createControl(Point... floor) {
        entityData.setComponent(bookcase, new Position(0, new Vector3f(BOOKCASE.x, WorldUtils.FLOOR_HEIGHT, BOOKCASE.y)));
        EntityId roomEntity = entityData.createEntity();
        RoomInstance roomInstance = new RoomInstance(new Room());
        for (Point tile : floor) {
            roomInstance.addCoordinate(tile);
        }
        roomInstance.addCoordinate(BOOKCASE);
        IRoomController room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> (short) 1;
                    case "getRoomInstance" -> roomInstance;
                    case "isTileAccessible" -> true;
                    case "getFloorFurniture" -> Set.of(bookcase);
                    case "getFloorFurnitureCount" -> 1;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new RoomSpecialControl(null, room, entityData, () -> 0) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }
        };
    }

}
