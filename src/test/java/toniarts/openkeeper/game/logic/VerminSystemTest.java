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
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController.ObjectType;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.tools.convert.map.HeroGateFixtures;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Bats appear in the Graveyards and rats in the Prisons, and both wander on
 * their owner's land (DKII.EXE 0x4BA0D0, 0x49FE60)
 */
class VerminSystemTest {

    private static final short KEEPER = 3;
    private static final short OTHER_KEEPER = 4;

    private final EntityData entityData = new DefaultEntityData();
    private final FakeMapData mapData = new FakeMapData(9, 9);
    private final Map<ObjectType, List<IRoomController>> rooms = new EnumMap<>(ObjectType.class);
    private final ScriptedRandom random = new ScriptedRandom();

    @Test
    void batAppearsInTheGraveyardOnFiveRollsInAHundred() {
        addRoom(ObjectType.CORPSE, Room.TileConstruction.NORMAL, 0, 0, 3, 3);
        VerminSystem system = system();

        // The bat roll just misses, and so does the rat roll
        random.values.addAll(List.of(5, 99));
        system.processTick(0.25f);
        assertEquals(0, count(VerminSystem.OBJECT_BAT_ID));

        // The bat roll hits, the bat is put on the room's fifth tile
        random.values.addAll(List.of(4, 4, 99));
        system.processTick(0.25f);

        assertEquals(1, count(VerminSystem.OBJECT_BAT_ID));
        EntityId bat = entityData.findEntity(null, ObjectComponent.class);
        Vector3f position = entityData.getComponent(bat, Position.class).position;
        assertEquals(new Point(1, 1), WorldUtils.vectorToPoint(position));
        assertEquals(WorldUtils.FLOOR_HEIGHT + 0.75f, position.y, 0.001f);
        assertEquals(KEEPER, entityData.getComponent(bat, Owner.class).ownerId);
    }

    @Test
    void thereIsOneBatForEveryNineGraveyardTiles() {
        addRoom(ObjectType.CORPSE, Room.TileConstruction.NORMAL, 0, 0, 3, 3);
        addRoom(ObjectType.CORPSE, Room.TileConstruction.NORMAL, 5, 0, 3, 3);
        VerminSystem system = system();

        // Every roll hits
        for (int tick = 0; tick < 5; tick++) {
            system.processTick(0.25f);
        }

        assertEquals(2, count(VerminSystem.OBJECT_BAT_ID));
    }

    @Test
    void ratsGoByThePrisonAndTortureChamberTilesAndAppearInThePrison() {
        addRoom(ObjectType.TORTUREE, Room.TileConstruction.NORMAL, 5, 5, 3, 3);
        VerminSystem system = system();

        // No Prison, no rats
        system.processTick(0.25f);
        assertEquals(0, count(VerminSystem.OBJECT_RAT_ID));

        addRoom(ObjectType.PRISONER, Room.TileConstruction.DOUBLE_QUAD, 0, 0, 3, 3);
        for (int tick = 0; tick < 5; tick++) {
            system.processTick(0.25f);
        }

        assertEquals(2, count(VerminSystem.OBJECT_RAT_ID));
        for (EntityId rat : entityData.findEntities(null, ObjectComponent.class)) {
            Point tile = WorldUtils.vectorToPoint(entityData.getComponent(rat, Position.class).position);
            assertEquals(new Point(0, 0), tile);
        }
    }

    @Test
    void verminWalksToANeighbouringTileOfItsOwner() {
        own(2, 2);
        own(2, 3);
        mapData.getTile(3, 2).ownerId = OTHER_KEEPER;
        EntityId rat = vermin(VerminSystem.OBJECT_RAT_ID, 2, 2);
        VerminSystem system = system();

        // No spawn rolls hit; the walk starts looking from the east
        random.values.addAll(List.of(99, 99, 1));
        system.processTick(0.25f);

        // East is another keeper's, south is its own
        Navigation navigation = entityData.getComponent(rat, Navigation.class);
        assertNotNull(navigation);
        assertEquals(new Point(2, 3), navigation.target);
        assertEquals(new Point(2, 2), WorldUtils.vectorToPoint(navigation.navigationPath.getFirst()));
        assertEquals(new Point(2, 3), WorldUtils.vectorToPoint(navigation.navigationPath.getLast()));
    }

