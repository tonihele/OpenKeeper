/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.controller;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.ObjectViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.storage.RoomGoldControl;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.GameObject;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;
import static toniarts.openkeeper.game.component.ObjectViewState.GameObjectAnimState.*;

class StoredGoldAppearanceTest {

    private final EntityData data = new DefaultEntityData();
    private final Point tile = new Point(5, 7);

    @ParameterizedTest
    @ValueSource(ints = {1000, 3000})
    void depositsAndWithdrawalsChangeModelsAtNativeThresholds(int capacity) throws Exception {
        GameObject definition = definition();
        IKwdFile kwd = kwd(definition);
        ObjectsController objects = new ObjectsController(kwd, data, Map.of(), null, null, null);
        RoomInstance instance = new RoomInstance(new Room());
        instance.addCoordinate(tile);
        EntityId roomEntity = data.createEntity();
        IRoomController parent = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> (short) 3;
                    case "getRoomInstance" -> instance;
                    case "isTileAccessible" -> true;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        RoomGoldControl storage = new RoomGoldControl(kwd, parent, data, null, objects) {
            @Override protected int getGoldPerObject() { return capacity; }
            @Override protected int getNumberOfAccessibleTiles() { return 1; }
        };
        int first = capacity / 3;
        int second = 2 * capacity / 3;
        assertEquals(0, storage.addItem(first - 1, tile));
        EntityId container = storage.getItems(tile).iterator().next();
        Position originalPosition = data.getComponent(container, Position.class);
        assertEquals(ADDITIONAL_RESOURCE_1, view(container).animState);
        EntitySet watched = data.getEntities(Gold.class, ObjectViewState.class);
        try {
            assertEquals(0, storage.addItem(1, tile));
            assertTrue(watched.applyChanges(), "Deposits must publish ECS component changes");
            assertEquals(ADDITIONAL_RESOURCE_2, view(container).animState);
            assertEquals(0, storage.addItem(second - first - 1, tile));
            assertEquals(ADDITIONAL_RESOURCE_2, view(container).animState);
            assertEquals(0, storage.addItem(1, tile));
            assertEquals(MESH_RESOURCE, view(container).animState);
            assertEquals(0, storage.addItem(capacity - second, tile));
            assertEquals(capacity, data.getComponent(container, Gold.class).gold);
            assertEquals(capacity, storage.getCurrentCapacity());
            assertEquals(17, storage.addItem(17, tile), "Full containers keep their capacity");
            assertEquals(0, storage.removeGold(capacity - second + 1));
            assertEquals(ADDITIONAL_RESOURCE_2, view(container).animState);
            assertEquals(0, storage.removeGold(second - first));
            assertEquals(ADDITIONAL_RESOURCE_1, view(container).animState);
            assertSame(originalPosition, data.getComponent(container, Position.class),
                    "Changing the model must preserve the initial orientation");
            assertEquals(first - 1, storage.getCurrentCapacity());
            assertEquals(0, storage.removeGold(first - 1));
            assertTrue(storage.getItems(tile).isEmpty());
            assertNull(data.getComponent(container, Gold.class));
            assertEquals(0, storage.getCurrentCapacity());
        } finally {
            watched.release();
        }
    }

    @Test
    void newContainersUseRandomFullTurnYawInNativeSteps() throws Exception {
        ObjectsController objects = new ObjectsController(kwd(definition()), data, Map.of(), null, null, null);
        Set<Float> angles = new HashSet<>();
        for (int i = 0; i < 32; i++) {
            EntityId entity = objects.addRoomGold((short) 3, 5, 7, 10, 1000);
            float angle = data.getComponent(entity, Position.class).rotation;
            assertTrue(angle >= 0 && angle < FastMath.TWO_PI);
            float step = angle / (FastMath.TWO_PI / 2048);
            assertEquals(Math.round(step), step, 0.0002f);
            angles.add(angle);
        }
        assertTrue(angles.size() > 1);
    }

    @Test
    void absentSelectedArtLeavesExistingStateAlone() throws Exception {
        GameObject definition = definition();
        set(definition, "additionalResources", List.of());
        EntityId entity = data.createEntity();
        ObjectViewState original = new ObjectViewState((short) 3, GameObject.State.NONE, MESH_RESOURCE, false);
        data.setComponent(entity, original);
        StoredGoldVisuals.update(data, entity, new Gold(10, 1000), definition);
        assertSame(original, view(entity));
    }

    private ObjectViewState view(EntityId entity) {
        return data.getComponent(entity, ObjectViewState.class);
    }

    private static GameObject definition() throws Exception {
        GameObject object = new GameObject();
        set(object, "objectId", (short) 3);
        set(object, "flags", EnumSet.of(GameObject.ObjectFlag.OBJECT_TYPE_GOLD));
        set(object, "startState", GameObject.State.NONE);
        set(object, "additionalResources", List.of(new ArtResource(), new ArtResource(), new ArtResource()));
        return object;
    }

    private static IKwdFile kwd(GameObject definition) {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getObject" -> definition;
                    case "getThings" -> List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void set(Object object, String fieldName, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
