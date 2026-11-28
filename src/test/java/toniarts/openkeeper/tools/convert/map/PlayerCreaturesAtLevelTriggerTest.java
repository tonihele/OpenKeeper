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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureExperience;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCreaturesAtLevelTriggerTest {

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
    void comparesCountAndExecutesParsedChildActions(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, threshold, 2)) {
            f.creature(7, 3, 2, false);
            f.creature(14, 3, 3, false);
            f.creature(7, 3, 1, false);
            f.creature(7, 4, 2, false);
            f.creature(9, 3, 10, true);
            assertEquals(expected, f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"0,4", "1,4", "2,3", "3,2", "10,1", "11,0", "128,0", "255,0"})
    void countsAtOrAboveTheUnsignedMinimumWithoutClamping(int minimum, int expected) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, expected, minimum)) {
            assertEquals((short) minimum, f.kwd.getTrigger(1).<Short>getUserData("level").shortValue());
            for (int level : new int[]{1, 2, 3, 10}) f.creature(7, 3, level, false);
            assertTrue(f.fires());
        }
    }

    @ParameterizedTest
    @CsvSource({"0,0", "7,1", "255,255", "7,8", "14,16"})
    void selectorAndValuePlayerFlagDoNotFilterOrReplaceTheNumericOperand(int selector, int flags) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 2, 2, selector, flags)) {
            f.creature(7, 3, 2, false);
            f.creature(14, 3, 3, false);
            // Operand byte 3 is level 2, not player 2, whose roster differs.
            for (int i = 0; i < 5; i++) f.creature(7, 2, 3, false);
            assertTrue(f.fires());
        }
    }

    @Test
    void levelTwoTrainingConditionMatchesHigherLevelsButNotWorkers() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.GREATER_OR_EQUAL_TO, 1, 2)) {
            f.creature(9, 3, 2, true);
            EntityId trainee = f.creature(7, 3, 1, false);
            assertFalse(f.fires());
            f.data.setComponent(trainee, experience(2));
            assertTrue(f.fires());
            f.data.setComponent(trainee, experience(3));
            assertTrue(f.fires());
        }
    }

    @Test
    void currentExperienceOwnershipAndRemovalChangeTheLiveCount() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 5)) {
            EntityId id = f.creature(7, 3, 4, false);
            assertFalse(f.fires());
            f.data.setComponent(id, experience(5));
            assertTrue(f.fires());
            f.data.setComponent(id, new Owner((short) 4, (short) 3));
            assertFalse(f.fires());
            f.data.setComponent(id, new Owner((short) 3, (short) 4));
            assertTrue(f.fires());
            f.data.setComponent(id, experience(4));
            assertFalse(f.fires());
            f.data.setComponent(id, experience(6));
            assertTrue(f.fires());
            f.data.removeEntity(id);
            assertFalse(f.fires());
        }
    }

    @Test
    void workersAreExcludedRegardlessOfTheirCreatureType() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 2)) {
            f.creature(7, 3, 10, true);
            EntityId ordinary = f.creature(9, 3, 2, false);
            assertTrue(f.fires());
            CreatureComponent updated = new CreatureComponent(f.data.getComponent(ordinary, CreatureComponent.class));
            updated.worker = true;
            f.data.setComponent(ordinary, updated);
            assertFalse(f.fires());
        }
    }

    @Test
    void prisonersCountForTheOwnerRatherThanTheCaptor() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 2)) {
            EntityId captive = f.creature(7, 3, 2, false);
            f.data.setComponents(captive, new Owner((short) 3, (short) 4), new CreatureImprisoned(0,0));
            EntityId enemy = f.creature(7, 4, 2, false);
            f.data.setComponents(enemy, new Owner((short) 4, (short) 3), new CreatureImprisoned(0,0));
            assertTrue(f.fires());
        }
    }

    @Test
    void unconsciousAndZeroHealthCreaturesRemainUntilRosterHealthIsRemoved() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, 2)) {
            EntityId id = f.creature(7, 3, 2, false);
            f.data.setComponents(id, new Unconscious(0), new Health(0,100));
            assertTrue(f.fires());
            f.data.removeComponent(id, Health.class);
            assertFalse(f.fires());
        }
    }

    @Test
    void incompleteEntitiesDoNotCount() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 2)) {
            EntityId noExperience = f.creature(7, 3, 2, false);
            f.data.removeComponent(noExperience, CreatureExperience.class);
            EntityId noCreature = f.creature(7, 3, 2, false);
            f.data.removeComponent(noCreature, CreatureComponent.class);
            EntityId noOwner = f.creature(7, 3, 2, false);
            f.data.removeComponent(noOwner, Owner.class);
            EntityId noHealth = f.creature(7, 3, 2, false);
            f.data.removeComponent(noHealth, Health.class);
            assertTrue(f.fires());
        }
    }

    @Test
    void emptyRosterCountsAsZero() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, 0)) {
            assertTrue(f.fires());
        }
    }

    private final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final AtomicInteger flag = new AtomicInteger();
        final IKwdFile kwd;
        final ILevelInfo level;
        final CreaturesController creatures;

        Fixture(ComparisonType comparison, int threshold, int minimumLevel) throws IOException {
            this(comparison, threshold, minimumLevel, 0, 1);
        }

        Fixture(ComparisonType comparison, int threshold, int minimumLevel, int selector, int flags) throws IOException {
            kwd = script(comparison, threshold, (short) minimumLevel, (short) selector, (short) flags);
            level = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                    new Class<?>[]{ILevelInfo.class}, (p, m, a) -> switch (m.getName()) {
                        case "getLevelData" -> kwd;
                        case "getFlag" -> flag.get();
                        case "setFlag" -> { flag.set((int) a[1]); yield null; }
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
            creatures = new CreaturesController(kwd, data, Map.of(), null, null, null, level);
        }

        EntityId creature(int type, int owner, int level, boolean worker) {
            EntityId id = data.createEntity();
            CreatureComponent creature = new CreatureComponent();
            creature.creatureId = (short) type;
            creature.worker = worker;
            data.setComponents(id, creature, experience(level), new Health(100,100),
                    new Owner((short) owner, (short) owner));
            return id;
        }

        boolean fires() {
            flag.set(0);
            var control = new PlayerTriggerControl(null, level, null, null, creatures, 1, (short) 3, null);
            control.update(.25f);
            control.update(.25f);
            return flag.get() == 1;
        }

        @Override
        public void close() {
            data.close();
        }
    }

    private static CreatureExperience experience(int level) {
        return new CreatureExperience(level, 0, 0, 0, 0);
    }

    /** Build a minimal level and trigger catalog entirely from synthetic data. */
    private IKwdFile script(ComparisonType comparison, int threshold, short minimumLevel, short selector, short flags) throws IOException {
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
        triggers.put((byte) comparison.getValue()).put((byte) selector).put((byte) flags).put((byte) minimumLevel).putInt(threshold);
        triggers.putShort((short) 1).putShort((short) 0).putShort((short) -1);
        triggers.put((byte) TriggerGeneric.TargetType.PLAYER_CREATURES_AT_LEVEL.getValue()).put((byte) 1);
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
