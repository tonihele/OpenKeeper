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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerDestroysTriggerTest {

    @TempDir
    Path directory;

    @ParameterizedTest
    @EnumSource(ComparisonType.class)
    void parsedComparisonAndUnusedOperandsDoNotBlockTheChildAction(ComparisonType comparison) throws IOException {
        Fixture fixture = new Fixture(comparison, false);
        // The script specifies target 255 and player 255 with no such game data.
        // NONE and thresholds impossible for an empty destruction statistic still succeed.
        fixture.ticks(2);
        assertEquals(1, fixture.flags[0]);
    }

    @Test
    void anEnclosingConditionStillGatesTheUnconditionalNativeBranch() throws IOException {
        Fixture fixture = new Fixture(ComparisonType.GREATER_THAN, true);
        fixture.ticks(8);
        assertEquals(0, fixture.flags[0]);
        fixture.flags[1] = 1;
        fixture.ticks(8);
        assertEquals(1, fixture.flags[0]);
    }

    @Test
    void nativeUnconditionalSuccessDoesNotBypassTheExistingOneShotRepeatLimit() throws IOException {
        Fixture fixture = new Fixture(ComparisonType.EQUAL_TO, false);
        fixture.ticks(12);
        assertEquals(1, fixture.flags[0]);
    }

    private final class Fixture {
        final int[] flags = new int[2];
        final PlayerTriggerControl control;

        Fixture(ComparisonType comparison, boolean wrapped) throws IOException {
            IKwdFile kwd = script(comparison, wrapped);
            ILevelInfo level = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                    new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getLevelData" -> kwd;
                        case "getFlag" -> flags[((Number) args[0]).intValue()];
                        case "setFlag" -> { flags[((Number) args[0]).intValue()] = (int) args[1]; yield null; }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            // No game, player statistics or world queries are needed by the native condition.
            control = new PlayerTriggerControl(null, level, null, null, null, 1, (short) 3, null);
        }

        void ticks(int count) {
            for (int i = 0; i < count; i++) {
                control.update(.25f);
            }
        }
    }

    /** Generate a minimal synthetic level, optional enclosing flag condition and child action. */
    private IKwdFile script(ComparisonType comparison, boolean wrapped) throws IOException {
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
        triggers.put((byte) comparison.getValue()).put((byte) 255).put((byte) 0).put((byte) 255);
        triggers.putInt(1_000_000);
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
