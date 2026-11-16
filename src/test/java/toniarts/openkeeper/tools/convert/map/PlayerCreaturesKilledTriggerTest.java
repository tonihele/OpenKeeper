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
import java.util.Map;
import toniarts.openkeeper.game.data.Keeper;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric.ComparisonType;

import static org.junit.jupiter.api.Assertions.*;

class PlayerCreaturesKilledTriggerTest {

    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource({
        "LESS_THAN,1,false", "LESS_THAN,2,false", "LESS_THAN,3,true",
        "LESS_OR_EQUAL_TO,1,false", "LESS_OR_EQUAL_TO,2,true", "LESS_OR_EQUAL_TO,3,true",
        "EQUAL_TO,1,false", "EQUAL_TO,2,true", "EQUAL_TO,3,false",
        "GREATER_THAN,1,true", "GREATER_THAN,2,false", "GREATER_THAN,3,false",
        "GREATER_OR_EQUAL_TO,1,true", "GREATER_OR_EQUAL_TO,2,true", "GREATER_OR_EQUAL_TO,3,false",
        "NOT_EQUAL_TO,1,true", "NOT_EQUAL_TO,2,false", "NOT_EQUAL_TO,3,true", "NONE,2,false"
    })
    void parsedConditionComparesLossesAndRunsChildAction(ComparisonType comparison,
            int threshold, boolean expected) throws IOException {
        PlayerStatsControl victim = new PlayerStatsControl();
        victim.creatureLost();
        victim.creatureLost();
        assertEquals(expected, fires(script(comparison, threshold, (short) 0), Map.of((short) 3, victim)));
    }

    @Test
    void opponentFilterUsesKillersPerOwnerCountIncludingWorkersAndOwnKills() throws IOException {
        PlayerStatsControl victim = new PlayerStatsControl();
        PlayerStatsControl killer = new PlayerStatsControl();
        victim.creatureLost(); victim.creatureLost(); victim.creatureLost(); victim.creatureLost();
        killer.creatureKilled((short) 4, (short) 3, false);
        killer.creatureKilled((short) 4, (short) 3, true);
        killer.creatureKilled((short) 4, (short) 5, false);
        victim.creatureKilled((short) 3, (short) 3, false);
        Map<Short, PlayerStatsControl> players = Map.of((short) 3, victim, (short) 4, killer);
        assertTrue(fires(script(ComparisonType.EQUAL_TO, 4, (short) 0), players));
        assertTrue(fires(script(ComparisonType.EQUAL_TO, 2, (short) 4), players));
        assertTrue(fires(script(ComparisonType.EQUAL_TO, 1, (short) 3), players));
        assertTrue(fires(script(ComparisonType.EQUAL_TO, 0, (short) 5), players));
    }

    @Test
    void noLossesAndMissingOpponentsCountAsZero() throws IOException {
        Map<Short, PlayerStatsControl> players = Map.of((short) 3, new PlayerStatsControl());
        assertTrue(fires(script(ComparisonType.EQUAL_TO, 0, (short) 0), players));
        assertTrue(fires(script(ComparisonType.EQUAL_TO, 0, (short) 4), players));
        assertFalse(fires(script(ComparisonType.GREATER_THAN, 0, (short) 4), players));
        assertTrue(fires(script(ComparisonType.GREATER_THAN, -1, (short) 0), players));
    }

    @Test
    void selectorIsUnsignedAndOtherOperandBytesAreIgnored() throws IOException {
        PlayerStatsControl killer = new PlayerStatsControl();
        killer.creatureKilled((short) 255, (short) 3, false);
        IKwdFile parsed = script(ComparisonType.EQUAL_TO, 1, (short) 255);
        assertEquals((short) 255, parsed.getTrigger(1).<Short>getUserData("playerId"));
        assertTrue(fires(parsed, Map.of((short) 3, new PlayerStatsControl(), (short) 255, killer)));
    }

    @Test
    void currentPlayerAliasResolvesTheVictimOwner() throws IOException {
        PlayerStatsControl killer = new PlayerStatsControl();
        killer.creatureKilled((short) 4, Player.KEEPER1_ID, false);
        killer.creatureKilled((short) 4, (short) 5, false);
        assertTrue(fires(script(ComparisonType.EQUAL_TO, 1, (short) 4),
                Map.of(Player.KEEPER1_ID, new PlayerStatsControl(), (short) 4, killer), (short) 0));
    }

    @Test
    void laterLossesAreVisibleAndPlayersRemainIsolated() throws IOException {
        PlayerStatsControl victim = new PlayerStatsControl();
        PlayerStatsControl other = new PlayerStatsControl();
        other.creatureLost();
        Map<Short, PlayerStatsControl> players = Map.of((short) 3, victim, (short) 4, other);
        IKwdFile parsed = script(ComparisonType.EQUAL_TO, 1, (short) 0);
        assertFalse(fires(parsed, players));
        victim.creatureLost();
        assertTrue(fires(parsed, players));
    }

    private boolean fires(IKwdFile kwd, Map<Short, PlayerStatsControl> players) {
        return fires(kwd, players, (short) 3);
    }

    private boolean fires(IKwdFile kwd, Map<Short, PlayerStatsControl> players, short watchedPlayer) {
        AtomicInteger flag = new AtomicInteger();
        IGameController game = (IGameController) Proxy.newProxyInstance(IGameController.class.getClassLoader(),
                new Class<?>[]{IGameController.class}, (p, m, a) -> {
                    short owner = (short) a[0];
                    PlayerStatsControl stats = players.get(owner);
                    if (stats == null) return null;
                    return Proxy.newProxyInstance(IPlayerController.class.getClassLoader(),
                            new Class<?>[]{IPlayerController.class}, (p2, m2, a2) -> switch (m2.getName()) {
                                case "getStatsControl" -> stats;
                                case "getKeeper" -> new Keeper(false, owner);
                                default -> throw new AssertionError(m2.getName());
                            });
                });
        ILevelInfo level = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (p, m, a) -> switch (m.getName()) {
                    case "getLevelData" -> kwd;
                    case "getFlag" -> flag.get();
                    case "setFlag" -> { flag.set((int) a[1]); yield null; }
                    default -> throw new AssertionError(m.getName());
                });
        var control = new PlayerTriggerControl(game, level, null, null, null, 1, watchedPlayer, null);
        control.update(.25f);
        control.update(.25f);
        return flag.get() == 1;
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
        triggers.put((byte) comparison.getValue()).put((byte) 254).put((byte) 253).put((byte) selector).putInt(threshold);
        triggers.putShort((short) 1).putShort((short) 0).putShort((short) -1);
        triggers.put((byte) TriggerGeneric.TargetType.PLAYER_CREATURES_KILLED.getValue()).put((byte) 1);
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
