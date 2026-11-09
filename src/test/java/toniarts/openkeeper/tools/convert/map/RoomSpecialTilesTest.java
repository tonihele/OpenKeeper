/* SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.ChickenGenerator;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.room.HatcheryController;
import toniarts.openkeeper.game.controller.room.NormalRoomController;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.*;

class RoomSpecialTilesTest {
    @Test
    void populatedObjectSlotsDoNotPlaceOrdinaryFurnitureWithoutTheFlag() {
        try (var fixture = new Fixture(false)) {
            fixture.normal().construct();
            assertTrue(fixture.spawns.isEmpty());
        }
    }

    @Test
    void enabledFurnitureStillUsesTheInteriorTileAndConfiguredObject() {
        try (var fixture = new Fixture(true)) {
            var controller = fixture.normal();
            controller.construct();
            assertEquals(List.of(new Spawn((short) 901, 21, 31)), fixture.spawns);
            assertEquals(1, controller.getFloorFurnitureCount());
        }
    }

    @Test
    void enabledFlagDoesNotAllowFurnitureWithoutASurroundingRoomSquare() {
        try (var fixture = new Fixture(true)) {
            fixture.instance.removeCoordinate(new Point(20, 30));
            fixture.normal().construct();
            assertTrue(fixture.spawns.isEmpty());
        }
    }

    @Test
    void rebuildingAfterDisablingTheFlagRemovesExistingFurniture() {
        try (var fixture = new Fixture(true)) {
            var controller = fixture.normal();
            controller.construct();
            assertEquals(1, controller.getFloorFurnitureCount());
            fixture.type.setFlags(EnumSet.noneOf(Room.RoomFlag.class));
            controller.construct();
            assertEquals(0, controller.getFloorFurnitureCount());
            assertEquals(1, fixture.spawns.size(), "Rebuilding must not spawn another object");
        }
    }

    @Test
    void hatcheryCoopsRemainChickenGeneratorsWithoutSpecialTiles() {
        try (var fixture = new Fixture(false)) {
            var timer = (IGameTimer) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{IGameTimer.class}, (proxy, method, args) -> 0.0);
            var controller = new HatcheryController(fixture.data.createEntity(), fixture.data, null,
                    fixture.instance, fixture.objects, timer);
            controller.construct();
            assertEquals(List.of(new Spawn((short) 901, 21, 31)), fixture.spawns);
            EntityId coop = controller.getFloorFurniture().iterator().next();
            assertNotNull(fixture.data.getComponent(coop, ChickenGenerator.class));
        }
    }

    private record Spawn(short objectId, int x, int y) {}

    private static final class Fixture implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        final Room type = new Room();
        final RoomInstance instance;
        final List<Spawn> spawns = new ArrayList<>();
        final IObjectsController objects = (IObjectsController) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{IObjectsController.class}, (proxy, method, args) -> {
                    if (method.getName().equals("loadObject") && args.length == 4) {
                        spawns.add(new Spawn((short) args[0], (int) args[2], (int) args[3]));
                        return data.createEntity();
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        Fixture(boolean enabled) {
            type.setName("Synthetic room");
            type.setRoomId((short) 99);
            type.setFlags(enabled ? EnumSet.of(Room.RoomFlag.SPECIAL_TILES) : EnumSet.noneOf(Room.RoomFlag.class));
            type.setObjects(List.of((short) 901, (short) 0, (short) 0, (short) 0,
                    (short) 0, (short) 0, (short) 0, (short) 0));
            instance = new RoomInstance(type);
            instance.setOwnerId(Player.KEEPER1_ID);
            for (int x = 20; x < 23; x++) {
                for (int y = 30; y < 33; y++) {
                    instance.addCoordinate(new Point(x, y));
                }
            }
        }

        NormalRoomController normal() {
            return new NormalRoomController(data.createEntity(), data, null, instance, objects);
        }

        @Override public void close() { data.close(); }
    }
}
