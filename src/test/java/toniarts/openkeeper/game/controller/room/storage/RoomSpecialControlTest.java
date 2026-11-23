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
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import static org.junit.jupiter.api.Assertions.*;

class RoomSpecialControlTest {
    private static final Point BOOKCASE = new Point(4, 4);
    private static final Point OTHER = new Point(8, 4);
    private final EntityData data = new DefaultEntityData();
    private final Set<EntityId> furniture = new LinkedHashSet<>();
    private final EntityId roomEntity = data.createEntity();
    private RoomSpecialControl specials;
    private RoomSpellBookControl books;

    private void createStorage(Point... bookcases) {
        for (Point p : bookcases) {
            furniture.add(item(p));
        }
        IRoomController room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> (short) 1;
                    case "getFloorFurniture" -> furniture;
                    case "getFloorFurnitureCount" -> furniture.size();
                    case "getObjectControl" -> args[0] == ObjectType.SPECIAL ? specials : books;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        specials = new RoomSpecialControl(null, room, data, () -> 0) {
            @Override protected int getNumberOfAccessibleTiles() { return furniture.size(); }
        };
        books = new RoomSpellBookControl(null, room, data, () -> 0) {
            @Override protected int getNumberOfAccessibleTiles() { return furniture.size(); }
        };
    }

    @Test
    void specialRestsAboveBookcaseAndNotOnPreferredEmptyFloor() {
        createStorage(BOOKCASE);
        EntityId special = item(new Point(3, 4));
        assertNull(specials.addItem(special, new Point(3, 4)));
        assertEquals(new Vector3f(4, WorldUtils.FLOOR_HEIGHT + 19f / 16, 4), position(special));
        assertEquals(ObjectType.SPECIAL, data.getComponent(special, Stored.class).objectType);
        assertEquals(2, specials.getMaxCapacity());
    }

    @Test
    void mixedPairSharesCapacityAndMovesBothItems() {
        createStorage(BOOKCASE);
        EntityId book = item(BOOKCASE);
        EntityId special = item(BOOKCASE);
        assertNull(books.addItem(book, BOOKCASE));
        assertNull(specials.addItem(special, BOOKCASE));
        assertEquals(-682f / 4096, position(book).x - 4, 0.000001);
        assertEquals(682f / 4096, position(special).x - 4, 0.000001);
        assertEquals(position(book).x, position(book).z);
        assertEquals(position(special).x, position(special).z);
        assertTrue(books.isFullCapacity());
        assertTrue(specials.isFullCapacity());
        assertEquals(2, books.getCurrentCapacity());
        assertEquals(2, specials.getCurrentCapacity());
        EntityId extra = item(OTHER);
        assertSame(extra, specials.addItem(extra, BOOKCASE));
        assertSame(extra, books.addItem(extra, BOOKCASE));
        assertNull(data.getComponent(extra, Stored.class));
        assertEquals(8, position(extra).x);
    }

    @Test
    void twoSpecialsOrTwoBooksFillTheBookcase() {
        createStorage(BOOKCASE);
        specials.addItem(item(BOOKCASE), null);
        specials.addItem(item(BOOKCASE), null);
        assertTrue(books.isFullCapacity());
        specials.destroy();
        books.addItem(item(BOOKCASE), null);
        books.addItem(item(BOOKCASE), null);
        assertTrue(books.isFullCapacity());
    }

    @Test
    void choosesNearestFreeBookcaseAndRechecksStalePreferredSlot() {
        createStorage(BOOKCASE, OTHER);
        EntityId first = item(OTHER);
        specials.addItem(first, null);
        assertEquals(8, position(first).x);
        books.addItem(item(OTHER), OTHER);
        EntityId next = item(OTHER);
        assertNull(specials.addItem(next, OTHER));
        assertEquals(4, position(next).x);
    }

    @Test
    void removalRestoresHeightAndFreesSharedSlotOnlyOnce() {
        createStorage(BOOKCASE);
        EntityId special = item(BOOKCASE);
        specials.addItem(special, null);
        specials.addItem(special, null);
        books.addItem(item(BOOKCASE), null);
        specials.removeItem(special);
        specials.removeItem(special);
        assertNull(data.getComponent(special, Stored.class));
        assertEquals(WorldUtils.FLOOR_HEIGHT, position(special).y);
        assertEquals(1, specials.getCurrentCapacity());
        assertEquals(1, books.getCurrentCapacity());
        assertFalse(books.isFullCapacity());
    }

    @Test
    void furnitureRemovalReleasesItemsButRebuildingTheSameTilePreservesThem() {
        createStorage(BOOKCASE);
        EntityId special = item(BOOKCASE);
        EntityId book = item(BOOKCASE);
        specials.addItem(special, null);
        books.addItem(book, null);
        furniture.clear();
        furniture.add(item(BOOKCASE));
        specials.furnitureChanged();
        assertNotNull(data.getComponent(special, Stored.class));
        furniture.clear();
        specials.furnitureChanged();
        books.furnitureChanged();
        assertNull(data.getComponent(special, Stored.class));
        assertNull(data.getComponent(book, Stored.class));
        assertEquals(WorldUtils.FLOOR_HEIGHT, position(book).y);
        assertTrue(specials.getAvailableCoordinates().isEmpty());
    }

    @Test
    void destroyingLibraryLeavesBothKindsOnTheFloor() {
        createStorage(BOOKCASE);
        EntityId special = item(BOOKCASE);
        EntityId book = item(BOOKCASE);
        specials.addItem(special, null);
        books.addItem(book, null);
        specials.destroy();
        books.destroy();
        assertNull(data.getComponent(special, Stored.class));
        assertNull(data.getComponent(book, Stored.class));
        assertEquals(WorldUtils.FLOOR_HEIGHT, position(special).y);
        assertEquals(WorldUtils.FLOOR_HEIGHT, position(book).y);
    }

    @Test
    void unfurnishedLibraryCannotStoreEitherKind() {
        createStorage();
        EntityId special = item(BOOKCASE);
        assertSame(special, specials.addItem(special, null));
        assertSame(special, books.addItem(special, null));
        assertTrue(specials.isFullCapacity());
        assertEquals(0, specials.getMaxCapacity());
    }

    private EntityId item(Point point) {
        EntityId item = data.createEntity();
        data.setComponent(item, new Position(0, new Vector3f(point.x, WorldUtils.FLOOR_HEIGHT, point.y)));
        return item;
    }

    private Vector3f position(EntityId item) {
        return data.getComponent(item, Position.class).position;
    }
}
