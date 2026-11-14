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
import org.junit.jupiter.params.provider.EnumSource;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.CreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCreaturesGroupedTriggerTest {

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
    void comparesCurrentPossessionCountThroughParsedScript(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        try (Fixture f = new Fixture(comparison, threshold, (short) 1, (short) 0)) {
            f.leader((short) 3, 2);
            f.leader((short) 4, 99);
            assertEquals(expected, f.fires());
        }
    }

    @ParameterizedTest
    @EnumSource(ComparisonType.class)
    void alwaysFalseOutsidePossessionEvenForZeroOrNegativeThresholds(ComparisonType comparison) throws IOException {
        for (int threshold : new int[]{-1, 0, 1}) {
            try (Fixture f = new Fixture(comparison, threshold, (short) 1, (short) 0)) {
                f.leader((short) 4, 5);
                assertFalse(f.fires());
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"0,4", "1,0", "255,255"})
    void ignoresFlagAndOtherPlayerOperands(short flag, short otherPlayer) throws IOException {
        try (Fixture f = new Fixture(ComparisonType.GREATER_OR_EQUAL_TO, 5, flag, otherPlayer)) {
            f.leader((short) 4, 100);
            EntityId leader = f.leader((short) 3, 4);
            assertFalse(f.fires());
            f.count(leader, 5);
            assertTrue(f.fires());
            f.count(leader, 6);
            assertTrue(f.fires());
        }
    }

    @Test
    void newPossessionCountsZeroAndThresholdIsSignedInt() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 0, (short) 0, (short) 4)) {
            EntityId leader = f.leader((short) 3, 0);
            assertTrue(f.fires());
            f.count(leader, 1);
            assertFalse(f.fires());
        }
        for (int threshold : new int[]{Integer.MIN_VALUE, -1, Integer.MAX_VALUE}) {
            try (Fixture f = new Fixture(ComparisonType.GREATER_THAN, threshold, (short) 0, (short) 4)) {
                f.leader((short) 3, 0);
                assertEquals(threshold < 0, f.fires());
            }
        }
    }

    @Test
    void reflectsCountUpdatesPossessionEndLeaderRemovalAndSwitch() throws IOException {
        try (Fixture f = new Fixture(ComparisonType.EQUAL_TO, 5, (short) 1, (short) 0)) {
            EntityId first = f.leader((short) 3, 5);
            assertTrue(f.fires());
            f.count(first, 4); // Grouping gameplay publishes a follower release/loss.
            assertFalse(f.fires());
            f.count(first, 5);
            assertTrue(f.fires());
            f.data.removeComponent(first, Possessed.class);
            assertFalse(f.fires());
            f.data.setComponent(first, new Possessed(0, 0));
            assertFalse(f.fires());
            f.count(first, 5);
            assertTrue(f.fires());
            f.data.removeEntity(first);
            assertFalse(f.fires());
            EntityId second = f.leader((short) 3, 0);
            assertFalse(f.fires());
            f.count(second, 5);
            assertTrue(f.fires());
        }
    }

    private final class Fixture implements AutoCloseable {
        final EntityData data = new DefaultEntityData();
        final AtomicInteger flag = new AtomicInteger();
        final IKwdFile kwd;
        final ILevelInfo level;
        final CreaturesController creatures;

        Fixture(ComparisonType comparison, int threshold, short flagOperand, short otherPlayer) throws IOException {
            kwd = script(comparison, threshold, flagOperand, otherPlayer);
            level = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                    new Class<?>[]{ILevelInfo.class}, (p, m, a) -> switch (m.getName()) {
                        case "getLevelData" -> kwd;
                        case "getFlag" -> flag.get();
                        case "setFlag" -> { flag.set((int) a[1]); yield null; }
                        default -> throw new UnsupportedOperationException(m.getName());
                    });
            creatures = new CreaturesController(kwd, data, Map.of(), null, null, null, level);
        }

        EntityId leader(short owner, int count) {
            EntityId id = data.createEntity();
            data.setComponent(id, new Owner(owner, owner));
            count(id, count);
            return id;
        }

        void count(EntityId id, int count) {
            data.setComponent(id, new Possessed(0, 0, false, Double.NaN, count));
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
    private IKwdFile script(ComparisonType comparison, int threshold, short flag, short otherPlayer) throws IOException {
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
        triggers.put((byte) comparison.getValue()).put((byte) 0).put((byte) flag).put((byte) otherPlayer).putInt(threshold);
        triggers.putShort((short) 1).putShort((short) 0).putShort((short) -1);
        triggers.put((byte) TriggerGeneric.TargetType.PLAYER_CREATURES_GROUPED.getValue()).put((byte) 1);
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
