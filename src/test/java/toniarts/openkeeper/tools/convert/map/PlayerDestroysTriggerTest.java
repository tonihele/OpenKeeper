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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TrapComponent;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.entity.EntityController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDestroysTriggerTest {

    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,1,false", "LESS_THAN,2,false", "LESS_THAN,3,true",
        "LESS_OR_EQUAL_TO,1,false", "LESS_OR_EQUAL_TO,2,true", "LESS_OR_EQUAL_TO,3,true",
        "EQUAL_TO,1,false", "EQUAL_TO,2,true", "EQUAL_TO,3,false",
        "GREATER_THAN,1,true", "GREATER_THAN,2,false", "GREATER_THAN,3,false",
        "GREATER_OR_EQUAL_TO,1,true", "GREATER_OR_EQUAL_TO,2,true", "GREATER_OR_EQUAL_TO,3,false",
        "NOT_EQUAL_TO,1,true", "NOT_EQUAL_TO,2,false", "NOT_EQUAL_TO,3,true",
        "NONE,2,false"
    })
    void comparesCreditedDestructionAndExecutesParsedChildActions(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, threshold, 0, 1, 0, false)) {
            f.destroy(f.door(1, false), (short) 3);
            f.destroy(f.trap(2, false), (short) 3);
            f.destroy(f.door(1, false), (short) 4);
            f.ticks(2);
            assertEquals(expected ? 1 : 0, f.flags[0]);
        }
    }

    @Test
    void emptyStatisticsDoNotSatisfyAPositiveThreshold() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.GREATER_THAN, 0, 0, 1, 0, false)) {
            f.ticks(2);
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void specificSelectorResolvesCollidingIdsToDoorsAsTheEditorDoes() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 1, 1, 0, false)) {
            f.destroy(f.door(1, false), (short) 3);
            f.destroy(f.trap(1, false), (short) 3);
            f.destroy(f.trap(1, false), (short) 3);
            assertEquals(3, f.stats.get((short) 3).getDestroyedCount((short) 0, false));
            f.ticks(2);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void trapOnlySelectorPreservesUnsignedIds() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 200, 1, 0, false)) {
            f.destroy(f.door(1, false), (short) 3);
            f.destroy(f.trap(200, false), (short) 3);
            f.ticks(2);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void comparesTheSameStatisticForAnotherPlayer() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.LESS_THAN, 999, 0, 2, 4, false)) {
            f.destroy(f.door(1, false), (short) 3);
            f.destroy(f.trap(2, false), (short) 4);
            f.destroy(f.trap(2, false), (short) 4);
            f.ticks(2);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void creditsTheLethalHitRatherThanTheVictimOrEarlierOrLaterAttackers() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 0, 1, 0, false)) {
            EntityId target = f.door(1, false);
            EntityController.setDamage(f.data, target, 4, (short) 3);
            EntityController.setDamage(f.data, target, 6, (short) 4);
            EntityController.setDamage(f.data, target, 50, (short) 3);
            f.health.processTick(.25f);
            assertEquals(0, f.stats.get((short) 3).getDestroyedCount((short) 0, false));
            assertEquals(1, f.stats.get((short) 4).getDestroyedCount((short) 0, false));
            f.health.processTick(.25f);
            assertEquals(1, f.stats.get((short) 4).getDestroyedCount((short) 0, false));
        }
    }

    @Test
    void healingCanReopenTheLethalThresholdForAnotherAttacker() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 0, 1, 0, false)) {
            EntityId target = f.door(1, false);
            EntityController.setDamage(f.data, target, 10, (short) 3);
            EntityController.setDamage(f.data, target, -6);
            EntityController.setDamage(f.data, target, 6, (short) 4);
            f.health.processTick(.25f);
            assertEquals(0, f.stats.get((short) 3).getDestroyedCount((short) 0, false));
            assertEquals(1, f.stats.get((short) 4).getDestroyedCount((short) 0, false));
        }
    }

    @Test
    void excludesSalesBlueprintsUnattributedDamageAndOtherEntityTypes() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 0, 1, 0, false)) {
            EntityId sold = f.trap(2, false);
            EntityController.setDamage(f.data, sold, 10, (short) 3);
            f.data.removeEntity(sold); // Selling removes the entity, bypassing lethal health processing.
            f.destroy(f.door(1, true), (short) 3);
            f.destroy(f.trap(2, true), (short) 3);
            f.destroy(f.door(1, false), (short) 0);
            EntityId object = f.data.createEntity();
            f.data.setComponent(object, new Health(10, 10));
            f.destroy(object, (short) 3);
            f.ticks(2);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void unattributedLethalDamageDoesNotReuseAnEarlierPlayersCredit() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 0, 1, 0, false)) {
            EntityId target = f.door(1, false);
            EntityController.setDamage(f.data, target, 4, (short) 3);
            EntityController.setDamage(f.data, target, 6);
            f.health.processTick(.25f);
            f.ticks(2);
            assertEquals(1, f.flags[0]);
        }
    }

    @Test
    void earlierNonlethalDamageDoesNotRetainCreditAcrossHealthUpdates() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 0, 1, 0, false)) {
            EntityId target = f.door(1, false);
            EntityController.setDamage(f.data, target, 4, (short) 3);
            f.health.processTick(.25f);
            f.destroy(target, (short) 4);
            f.ticks(2);
            assertEquals(1, f.flags[0]);
            assertEquals(1, f.stats.get((short) 4).getDestroyedCount((short) 0, false));
        }
    }

    @Test
    void enclosingConditionsAndOneShotRepeatLimitsStillApply() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.GREATER_THAN, 0, 0, 1, 0, true)) {
            f.destroy(f.door(1, false), (short) 3);
            f.ticks(8);
            assertEquals(0, f.flags[0]);
            f.flags[1] = 1;
            f.ticks(12);
            assertEquals(1, f.flags[0]);
        }
    }

    private final class Fixture implements AutoCloseable {
        final int[] flags = new int[2];
        final EntityData data = new DefaultEntityData();
        final Map<Short, PlayerStatsControl> stats = Map.of((short) 3, new PlayerStatsControl(),
                (short) 4, new PlayerStatsControl());
        final PlayerTriggerControl control;
        final HealthSystem health;

        Fixture(ComparisonType comparison, int threshold, int type, int operand, int other, boolean wrapped) throws IOException {
            IKwdFile parsed = script(comparison, wrapped, type, threshold, operand, other);
            Door definition = new Door();
            definition.setDoorId((short) 1);
            IKwdFile kwd = proxy(IKwdFile.class, (proxy, method, args) -> {
                if (method.getName().equals("getDoorById")) {
                    return ((Number) args[0]).intValue() == 1 ? definition : null;
                }
                return method.invoke(parsed, args);
            });
            ILevelInfo level = proxy(ILevelInfo.class, (proxy, method, args) -> switch (method.getName()) {
                case "getLevelData" -> kwd;
                case "getFlag" -> flags[((Number) args[0]).intValue()];
                case "setFlag" -> { flags[((Number) args[0]).intValue()] = (int) args[1]; yield null; }
                default -> throw new UnsupportedOperationException(method.getName());
            });
            Map<Short, IPlayerController> players = new HashMap<>();
            for (short id : new short[]{3, 4}) {
                players.put(id, proxy(IPlayerController.class, (proxy, method, args) -> switch (method.getName()) {
                    case "getKeeper" -> new Keeper(false, id);
                    case "getStatsControl" -> stats.get(id);
                    default -> throw new UnsupportedOperationException(method.getName());
                }));
            }
            IGameController game = proxy(IGameController.class, (proxy, method, args) -> switch (method.getName()) {
                case "getPlayerController" -> players.get((Short) args[0]);
                default -> throw new UnsupportedOperationException(method.getName());
            });
            IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (proxy, method, args) -> switch (method.getName()) {
                case "getEntityController" -> new EntityController((EntityId) args[0], data, null, null);
                default -> throw new UnsupportedOperationException(method.getName());
            });
            GameEventService events = proxy(GameEventService.class, (proxy, method, args) -> null);
            Map<MiscVariable.MiscType, MiscVariable> settings = new HashMap<>();
            for (var setting : List.of(MiscVariable.MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                    MiscVariable.MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                    MiscVariable.MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
                settings.put(setting, CreatureSleepFixtures.variable(setting, 0));
            }
            health = new HealthSystem(data, lookup, settings, null, level, players.values(), null, () -> 0, events);
            control = new PlayerTriggerControl(game, level, null, null, null, 1, (short) 3, null);
        }

        EntityId door(int type, boolean blueprint) {
            EntityId entity = data.createEntity();
            data.setComponents(entity, new DoorComponent((short) type, false, blueprint), new Health(10, 10),
                    new Owner((short) 4, (short) 4));
            return entity;
        }

        EntityId trap(int type, boolean blueprint) {
            EntityId entity = data.createEntity();
            data.setComponents(entity, new TrapComponent((short) type), new TrapViewState((short) type, blueprint),
                    new Health(10, 10), new Owner((short) 4, (short) 4));
            return entity;
        }

        void destroy(EntityId entity, short player) {
            EntityController.setDamage(data, entity, 10, player);
            health.processTick(.25f);
        }

        void ticks(int count) {
            for (int i = 0; i < count; i++) {
                control.update(.25f);
            }
        }

        @Override
        public void close() {
            health.stop();
            data.close();
        }
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> handler.invoke(p, method, args)));
    }

    /** Generate a minimal synthetic level, optional enclosing flag condition and child action. */
    private IKwdFile script(ComparisonType comparison, boolean wrapped, int type, int threshold, int operand, int other) throws IOException {
        int levelInfoSize = 25603;
        ByteBuffer level = bytes(56 + levelInfoSize + 8 + 3 * 72);
        header(level, 220, 28);
        level.putShort((short) 3).putShort((short) 0).putInt(0);
        level.position(48);
        level.putInt(223).putInt(levelInfoSize);
        level.position(56 + levelInfoSize);
        level.putInt(222).putInt(3 * 72);
        path(level, 0, "Globals.kld");
        path(level, 100, "Map.kld");
        path(level, 210, "Triggers.kld");
        Files.write(directory.resolve("Level.kwd"), level.array());
        Files.write(directory.resolve("Globals.kld"), new byte[0]);

        ByteBuffer map = bytes(40);
        header(map, 100, 8);
        map.putInt(1).putInt(1).putInt(102).putInt(4);
        Files.write(directory.resolve("Map.kld"), map.array());

        ByteBuffer triggers = bytes(wrapped ? 132 : 108);
        header(triggers, 210, 32);
        triggers.putInt(wrapped ? 2 : 1).putInt(1).putInt(1);
        triggers.position(52);
        triggers.putInt(212).putInt(16);
        if (wrapped) {
            triggers.putInt(213).putInt(16);
            triggers.put((byte) ComparisonType.EQUAL_TO.getValue());
            triggers.put((byte) 1).put((byte) 1).put((byte) 0).putInt(1);
            links(triggers, 1, 2, TriggerGeneric.TargetType.FLAG.getValue());
        }
        triggers.putInt(213).putInt(16);
        triggers.put((byte) comparison.getValue()).put((byte) type).put((byte) operand).put((byte) other);
        triggers.putInt(threshold);
        links(triggers, wrapped ? 2 : 1, -1, TriggerGeneric.TargetType.PLAYER_DESTROYS.getValue());
        triggers.putInt(214).putInt(16);
        triggers.put((byte) 0).put((byte) 18).putShort((short) 0).putInt(1); // flag 0 += 1
        links(triggers, -1, 0, TriggerAction.ActionType.FLAG.getValue());
        Files.write(directory.resolve("Triggers.kld"), triggers.array());
        return KwdFile.load(directory.toString(), directory.resolve("Level.kwd"));
    }

    private static void links(ByteBuffer buffer, int id, int child, int type) {
        buffer.putShort((short) id).putShort((short) 0).putShort((short) child);
        buffer.put((byte) type).put((byte) 1);
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
