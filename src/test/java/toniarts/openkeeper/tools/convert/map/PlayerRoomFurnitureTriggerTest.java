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
package toniarts.openkeeper.tools.convert.map;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerRoomControl;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.controller.room.NormalRoomController;
import toniarts.openkeeper.game.controller.room.TrainingRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerRoomFurnitureTriggerTest {

    @TempDir Path directory;

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,2,false", "LESS_THAN,3,false", "LESS_THAN,4,true",
        "LESS_OR_EQUAL_TO,2,false", "LESS_OR_EQUAL_TO,3,true", "LESS_OR_EQUAL_TO,4,true",
        "EQUAL_TO,2,false", "EQUAL_TO,3,true", "EQUAL_TO,4,false",
        "GREATER_THAN,2,true", "GREATER_THAN,3,false", "GREATER_THAN,4,false",
        "GREATER_OR_EQUAL_TO,2,true", "GREATER_OR_EQUAL_TO,3,true", "GREATER_OR_EQUAL_TO,4,false",
        "NOT_EQUAL_TO,2,true", "NOT_EQUAL_TO,3,false", "NOT_EQUAL_TO,4,true",
        "NONE,3,false"
    })
    void comparesTheOwnedTypedCounterThroughParsedScripts(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, threshold, 6)) {
            f.room(3,6,0,0,3,3);
            f.object(100,1,1);
            f.object(100,1,2);
            f.room(3,11,10,0,5,5);
            f.room(4,6,20,0,3,3);
            f.object(100,21,1);
            assertEquals(expected, f.fires());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0,1,8,16,24,255})
    void flagsDoNotSelectAnotherPlayerOrChangeTheCountMode(int flags) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 2, 6, flags)) {
            f.room(3,6,0,0,3,3);
            f.object(100,1,1);
            f.room(4,6,10,0,3,3);
            for (int i = 0; i < 4; i++) f.object(100,11,1);
            assertTrue(f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"6,0,0", "6,1,2", "6,2,3", "7,0,0", "7,1,2", "7,2,3",
            "10,0,0", "10,1,2", "10,2,3", "15,0,0", "15,1,2", "15,2,3",
            "12,0,0", "12,1,1", "12,2,2"})
    void configuredObjectRulesMatchNativeRoomTypes(int type, int objects, int expected) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, expected, type)) {
            f.room(3,type,0,0,3,3);
            for (int i = 0; i < objects; i++) f.object(100,1,1);
            assertTrue(f.fires());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1,2,4,5,8,9,17,18,20,26,255})
    void defaultRoomTypesUseTilesWithoutAvailabilityOrBuildabilityRequirements(int type) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 6, type)) {
            f.room(3,type,0,0,3,2);
            f.object(100,1,1);
            assertEquals((short) type, f.kwd.getTrigger(1).<Short>getUserData("roomId").shortValue());
            assertTrue(f.fires());
        }
    }

    @Test
    void perRoomTileCounterIsUnsigned16BitBeforePlayerAggregation() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 65535, 2)) {
            IRoomController room = f.room(3,2,0,0,256,256);
            assertEquals(0, room.getScriptFurnitureCount());
            room.getRoomInstance().removeCoordinate(new Point(255,255));
            assertTrue(f.fires());
        }
    }

    @Test
    void portalContributesZeroRegardlessOfTilesAndObjects() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 3)) {
            f.room(3,3,0,0,5,5);
            f.object(100,2,2);
            assertTrue(f.fires());
        }
    }

    @Test
    void eachPrisonAdds65535AndTheSumIsWiderThanUnsignedShort() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 131070, 11)) {
            IRoomController small = f.room(3,11,0,0,3,3);
            IRoomController large = f.room(3,11,10,0,5,5);
            assertEquals(65535, small.getScriptFurnitureCount());
            assertEquals(65535, large.getScriptFurnitureCount());
            f.room(4,11,20,0,3,3);
            assertTrue(f.fires());
            f.owned.get((short)3).onSold(small);
            assertFalse(f.fires());
            assertEquals(65535, f.owned.get((short)3).getRoomFurnitureCount((short)11));
        }
    }

    @ParameterizedTest
    @CsvSource({"EQUAL_TO,16842495,true", "EQUAL_TO,16842496,false",
            "LESS_THAN,16842496,true", "GREATER_THAN,16842494,true"})
    void largePrisonTotalsPreserveIntegerComparisonPrecision(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, threshold, 11)) {
            for (int i = 0; i < 257; i++) f.room(3,11,i*2,0,1,1);
            assertEquals(16842495, f.owned.get((short)3).getRoomFurnitureCount((short)11));
            assertEquals(expected, f.fires());
        }
    }

    @Test
    void prisonDoorLockingAndObjectPopulationDoNotAffectItsCounter() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 65535, 11)) {
            IRoomController prison = f.room(3,11,0,0,5,5);
            EntityId door = f.object(49,1,0);
            f.data.setComponent(door, new DoorComponent((short)1,false,false));
            assertTrue(f.fires());
            f.data.setComponent(door, new DoorComponent((short)1,true,false));
            f.object(100,2,2);
            prison.getRoomInstance().removeCoordinate(new Point(4,4));
            assertTrue(f.fires());
        }
    }

    @Test
    void allTypesSumTheirDifferentUnitsAndRetainPrisonContribution() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 65543, 0)) {
            f.room(3,11,0,0,5,5);
            f.room(3,7,10,0,3,3);
            f.object(100,11,1); // 2
            f.room(3,1,20,0,3,2); // 6
            f.room(3,3,30,0,5,5); // 0
            assertTrue(f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"13,1,1,1", "13,3,3,8", "13,5,5,16", "13,1,5,5",
            "16,1,1,1", "16,3,3,8", "16,5,5,16", "16,1,5,5"})
    void templeAndCombatPitCountBoundaryTiles(int type, int width, int height, int expected) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, expected, type)) {
            f.room(3,type,0,0,width,height);
            f.object(100,0,0);
            assertTrue(f.fires());
        }
    }

    @Test
    void irregularGeometryRequiresAllEightNeighborsInTheSameRoom() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 24, 13)) {
            IRoomController room = f.room(3,13,0,0,5,5);
            room.getRoomInstance().removeCoordinate(new Point(2,2));
            // The central hole breaks the interior test for every other inner tile.
            assertTrue(f.fires());
        }
    }

    @Test
    void graveyardExcludesOnlyInteriorTilesOccupiedByCrypts() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 8, 14)) {
            IRoomController graveyard = f.room(3,14,0,0,5,5);
            assertEquals(9, graveyard.getScriptFurnitureCount());
            EntityId crypt = f.object(75,2,2);
            f.object(75,2,2); // One excluded tile even with two objects on it.
            f.object(75,0,0); // Already outside the interior.
            f.object(75,20,20); // Outside this room.
            f.object(100,1,1); // Ordinary grave furniture doesn't exclude a tile.
            assertTrue(f.fires());
            f.data.removeComponent(crypt, Position.class);
            assertTrue(f.fires()); // The other crypt still occupies that tile.
        }
    }

    @Test
    void graveyardCryptRemovalAndGeometryChangesAffectTheLiveCount() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 9, 14)) {
            IRoomController graveyard = f.room(3,14,0,0,5,5);
            EntityId crypt = f.object(75,2,2);
            assertFalse(f.fires());
            f.data.removeEntity(crypt);
            assertTrue(f.fires());
            graveyard.getRoomInstance().removeCoordinate(new Point(0,0));
            assertFalse(f.fires()); // A diagonal corner also makes tile (1,1) a boundary tile.
            assertEquals(8, graveyard.getScriptFurnitureCount());
        }
    }

    @Test
    void configuredSlotsAreDeduplicatedAndSlotsSixAndSevenAreExcluded() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 3, 6)) {
            f.definition(6).setObjects(List.of((short)100,(short)100,(short)101,(short)102,
                    (short)103,(short)104,(short)105,(short)106));
            f.room(3,6,0,0,3,3);
            f.object(100,1,1);
            f.object(104,1,2);
            f.object(105,1,1);
            f.object(106,1,1);
            f.object(0,1,1);
            f.object(84,1,1);
            assertTrue(f.fires());
        }
    }

    @Test
    void objectPositionAndLifecycleDefineMembershipRatherThanObjectOwnerOrHealth() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 2, 6)) {
            f.room(3,6,0,0,3,3);
            EntityId object = f.object(100,1,1); // No Health and neutral owner.
            f.data.setComponent(object, new Owner((short)4,(short)4));
            assertTrue(f.fires());
            f.data.setComponent(object, new Position(0,WorldUtils.pointToVector3f(10,10)));
            assertFalse(f.fires());
            f.data.setComponent(object, new Position(0,WorldUtils.pointToVector3f(1,1)));
            f.data.setComponent(object, new Death(0));
            assertFalse(f.fires());
            f.data.removeComponent(object, Death.class);
            assertTrue(f.fires());
            f.data.removeComponent(object, Position.class);
            assertFalse(f.fires());
        }
    }

    @Test
    void captureAndSaleUpdateTheOwningPlayersCurrentRegistry() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 2, 6)) {
            IRoomController room = f.room(3,6,0,0,3,3);
            f.object(100,1,1);
            assertTrue(f.fires());
            f.owned.get((short)3).onCapturedByEnemy(room);
            room.getRoomInstance().setOwnerId((short)4);
            room.captured((short)4);
            f.owned.get((short)4).onCaptured(room);
            assertFalse(f.fires());
            assertEquals(2, f.owned.get((short)4).getRoomFurnitureCount((short)6));
            f.owned.get((short)4).onSold(room);
            assertEquals(0, f.owned.get((short)4).getRoomFurnitureCount((short)6));
        }
    }

    @Test
    void mergingAndSplittingChangesTheExtraOnePerNonemptyRoom() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 4, 6)) {
            IRoomController first = f.room(3,6,0,0,3,3);
            IRoomController second = f.room(3,6,0,4,3,3);
            f.object(100,1,1);
            f.object(100,1,5);
            assertTrue(f.fires());
            PlayerRoomControl rooms = f.owned.get((short)3);
            rooms.onSold(first);
            rooms.onSold(second);
            IRoomController combined = f.room(3,6,0,0,3,7);
            assertFalse(f.fires());
            assertEquals(3, rooms.getRoomFurnitureCount((short)6));
            rooms.onSold(combined);
            rooms.onBuild(first);
            rooms.onBuild(second);
            assertTrue(f.fires());
        }
    }

    @Test
    void removedRoomCannotContributeEvenIfNotificationHasNotRemovedItFromTheRegistry() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 11)) {
            IRoomController room = f.room(3,11,0,0,3,3);
            assertFalse(f.fires());
            f.data.removeEntity(room.getEntityId());
            assertTrue(f.fires());
        }
    }

    @Test
    void constructedTrainingRoomKeepsItsVisibleCountAndGameplayCapacity() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 2, 7)) {
            RoomInstance instance = f.instance(3,7,0,0,3,3);
            IObjectsController objects = proxy(IObjectsController.class, (name,args) -> switch(name) {
                case "loadObject" -> f.object(((Number)args[0]).intValue(),
                        ((Number)args[2]).intValue(), ((Number)args[3]).intValue());
                default -> throw new UnsupportedOperationException(name);
            });
            TrainingRoomController room = new TrainingRoomController(f.data.createEntity(), f.data,
                    f.kwd, instance, objects, () -> 0);
            room.construct();
            f.owned.get((short)3).onBuild(room);
            assertEquals(1, room.getFurnitureCount());
            assertEquals(1, room.getMaxCapacity());
            assertTrue(f.fires());
            assertEquals(2, room.getScriptFurnitureCount());
            assertEquals(1, room.getFurnitureCount());
            assertEquals(1, room.getMaxCapacity());
        }
    }

    @ParameterizedTest
    @CsvSource({"6,LESS_THAN,0,true", "6,LESS_THAN,1,false", "6,GREATER_OR_EQUAL_TO,0,false",
            "6,GREATER_OR_EQUAL_TO,1,true", "7,GREATER_OR_EQUAL_TO,1,true", "10,GREATER_OR_EQUAL_TO,1,true"})
    void campaignReadinessConditionsUseTheScriptCount(int type, ComparisonType comparison,
            int objects, boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, 1, type)) {
            f.room(3,type,0,0,3,3);
            for (int i = 0; i < objects; i++) f.object(100,1,1);
            assertEquals(expected,f.fires());
        }
    }

    @Test
    void emptyRegistryAndUnmatchedSelectorCountAsZero() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 6)) {
            assertTrue(f.fires());
            f.room(3,11,0,0,3,3);
            assertTrue(f.fires());
        }
    }

    private final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final AtomicInteger flag = new AtomicInteger();
        final Map<Short,Room> types = new HashMap<>();
        final Map<Short,PlayerRoomControl> owned = Map.of(
                (short)3,new PlayerRoomControl(new Keeper(false,(short)3),List.of()),
                (short)4,new PlayerRoomControl(new Keeper(false,(short)4),List.of()));
        final IKwdFile kwd;
        final ILevelInfo level;
        final IGameController game;

        Fixture(ComparisonType comparison, int threshold, int selector) throws IOException {
            this(comparison,threshold,selector,1);
        }

        Fixture(ComparisonType comparison, int threshold, int selector, int flags) throws IOException {
            kwd = script(comparison,threshold,(short)selector,(short)flags);
            level = proxy(ILevelInfo.class, (name,args) -> switch(name) {
                case "getLevelData" -> kwd;
                case "getFlag" -> flag.get();
                case "setFlag" -> {flag.set((int)args[1]); yield null;}
                default -> throw new UnsupportedOperationException(name);
            });
            game = proxy(IGameController.class, (name,args) -> switch(name) {
                case "getPlayerController" -> proxy(IPlayerController.class, (method,a) -> switch(method) {
                    case "getRoomControl" -> owned.get((short)args[0]);
                    default -> throw new UnsupportedOperationException(method);
                });
                default -> throw new UnsupportedOperationException(name);
            });
        }

        Room definition(int type) {
            return types.computeIfAbsent((short)type, id -> {
                Room room = RoomFixtures.room(id,0);
                room.setObjects(List.of((short)100,(short)0,(short)0,(short)0,
                        (short)0,(short)0,(short)0,(short)0));
                return room;
            });
        }

        RoomInstance instance(int owner, int type, int x, int y, int width, int height) {
            RoomInstance instance = new RoomInstance(definition(type));
            instance.setOwnerId((short)owner);
            for (int yy = y; yy < y+height; yy++) {
                for (int xx = x; xx < x+width; xx++) instance.addCoordinate(new Point(xx,yy));
            }
            return instance;
        }

        IRoomController room(int owner, int type, int x, int y, int width, int height) {
            IRoomController room = new NormalRoomController(data.createEntity(),data,kwd,
                    instance(owner,type,x,y,width,height),null);
            owned.get((short)owner).onBuild(room);
            return room;
        }

        EntityId object(int type, int x, int y) {
            EntityId id = data.createEntity();
            data.setComponents(id,new ObjectComponent((short)type,null),new Owner((short)0,(short)0),
                    new Position(0,WorldUtils.pointToVector3f(x,y)));
            return id;
        }

        boolean fires() {
            flag.set(0);
            var control = new PlayerTriggerControl(game,level,null,null,null,1,(short)3,null);
            control.update(.25f);
            control.update(.25f);
            return flag.get()==1;
        }

        @Override public void close() {data.close();}
    }

    @FunctionalInterface
    private interface Call {Object invoke(String name,Object[] args);}

    private static <T>T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,m,a)->call.invoke(m.getName(),a)));
    }

    /** Build a minimal level and trigger catalog entirely from synthetic data. */
    private IKwdFile script(ComparisonType comparison, int threshold, short selector, short flags) throws IOException {
        int levelInfoSize = 25603;
        ByteBuffer level = bytes(56 + levelInfoSize + 8 + 4 * 72);
        header(level, 220, 28);
        level.putShort((short) 4).putShort((short) 0).putInt(0);
        level.position(48);
        level.putInt(223).putInt(levelInfoSize);
        level.position(56 + levelInfoSize);
        level.putInt(222).putInt(4 * 72);
        path(level, 0, "Globals.kld");
        path(level, 100, "Map.kld");
        path(level, 190, "Things.kld");
        path(level, 210, "Triggers.kld");
        Files.write(directory.resolve("Level.kwd"), level.array());
        Files.write(directory.resolve("Globals.kld"), new byte[0]);

        ByteBuffer map = bytes(40);
        header(map, 100, 8);
        map.putInt(1).putInt(1).putInt(102).putInt(4);
        Files.write(directory.resolve("Map.kld"), map.array());

        ByteBuffer things = bytes(56);
        header(things, 190, 28);
        things.putInt(0).putInt(1);
        things.position(48);
        things.putInt(192).putInt(0);
        Files.write(directory.resolve("Things.kld"), things.array());

        ByteBuffer triggers = bytes(108);
        header(triggers, 210, 32);
        triggers.putInt(1).putInt(1).putInt(1);
        triggers.position(52);
        triggers.putInt(212).putInt(16);
        triggers.putInt(213).putInt(16);
        triggers.put((byte) comparison.getValue()).put((byte) selector).put((byte) flags).put((byte) 4).putInt(threshold);
        triggers.putShort((short) 1).putShort((short) 0).putShort((short) -1);
        triggers.put((byte) TriggerGeneric.TargetType.PLAYER_ROOM_FURNITURE.getValue()).put((byte) 1);
        triggers.putInt(214).putInt(16);
        triggers.put((byte) 0).put((byte) 10).putShort((short) 0).putInt(1);
        triggers.putShort((short) -1).putShort((short) 0).putShort((short) 0);
        triggers.put((byte) TriggerAction.ActionType.FLAG.getValue()).put((byte) 1);
        Files.write(directory.resolve("Triggers.kld"), triggers.array());
        return KwdFile.load(directory.toString(), directory.resolve("Level.kwd"));
    }

    private static ByteBuffer bytes(int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void header(ByteBuffer buffer, int type, int extraSize) {
        buffer.putInt(type).putInt(4).putInt(buffer.capacity()).putInt(type + 1).putInt(extraSize);
    }

    private static void path(ByteBuffer buffer, int type, String name) {
        buffer.putInt(type).putInt(0);
        int end = buffer.position() + 64;
        buffer.put(name.getBytes(StandardCharsets.US_ASCII));
        buffer.position(end);
    }
}
