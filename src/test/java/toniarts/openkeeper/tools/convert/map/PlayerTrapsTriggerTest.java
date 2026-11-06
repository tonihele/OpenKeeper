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

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.Death;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.TrapsController;
import toniarts.openkeeper.game.controller.player.PlayerTrapControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import toniarts.openkeeper.utils.Point;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTrapsTriggerTest {
    @TempDir Path directory;

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,1,false", "LESS_THAN,2,false", "LESS_THAN,3,true",
        "LESS_OR_EQUAL_TO,1,false", "LESS_OR_EQUAL_TO,2,true", "LESS_OR_EQUAL_TO,3,true",
        "EQUAL_TO,1,false", "EQUAL_TO,2,true", "EQUAL_TO,3,false",
        "GREATER_THAN,1,true", "GREATER_THAN,2,false", "GREATER_THAN,3,false",
        "GREATER_OR_EQUAL_TO,1,true", "GREATER_OR_EQUAL_TO,2,true", "GREATER_OR_EQUAL_TO,3,false",
        "NOT_EQUAL_TO,1,true", "NOT_EQUAL_TO,2,false", "NOT_EQUAL_TO,3,true"
    })
    void comparesPlacedTrapCountsThroughParsedScripts(ComparisonType comparison, int value,
            boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, value, (short) 0, (short) 1)) {
            f.trap((short) 1, (short) 3, false);
            f.trap((short) 2, (short) 3, true);
            f.trap((short) 1, (short) 4, false);
            assertEquals(expected, f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"0,4", "1,4", "8,1", "9,1", "16,3", "17,3", "24,1", "255,1"})
    void nativeModeBitsAndTheirPrecedenceArePreserved(short flags, int expected) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, expected, (short) 0, flags)) {
            f.populateFourTraps();
            assertTrue(f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"8,2,1", "17,1,3"})
    void blueprintAndBuiltModesIgnoreTheAuthoredType(short flags, short type, int expected) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, expected, type, flags)) {
            f.populateFourTraps();
            assertTrue(f.fires());
        }
    }

    @Test
    void ordinaryModeFiltersTypeAndIncludesItsBlueprints() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 2, (short) 1, (short) 1)) {
            f.populateFourTraps();
            assertTrue(f.fires());
        }
    }

    @Test
    void selectorIsUnsignedAndOtherPlayerOperandDoesNotOverrideTheNumber() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 255, (short) 0)) {
            assertEquals((short) 255, f.kwd.getTrigger(1).<Short>getUserData("targetId").shortValue());
            f.trap((short) 255, (short) 3, false);
            for (int i = 0; i < 4; i++) f.trap((short) 255, (short) 4, false);
            assertTrue(f.fires());
        }
    }

    @Test
    void blueprintCompletionOwnershipAndRemovalChangeTheLiveCount() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.GREATER_OR_EQUAL_TO, 1, (short) 1, (short) 17)) {
            EntityId id = f.trap((short) 1, (short) 3, true);
            assertFalse(f.fires());
            f.data.setComponents(id, new TrapComponent((short) 1, false), new Health(10,10));
            assertTrue(f.fires());
            f.data.setComponent(id, new Owner((short) 4, (short) 3));
            assertFalse(f.fires());
            f.data.setComponent(id, new Owner((short) 3, (short) 4));
            assertTrue(f.fires());
            f.data.removeComponent(id, Health.class);
            assertFalse(f.fires());
            EntityId second = f.trap((short) 2, (short) 3, false);
            assertTrue(f.fires());
            f.data.removeEntity(second);
            assertFalse(f.fires());
        }
    }

    @Test
    void cratesDoorMountedTrapsAndDeadTrapsDoNotCount() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, (short) 0, (short) 1)) {
            f.object((short) 36, (short) 3);
            EntityId door = f.trap((short) 1, (short) 3, false);
            f.data.setComponent(door, new DoorComponent((short) 1, false, false));
            EntityId dead = f.trap((short) 1, (short) 3, false);
            f.data.setComponent(dead, new Death(0));
            EntityId removed = f.trap((short) 1, (short) 3, false);
            f.data.removeComponent(removed, Health.class);
            assertTrue(f.fires());
        }
    }

    @Test
    void zeroHealthStaysInRosterUntilDeathIsProcessed() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 0, (short) 1)) {
            EntityId id = f.trap((short) 1, (short) 3, false);
            f.data.setComponent(id, new Health(0,10));
            assertTrue(f.fires());
            f.data.setComponent(id, new Death(0));
            assertFalse(f.fires());
        }
    }

    @Test
    void blueprintLoaderPublishesGameplayAndViewStateWithoutHealth() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 0, (short) 9)) {
            EntityId id = f.traps.addTrapBlueprint((short) 1, new Point(0,0), (short) 3);
            assertTrue(f.data.getComponent(id, TrapComponent.class).blueprint);
            assertTrue(f.data.getComponent(id, TrapViewState.class).blueprint);
            assertNull(f.data.getComponent(id, Health.class));
            assertTrue(f.fires());
            f.data.setComponent(id, new Death(0));
            assertFalse(f.fires());
        }
    }

    @Test
    void ordinaryTypedBoulderCountIncludesOnlyOwnedLiveBoulders() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 3, (short) 2, (short) 1)) {
            f.typeTwo.setShotTypeId((short) 26);
            f.trap((short) 2, (short) 3, false);
            f.trap((short) 2, (short) 3, true);
            EntityId boulder = f.object((short) 74, (short) 3);
            f.object((short) 74, (short) 4);
            f.object((short) 36, (short) 3);
            EntityId dead = f.object((short) 74, (short) 3);
            f.data.setComponent(dead, new Death(0));
            assertTrue(f.fires());
            f.data.removeEntity(boulder);
            assertFalse(f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"0,1,0", "0,2,1", "0,0,0", "8,2,0", "16,2,0"})
    void boulderSupplementAppliesOnlyToOrdinaryTypedShot26Counts(short flags, short type, int expected) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, expected, type, flags)) {
            f.typeTwo.setShotTypeId((short) 26);
            f.object((short) 74, (short) 3);
            assertTrue(f.fires());
        }
    }

    @Test
    void boulderIdentificationUsesTheTrapShotRatherThanHardcodedTrapType() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 1, (short) 1)) {
            f.typeOne.setShotTypeId((short) 26);
            f.object((short) 74, (short) 3);
            assertTrue(f.fires());
        }
    }

    @Test
    void secretMapZeroBoulderConditionWaitsForRollingBouldersToDisappear() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, (short) 2, (short) 1)) {
            f.typeTwo.setShotTypeId((short) 26);
            EntityId placed = f.trap((short) 2, (short) 3, false);
            assertFalse(f.fires());
            f.data.removeEntity(placed);
            EntityId rolling = f.object((short) 74, (short) 3);
            assertFalse(f.fires());
            f.data.removeEntity(rolling);
            assertTrue(f.fires());
        }
    }

    @Test
    void noneComparisonChecksDiscoveryRatherThanCountsOrNumericValue() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.NONE, 999, (short) 1, (short) 17)) {
            f.trap((short) 1, (short) 3, false);
            assertFalse(f.fires());
            f.available.setTypeAvailable(f.typeOne, true, false);
            assertFalse(f.fires());
            f.available.setTypeAvailable(f.typeOne, true, true);
            assertTrue(f.fires());
            f.available.setTypeAvailable(f.typeOne, false);
            assertFalse(f.fires());
        }
    }

    @Test
    void noneWithZeroTypeChecksAnyAvailableDiscoveredTrap() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.NONE, 0, (short) 0, (short) 1)) {
            assertFalse(f.fires());
            f.available.setTypeAvailable(f.typeTwo, true, false);
            assertFalse(f.fires());
            f.available.setTypeAvailable(f.typeTwo, true, true);
            assertTrue(f.fires());
        }
    }

    @Test
    void availabilityOfAnotherTypeDoesNotSatisfyASelectedType() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.NONE, 0, (short) 1, (short) 1)) {
            f.available.setTypeAvailable(f.typeTwo, true, true);
            assertFalse(f.fires());
        }
    }

    @Test
    void ordinaryNumericModeDoesNotCountResearchAvailability() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, (short) 1, (short) 1)) {
            f.available.setTypeAvailable(f.typeOne, true, true);
            assertTrue(f.fires());
        }
    }

    @Test
    void levelThreeZeroTrapConditionStopsMatchingWhenABlueprintIsPlaced() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, (short) 1, (short) 1)) {
            assertTrue(f.fires());
            f.trap((short) 1, (short) 3, true);
            assertFalse(f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"9,true,false", "17,false,true"})
    void levelThreeModesIgnoreTheSentryTrapSelector(short flags, boolean blueprintMatches,
            boolean builtMatches) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.GREATER_OR_EQUAL_TO, 1, (short) 3, flags)) {
            EntityId id = f.trap((short) 1, (short) 3, true);
            assertEquals(blueprintMatches, f.fires());
            f.data.setComponents(id, new TrapComponent((short) 1, false), new Health(10,10));
            assertEquals(builtMatches, f.fires());
        }
    }

    private final class Fixture implements AutoCloseable {
        final DefaultEntityData data = new DefaultEntityData();
        final AtomicInteger result = new AtomicInteger();
        final Trap typeOne = type((short) 1), typeTwo = type((short) 2);
        final PlayerTrapControl available = new PlayerTrapControl(new Keeper(false, (short) 3), List.of(typeOne, typeTwo));
        final TrapsController traps;
        final IKwdFile kwd;
        final ILevelInfo level;
        final IGameController game;

        Fixture(ComparisonType comparison, int value, short type, short flags) throws IOException {
            kwd = script(comparison, value, type, flags, (short) 4);
            level = proxy(ILevelInfo.class, (method,args)->switch(method) {
                case "getLevelData" -> kwd;
                case "getFlag" -> result.get();
                case "setFlag" -> { result.set((int)args[1]);yield null; }
                default -> throw new UnsupportedOperationException(method);
            });
            IKwdFile catalog = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                    new Class<?>[]{IKwdFile.class}, (p,m,a) -> m.getName().equals("getTrapById")
                    ? (((Number)a[0]).shortValue() == 1 ? typeOne : ((Number)a[0]).shortValue() == 2 ? typeTwo : null) : m.invoke(kwd,a));
            traps = new TrapsController(catalog, data, Map.of(), null, level);
            IGameWorldController world = proxy(IGameWorldController.class, (method,args)->switch(method) {
                case "getTrapsController" -> traps;
                default -> throw new UnsupportedOperationException(method);
            });
            IPlayerController player = proxy(IPlayerController.class, (method,args)->switch(method) {
                case "getTrapControl" -> available;
                default -> throw new UnsupportedOperationException(method);
            });
            game = proxy(IGameController.class, (method,args)->switch(method) {
                case "getGameWorldController" -> world;
                case "getPlayerController" -> { assertEquals((short)3,args[0]);yield player; }
                default -> throw new UnsupportedOperationException(method);
            });
        }
        EntityId trap(short type, short owner, boolean blueprint) {
            EntityId id = data.createEntity();
            data.setComponents(id, new TrapComponent(type,blueprint), new Owner(owner,owner));
            if (!blueprint) data.setComponent(id, new Health(10,10));
            return id;
        }
        EntityId object(short type, short owner) {
            EntityId id = data.createEntity();
            data.setComponents(id, new ObjectComponent(type, null), new Owner(owner,owner));
            return id;
        }
        void populateFourTraps() {
            trap((short)1,(short)3,false);
            trap((short)1,(short)3,true);
            trap((short)2,(short)3,false);
            trap((short)2,(short)3,false);
            trap((short)1,(short)4,false);
        }
        boolean fires() {
            result.set(0);
            var control = new PlayerTriggerControl(game,level,null,null,null,1,(short)3,null);
            control.update(.25f);
            control.update(.25f);
            return result.get()==1;
        }
        @Override public void close() {data.close();}
    }

    private static Trap type(short id) {
        Trap trap = new Trap();
        trap.setTrapId(id);
        trap.setOrderInEditor(id);
        return trap;
    }
    @FunctionalInterface private interface Call {Object invoke(String method,Object[] args);}
    private static <T>T proxy(Class<T>type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->call.invoke(m.getName(),a)));
    }

    /** Build a minimal level and trigger catalog entirely from synthetic data. */
    private IKwdFile script(ComparisonType comparison, int threshold, short type, short flag, short other) throws IOException {
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
        triggers.put((byte) comparison.getValue()).put((byte) type).put((byte) flag).put((byte) other).putInt(threshold);
        triggers.putShort((short) 1).putShort((short) 0).putShort((short) -1);
        triggers.put((byte) TriggerGeneric.TargetType.PLAYER_TRAPS.getValue()).put((byte) 1);
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
