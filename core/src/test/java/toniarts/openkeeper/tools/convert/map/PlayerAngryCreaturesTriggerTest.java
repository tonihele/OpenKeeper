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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerAngryCreaturesTriggerTest {

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
    void comparesAngryCountAndExecutesParsedChildActions(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, threshold, (short) 0)) {
            f.creature((short) 7, (short) 3, true);
            f.creature((short) 14, (short) 3, true);
            f.creature((short) 7, (short) 1, true);
            f.creature((short) 7, (short) 3, false);
            assertEquals(expected, f.fires());
        }
    }

    @Test
    void filtersTypesAndPreservesUnsignedSelector() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 255)) {
            assertEquals((short) 255, f.kwd.getTrigger(1).<Short>getUserData("creatureId").shortValue());
            f.creature((short) 255, (short) 3, true);
            f.creature((short) 7, (short) 3, true);
            f.creature((short) 255, (short) 1, true);
            assertTrue(f.fires());
        }
    }

    @Test
    void countsFullyAngryRatherThanUnhappyOrSumOfReasons() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 0)) {
            EntityId angry = f.creature((short) 7, (short) 3, false);
            f.data.setComponent(angry, new CreatureMood(0, 0, 0, 0, 0, 32768));
            EntityId unhappy = f.creature((short) 7, (short) 3, false);
            f.data.setComponent(unhappy, new CreatureMood(32767, 0, 0, 0, 0, 0));
            EntityId manyReasons = f.creature((short) 7, (short) 3, false);
            f.data.setComponent(manyReasons, new CreatureMood(10000, 10000, 10000, 10000, 10000, 10000));
            assertTrue(f.fires());
        }
    }

    @Test
    void excludesWorkersEvenWhenExplicitlySelected() throws IOException {
        for (short selector : new short[]{0, 1}) {
            try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, selector)) {
                f.creature((short) 1, (short) 3, true);
                f.definitions.get((short) 1).setFlags(EnumSet.of(Creature.CreatureFlag.IS_WORKER));
                assertTrue(f.fires());
            }
        }
    }

    @Test
    void usesOwnerRatherThanCaptorAndIgnoresOtherPlayerOperandBytes() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 0)) {
            f.creature((short) 7, (short) 3, true);
            EntityId prisoner = f.creature((short) 14, (short) 1, true);
            f.data.setComponents(prisoner, new Owner((short) 1, (short) 3),
                    new CreatureImprisoned(0, 0));
            for (int i = 0; i < 3; i++) {
                f.creature((short) 7, (short) 4, true);
            }
            assertEquals(1, f.creatures.getAngryCreatureCount((short) 1, (short) 0));
            // Bytes 2/3 suggest player 4 in other layouts; DKII ignores them here.
            assertTrue(f.fires());
        }
    }

    @Test
    void reflectsRecoveryConversionTypeChangesDeathAndRemoval() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 7)) {
            EntityId id = f.creature((short) 7, (short) 3, true);
            assertTrue(f.fires());
            f.data.setComponent(id, new CreatureMood(32767, 0, 0, 0, 0, 0));
            assertFalse(f.fires());
            f.data.setComponent(id, new CreatureMood(32768, 0, 0, 0, 0, 0));
            assertTrue(f.fires());
            f.data.setComponent(id, new Owner((short) 1, (short) 1));
            assertFalse(f.fires());
            f.data.setComponent(id, new Owner((short) 3, (short) 3));
            CreatureComponent changed = new CreatureComponent();
            changed.creatureId = 14;
            f.data.setComponent(id, changed);
            assertFalse(f.fires());
            changed = new CreatureComponent();
            changed.creatureId = 7;
            f.data.setComponent(id, changed);
            assertTrue(f.fires());
            f.data.removeComponent(id, Health.class);
            assertFalse(f.fires());
            EntityId removed = f.creature((short) 7, (short) 3, true);
            assertTrue(f.fires());
            f.data.removeEntity(removed);
            assertFalse(f.fires());
        }
    }

    @Test
    void needsLivingCreatureAndMoodComponentsButDoesNotExcludeUnconsciousCreatures() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 1, (short) 0)) {
            EntityId unconscious = f.creature((short) 7, (short) 3, true);
            f.data.setComponent(unconscious, new Unconscious(0));
            EntityId noMood = f.creature((short) 7, (short) 3, true);
            f.data.removeComponent(noMood, CreatureMood.class);
            EntityId corpse = f.creature((short) 7, (short) 3, true);
            f.data.removeComponent(corpse, Health.class);
            f.data.setComponents(f.data.createEntity(), new Health(100, 100),
                    new Owner((short) 3, (short) 3), new CreatureMood(32768, 0, 0, 0, 0, 0));
            assertTrue(f.fires());
        }
    }

    @Test
    void noMatchingTypeCountsAsZero() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, (short) 14)) {
            f.creature((short) 7, (short) 3, true);
            assertTrue(f.fires());
        }
    }

    @Test
    void emptyRosterCountsAsZero() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, (short) 0)) {
            assertTrue(f.fires());
        }
    }

    private final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final AtomicInteger flag = new AtomicInteger();
        final Map<Short, Creature> definitions = new HashMap<>();
        final IKwdFile kwd;
        final ILevelInfo level;
        final CreaturesController creatures;

        Fixture(ComparisonType comparison, int threshold, short selector) throws IOException {
            IKwdFile parsed = script(comparison, threshold, selector);
            kwd = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                    new Class<?>[]{IKwdFile.class}, (p, m, a) -> {
                        if (m.getName().equals("getCreature")) {
                            return definitions.get((Short) a[0]);
                        }
                        return m.invoke(parsed, a);
                    });
            level = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                    new Class<?>[]{ILevelInfo.class}, (p, m, a) -> switch (m.getName()) {
                        case "getLevelData" -> kwd;
                        case "getFlag" -> flag.get();
                        case "setFlag" -> { flag.set((int) a[1]); yield null; }
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
            creatures = new CreaturesController(kwd, data, Map.of(), null, null, null, level);
        }

        EntityId creature(short type, short owner, boolean angry) {
            EntityId id = data.createEntity();
            CreatureComponent creature = new CreatureComponent();
            creature.creatureId = type;
            data.setComponents(id, creature, new Health(100, 100), new Owner(owner, owner));
            definitions.computeIfAbsent(type, key -> {
                Creature definition = new Creature();
                definition.setFlags(EnumSet.noneOf(Creature.CreatureFlag.class));
                definition.getAttributes().setUnhappyThreshold((short) 50);
                return definition;
            });
            data.setComponent(id, new CreatureMood(angry ? 32768 : 0, 0, 0, 0, 0, 0));
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

    /** Build a minimal level and trigger catalog entirely from synthetic data. */
    private IKwdFile script(ComparisonType comparison, int threshold, short selector) throws IOException {
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
        triggers.put((byte) comparison.getValue()).put((byte) selector).put((byte) 0).put((byte) 4).putInt(threshold);
        triggers.putShort((short) 1).putShort((short) 0).putShort((short) -1);
        triggers.put((byte) TriggerGeneric.TargetType.PLAYER_ANGRY_CREATURES.getValue()).put((byte) 1);
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
