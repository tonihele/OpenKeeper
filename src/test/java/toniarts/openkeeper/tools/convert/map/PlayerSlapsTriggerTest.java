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

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSlapsTriggerTest {
    @TempDir Path directory;

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
    void comparesTheRealCumulativeCounterThroughParsedScripts(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        Fixture f = new Fixture(comparison, threshold, (short) 1, (short) 0);
        f.stats.entitySlapped();
        f.stats.entitySlapped();
        assertEquals(expected, f.fires());
    }

    @ParameterizedTest
    @CsvSource({"0,4", "255,4", "0,255"})
    void ignoresTheEditorOtherPlayerOptionLikeDKII(short flag, short other) throws IOException {
        Fixture f = new Fixture(ComparisonType.EQUAL_TO, 2, flag, other);
        f.stats.entitySlapped();
        f.stats.entitySlapped();
        // The fixture rejects any request for another player's stats.
        assertTrue(f.fires());
    }

    @Test
    void startsAtZero() throws IOException {
        assertTrue(new Fixture(ComparisonType.EQUAL_TO, 0, (short) 1, (short) 0).fires());
    }

    @Test
    void secretOneThresholdActivatesOnTheEleventhSlap() throws IOException {
        Fixture f = new Fixture(ComparisonType.GREATER_OR_EQUAL_TO, 11, (short) 1, (short) 0);
        for (int i = 0; i < 10; i++) f.stats.entitySlapped();
        assertFalse(f.fires());
        f.stats.entitySlapped();
        assertTrue(f.fires());
        f.stats.entitySlapped();
        assertTrue(f.fires());
    }

    private final class Fixture {
        final PlayerStatsControl stats = new PlayerStatsControl();
        final AtomicInteger result = new AtomicInteger();
        final ILevelInfo level;
        final IGameController game;
        Fixture(ComparisonType comparison, int threshold, short flag, short other) throws IOException {
            IKwdFile kwd = script(comparison, threshold, flag, other);
            assertEquals(flag, kwd.getTrigger(1).<Short>getUserData("flag").shortValue());
            assertEquals(other, kwd.getTrigger(1).<Short>getUserData("playerId").shortValue());
            level = proxy(ILevelInfo.class, (method, args) -> switch (method) {
                case "getLevelData" -> kwd;
                case "getFlag" -> result.get();
                case "setFlag" -> { result.set((int) args[1]); yield null; }
                default -> throw new UnsupportedOperationException(method);
            });
            IPlayerController player = proxy(IPlayerController.class, (method, args) -> switch (method) {
                case "getStatsControl" -> stats;
                default -> throw new UnsupportedOperationException(method);
            });
            game = proxy(IGameController.class, (method, args) -> {
                assertEquals("getPlayerController", method);
                assertEquals((short) 3, args[0]);
                return player;
            });
        }
        boolean fires() {
            result.set(0);
            PlayerTriggerControl control = new PlayerTriggerControl(game, level, null, null, null, 1, (short) 3, null);
            control.update(.25f);
            control.update(.25f);
            return result.get() == 1;
        }
    }

    @FunctionalInterface private interface Call { Object invoke(String method, Object[] args); }
    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, m, a) -> call.invoke(m.getName(), a)));
    }

    /** Build a minimal level and trigger catalog entirely from synthetic data. */
    private IKwdFile script(ComparisonType comparison, int threshold, short flag, short other) throws IOException {
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
        triggers.put((byte) comparison.getValue()).put((byte) 0).put((byte) flag).put((byte) other).putInt(threshold);
        triggers.putShort((short) 1).putShort((short) 0).putShort((short) -1);
        triggers.put((byte) TriggerGeneric.TargetType.PLAYER_SLAPS.getValue()).put((byte) 1);
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
