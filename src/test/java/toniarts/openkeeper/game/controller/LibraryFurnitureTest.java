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
package toniarts.openkeeper.game.controller;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.LibraryController;
import toniarts.openkeeper.game.controller.room.NormalRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomSpellBookControl;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic rooms; no original map or model data is needed. */
class LibraryFurnitureTest {

    private final EntityData entities = new DefaultEntityData();

    @Test
    void bookcasesUseThreeAdditionalModelsAndAlternateAbsoluteRows() {
        RoomInstance instance = room((short) 64);
        rectangle(instance, 1, 1, 5, 5);
        LibraryController library = library(instance);
        library.construct();

        Map<Point, Appearance> furniture = furniture(library);
        assertEquals(9, furniture.size());
        // Calculated from the tile-seeded formula, on a small synthetic grid.
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_1, furniture.get(new Point(2, 2)).model());
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_3, furniture.get(new Point(3, 2)).model());
        assertEquals(GameObjectAnimState.ADDITIONAL_RESOURCE_2, furniture.get(new Point(3, 3)).model());
        for (var entry : furniture.entrySet()) {
            assertEquals((entry.getKey().y & 1) * FastMath.PI, entry.getValue().rotation());
            assertTrue(List.of(GameObjectAnimState.ADDITIONAL_RESOURCE_1,
                    GameObjectAnimState.ADDITIONAL_RESOURCE_2,
                    GameObjectAnimState.ADDITIONAL_RESOURCE_3).contains(entry.getValue().model()));
        }
        for (EntityId object : library.getFloorFurniture()) {
            ObjectViewState view = entities.getComponent(object, ObjectViewState.class);
            assertEquals(64, view.objectId);
            assertEquals(GameObject.State.NONE, view.state);
            assertFalse(view.visible);
            assertEquals(7, view.lairCreatureTypeId);
        }
    }

    @Test
    void reconstructionAndExpansionKeepExistingTileAppearanceAndStoredBooks() {
        RoomInstance instance = room((short) 64);
        rectangle(instance, 4, 4, 5, 5);
        LibraryController library = library(instance);
        library.construct();
        Map<Point, Appearance> before = furniture(library);
        RoomSpellBookControl books = library.getObjectControl(ObjectType.SPELL_BOOK);
        EntityId book = entities.createEntity();
        entities.setComponent(book, new Position(0, new Vector3f(0, 1, 0)));
        assertNull(books.addItem(book, new Point(5, 5)));
        Position storedPosition = entities.getComponent(book, Position.class);

        library.construct();
        assertEquals(before, furniture(library));
        assertSame(books, library.getObjectControl(ObjectType.SPELL_BOOK));

        // Expand north and west, changing the room's matrix origin.
        rectangle(instance, 2, 2, 7, 7);
        library.construct();
        Map<Point, Appearance> expanded = furniture(library);
        before.forEach((tile, appearance) -> assertEquals(appearance, expanded.get(tile)));
        assertEquals(25, expanded.size());
        assertEquals(26, library.getMaxCapacity(ObjectType.RESEARCHER));
        assertEquals(50, books.getMaxCapacity());
        assertEquals(1, books.getCurrentCapacity());
        assertEquals(List.of(book), books.getItems(new Point(5, 5)));
        assertSame(storedPosition, entities.getComponent(book, Position.class));
        assertEquals(library.getEntityId(), entities.getComponent(book, Stored.class).room);
        assertEquals(expanded.keySet(), new java.util.HashSet<>(library.getObjectControl(ObjectType.RESEARCHER).getAvailableCoordinates()));
    }

    @Test
    void floorGeometryAndCapacitiesStayTheSameForDifferentRoomShapes() {
        for (int shape = 0; shape < 3; shape++) {
            RoomInstance instance = room((short) 64);
            int expected;
            if (shape == 0) {
                rectangle(instance, 13, 33, 11, 11);
                expected = 81;
            } else if (shape == 1) {
                rectangle(instance, 2, 7, 3, 7);
                expected = 5;
            } else {
                rectangle(instance, 1, 1, 5, 5);
                rectangle(instance, 6, 1, 3, 3);
                expected = 12;
            }
            LibraryController library = library(instance);
            library.construct();
            assertEquals(expected, library.getFloorFurnitureCount());
            assertEquals(expected + 1, library.getMaxCapacity(ObjectType.RESEARCHER));
            assertEquals(2 * expected, library.getMaxCapacity(ObjectType.SPELL_BOOK));
        }
    }

    @Test
    void otherFloorObjectsAndOtherRoomsKeepTheirDefaultAppearance() {
        RoomInstance otherObject = room((short) 58);
        rectangle(otherObject, 1, 1, 3, 3);
        LibraryController library = library(otherObject);
        library.construct();
        assertEquals(new Appearance(GameObjectAnimState.MESH_RESOURCE, 0), furniture(library).get(new Point(2, 2)));

        RoomInstance otherRoom = room((short) 64);
        rectangle(otherRoom, 1, 2, 3, 3);
        NormalRoomController normal = new NormalRoomController(entities.createEntity(), entities, null, otherRoom, objects());
        normal.construct();
        assertEquals(new Appearance(GameObjectAnimState.MESH_RESOURCE, 0), furniture(normal).get(new Point(2, 3)));
    }

    private RoomInstance room(short floorObject) {
        Room room = RoomFixtures.room((short) 3, List.of(floorObject, (short) 0, (short) 0,
                (short) 0, (short) 0, (short) 0));
        return new RoomInstance(room);
    }

    private void rectangle(RoomInstance room, int x, int y, int width, int height) {
        for (int tx = x; tx < x + width; tx++) {
            for (int ty = y; ty < y + height; ty++) {
                room.addCoordinate(new Point(tx, ty));
            }
        }
    }

    private LibraryController library(RoomInstance room) {
        return new LibraryController(entities.createEntity(), entities, null, room, objects(), () -> 0);
    }

    private IObjectsController objects() {
        return (IObjectsController) Proxy.newProxyInstance(IObjectsController.class.getClassLoader(),
                new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("loadObject")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    EntityId object = entities.createEntity();
                    float rotation = args.length == 5 ? (float) args[4] : 0;
                    entities.setComponents(object, new Position(rotation, WorldUtils.pointToVector3f(new Point((int) args[2], (int) args[3])).setY(WorldUtils.FLOOR_HEIGHT)),
                            new ObjectViewState((short) args[0], GameObject.State.NONE, GameObjectAnimState.MESH_RESOURCE, false, (short) 7));
                    return object;
                });
    }

    private Map<Point, Appearance> furniture(NormalRoomController room) {
        Map<Point, Appearance> result = new HashMap<>();
        for (EntityId object : room.getFloorFurniture()) {
            Position position = entities.getComponent(object, Position.class);
            assertEquals(WorldUtils.FLOOR_HEIGHT, position.position.y);
            result.put(WorldUtils.vectorToPoint(position.position),
                    new Appearance(entities.getComponent(object, ObjectViewState.class).animState, position.rotation));
        }
        return result;
    }

    private record Appearance(GameObjectAnimState model, float rotation) {
    }
}