    @Test
    void verminKeepsOutOfTheMiddleOfAPrison() {
        addRoom(ObjectType.PRISONER, Room.TileConstruction.DOUBLE_QUAD, 0, 0, 3, 3);
        EntityId rat = vermin(VerminSystem.OBJECT_RAT_ID, 1, 0);
        VerminSystem system = system();

        // No spawn rolls hit; the walk starts looking from the south, the middle of the Prison
        random.values.addAll(List.of(99, 99, 2));
        system.processTick(0.25f);

        assertEquals(new Point(0, 0), entityData.getComponent(rat, Navigation.class).target);
    }

    @Test
    void verminOnATileThatIsNotItsOwnersIsGone() {
        mapData.getTile(2, 2).ownerId = OTHER_KEEPER;
        EntityId bat = vermin(VerminSystem.OBJECT_BAT_ID, 2, 2);
        VerminSystem system = system();

        random.values.addAll(List.of(99, 99));
        system.processTick(0.25f);

        assertNull(entityData.getComponent(bat, ObjectComponent.class));
    }

    private int count(short objectId) {
        int count = 0;
        for (EntityId entityId : entityData.findEntities(null, ObjectComponent.class)) {
            if (entityData.getComponent(entityId, ObjectComponent.class).objectId == objectId) {
                count++;
            }
        }
        return count;
    }

    private void own(int x, int y) {
        mapData.getTile(x, y).ownerId = KEEPER;
    }

    private EntityId vermin(short objectId, int x, int y) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new ObjectComponent(objectId, null), new Owner(KEEPER, KEEPER),
                new Position(0, WorldUtils.pointToVector3f(x, y)));
        return entityId;
    }

    private void addRoom(ObjectType objectType, Room.TileConstruction tileConstruction, int x, int y, int width, int height) {
        Room room = HeroGateFixtures.heroGateRoom((short) 1, "Room", tileConstruction);
        RoomInstance roomInstance = new RoomInstance(room);
        for (int tileY = y; tileY < y + height; tileY++) {
            for (int tileX = x; tileX < x + width; tileX++) {
                roomInstance.addCoordinate(new Point(tileX, tileY));
                own(tileX, tileY);
            }
        }
        IRoomController roomController = proxy(IRoomController.class, (method, args) -> switch (method) {
            case "getRoomInstance" -> roomInstance;
            case "getRoom" -> room;
            default -> throw new UnsupportedOperationException(method);
        });
        rooms.computeIfAbsent(objectType, type -> new ArrayList<>()).add(roomController);
    }

    private VerminSystem system() {
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "getMapData" -> mapData;
            case "isSolid" -> false;
            case "getRoomsByFunction" -> rooms.getOrDefault((ObjectType) args[0], List.of());
            case "getRoomControllerByCoordinates" -> rooms.values().stream().flatMap(List::stream)
                    .filter(room -> room.getRoomInstance().hasCoordinate((Point) args[0])).findFirst().orElse(null);
            default -> throw new UnsupportedOperationException(method);
        });
        IObjectsController objectsController = proxy(IObjectsController.class, (method, args) -> {
            if (!method.equals("loadObject")) {
                throw new UnsupportedOperationException(method);
            }
            EntityId entityId = entityData.createEntity();
            Vector3f position = ((Vector3f) args[2]).clone().setY(WorldUtils.FLOOR_HEIGHT);
            entityData.setComponents(entityId, new ObjectComponent((Short) args[0], null),
                    new Owner((Short) args[1], (Short) args[1]), new Position(0, position));
            return entityId;
        });
        Keeper keeper = new Keeper(false, KEEPER);
        IPlayerController player = proxy(IPlayerController.class, (method, args) -> keeper);

        return new VerminSystem(entityData, mapController, objectsController, List.of(player), random);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

    private interface Answer {

        Object answer(String method, Object[] args);
    }

    /**
     * Hands out scripted values, zero when out of them
     */
    private static final class ScriptedRandom extends Random {

        private final Deque<Integer> values = new ArrayDeque<>();

        @Override
        public int nextInt(int bound) {
            Integer value = values.poll();
            return value != null ? value : 0;
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }

}
