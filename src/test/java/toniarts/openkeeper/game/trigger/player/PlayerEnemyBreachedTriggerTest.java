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
package toniarts.openkeeper.game.trigger.player;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PlayerBreachFixtures;
import toniarts.openkeeper.tools.convert.map.Trigger;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerEnemyBreachedTriggerTest {

    @Test
    void deliversToTheBreachingPlayersRootAndIgnoresNativeUnusedFilters() {
        Fixture f = new Fixture(255);
        // The authored other-player selector is 255; DKII does not inspect it.
        f.scripts.onEnemyBreached((short) 3);
        assertEquals(1, f.flags[0]);
        assertEquals(0, f.flags[2]);
        f.scripts.onEnemyBreached((short) 4);
        assertEquals(1, f.flags[0]);
        assertEquals(1, f.flags[2]);
    }

    @Test
    void ordinaryTicksNeverTreatTheBreachAsAHistoricalFlag() {
        Fixture f = new Fixture(255);
        f.ticks(12);
        assertEquals(0, f.flags[0]);
        f.scripts.onEnemyBreached((short) 3);
        f.ticks(12);
        assertEquals(1, f.flags[0]);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void eachActualEventUsesTheExistingRepeatLimit(int repeat, int expected) {
        Fixture f = new Fixture(repeat);
        for (int i = 0; i < 3; i++) {
            f.scripts.onEnemyBreached((short) 3);
        }
        assertEquals(expected, f.flags[0]);
    }

    @Test
    void failedNestedConditionsAreNotLatchedForLaterTicks() {
        Fixture f = new Fixture(255);
        f.records.put(1, PlayerBreachFixtures.condition(1, PLAYER_ENEMY_BREACHED, 2, 0, 255));
        f.records.put(2, PlayerBreachFixtures.condition(2, FLAG, -1, 0, 255));
        f.bind();
        f.scripts.onEnemyBreached((short) 3);
        assertEquals(0, f.flags[0]);
        f.flags[1] = 1;
        f.ticks(12);
        assertEquals(0, f.flags[0]);
        f.scripts.onEnemyBreached((short) 3);
        assertEquals(1, f.flags[0]);
    }

    @Test
    void nestedBreachEventsAreNotOrdinaryConditions() {
        Fixture f = new Fixture(255);
        f.records.put(1, PlayerBreachFixtures.condition(1, FLAG, 2, 0, 255));
        f.records.put(2, PlayerBreachFixtures.condition(2, PLAYER_ENEMY_BREACHED, -1, 0, 255));
        f.bind();
        f.flags[1] = 1;
        f.scripts.onEnemyBreached((short) 3);
        f.ticks(12);
        assertEquals(0, f.flags[0]);
    }

    @Test
    void allMatchingRootsRunOnOneBreach() {
        Fixture f = new Fixture(255);
        f.records.put(1, PlayerBreachFixtures.condition(1, PLAYER_ENEMY_BREACHED, -1, 2, 255));
        f.records.put(2, PlayerBreachFixtures.condition(2, PLAYER_ENEMY_BREACHED, -2, 0, 255));
        f.records.put(-2, PlayerBreachFixtures.action(-2, 1, 0));
        f.bind();
        f.scripts.onEnemyBreached((short) 3);
        assertEquals(1, f.flags[0]);
        assertEquals(1, f.flags[1]);
    }

    @Test
    void childActionsAndConditionsRunInScriptOrder() {
        Fixture f = new Fixture(255);
        f.records.put(1, PlayerBreachFixtures.condition(1, PLAYER_ENEMY_BREACHED, -2, 0, 255));
        f.records.put(-2, PlayerBreachFixtures.action(-2, 1, 2));
        f.records.put(2, PlayerBreachFixtures.condition(2, FLAG, -1, 0, 255));
        f.bind();
        f.scripts.onEnemyBreached((short) 3);
        assertEquals(1, f.flags[0]);
    }

    @Test
    void dispatchPreservesAPendingOrdinaryBranch() {
        Fixture f = new Fixture(255);
        f.records.put(1, PlayerBreachFixtures.condition(1, FLAG, -2, 2, 255));
        f.records.put(-2, PlayerBreachFixtures.action(-2, 1, 0));
        f.records.put(2, PlayerBreachFixtures.condition(2, PLAYER_ENEMY_BREACHED, -1, 0, 255));
        f.bind();
        f.flags[1] = 1;
        f.ticks(1); // Select the ordinary branch, leaving its action pending.
        f.scripts.onEnemyBreached((short) 3);
        assertEquals(1, f.flags[0]);
        assertEquals(1, f.flags[1]);
        f.ticks(1);
        assertEquals(2, f.flags[1]);
        assertEquals(1, f.flags[0]);
    }

    @Test
    void otherEventTypesAndPlayersWithoutScriptsAreIgnored() {
        Fixture f = new Fixture(255);
        f.records.put(1, PlayerBreachFixtures.condition(1, PLAYER_CREATURE_SACKED, -1, 0, 255));
        f.bind();
        f.scripts.onEnemyBreached((short) 3);
        f.scripts.onEnemyBreached((short) 7);
        assertEquals(0, f.flags[0]);
    }

    private static final class Fixture {
        final int[] flags = new int[3];
        final Map<Integer, Trigger> records = new HashMap<>();
        final IKwdFile kwd;
        final ILevelInfo level;
        PlayerTriggerLogicController scripts;

        Fixture(int repeat) {
            records.put(1, PlayerBreachFixtures.condition(1, PLAYER_ENEMY_BREACHED, -1, 0, repeat));
            records.put(-1, PlayerBreachFixtures.action(-1, 0, 0));
            records.put(4, PlayerBreachFixtures.condition(4, PLAYER_ENEMY_BREACHED, -4, 0, 255));
            records.put(-4, PlayerBreachFixtures.action(-4, 2, 0));
            kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
                case "getTriggers" -> records;
                case "getPlayers" -> Map.of((short) 3, PlayerBreachFixtures.player((short) 3, 1),
                        (short) 4, PlayerBreachFixtures.player((short) 4, 4));
                default -> throw new UnsupportedOperationException(method);
            });
            level = proxy(ILevelInfo.class, (method, args) -> switch (method) {
                case "getLevelData" -> kwd;
                case "getPlayers" -> Map.of((short) 3, new Keeper(false, (short) 3),
                        (short) 4, new Keeper(false, (short) 4));
                case "getFlag" -> flags[((Number) args[0]).intValue()];
                case "setFlag" -> { flags[((Number) args[0]).intValue()] = (int) args[1]; yield null; }
                default -> throw new UnsupportedOperationException(method);
            });
            bind();
        }

        void bind() {
            scripts = new PlayerTriggerLogicController(null, level, null, null, null, null);
        }

        void ticks(int count) {
            for (int i = 0; i < count; i++) {
                scripts.processTick(.25f);
            }
        }
    }

    @FunctionalInterface
    private interface Call {
        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (p, method, args) -> call.invoke(method.getName(), args)));
    }
}
