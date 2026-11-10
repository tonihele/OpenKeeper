/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.logic;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLogicManagerTest {

    @Test
    void actionQueuedDuringTickRunsAtStartOfNextTick() {
        List<String> events = new ArrayList<>();
        GameLogicManager[] manager = new GameLogicManager[1];
        manager[0] = new GameLogicManager(new IGameLogicUpdatable() {
            @Override
            public void start() {
            }

            @Override
            public void processTick(float tpf) {
                events.add("tick");
                if (events.size() == 1) {
                    manager[0].enqueueAction(() -> events.add("action"));
                }
            }

            @Override
            public void stop() {
            }
        });

        manager[0].processTick(1f / 20f);
        assertEquals(List.of("tick"), events);

        manager[0].processTick(1f / 20f);
        assertEquals(List.of("tick", "action", "tick"), events);
    }

    @Test
    void inputsFromOnePlayerCoalesceToTheLatestValue() {
        List<String> events = new ArrayList<>();
        GameLogicManager manager = new GameLogicManager();
        manager.enqueueLatestInput((short) 1, () -> events.add("old"));
        manager.enqueueLatestInput((short) 1, () -> events.add("latest"));

        manager.processTick(1f / 20f);

        assertEquals(List.of("latest"), events);
    }

    @Test
    void simultaneousPlayerInputsUseStablePlayerIdOrder() {
        List<Short> applied = new ArrayList<>();
        GameLogicManager manager = new GameLogicManager();
        manager.enqueueLatestInput((short) 4, () -> applied.add((short) 4));
        manager.enqueueLatestInput((short) 1, () -> applied.add((short) 1));

        manager.processTick(1f / 20f);

        assertEquals(List.of((short) 1, (short) 4), applied);
    }

    @Test
    void inputEnqueuedWhileDrainingWaitsForTheFollowingTick() {
        List<String> events = new ArrayList<>();
        GameLogicManager manager = new GameLogicManager();
        manager.enqueueAction(() -> {
            events.add("action");
            manager.enqueueLatestInput((short) 1, () -> events.add("input"));
        });

        manager.processTick(1f / 20f);
        assertEquals(List.of("action"), events);

        manager.processTick(1f / 20f);
        assertEquals(List.of("action", "input"), events);
    }

    @Test
    void actionQueueRejectsExcessRequestsWithoutBlocking() {
        GameLogicManager manager = new GameLogicManager();
        for (int i = 0; i < 1024; i++) {
            assertTrue(manager.enqueueAction(() -> { }));
        }

        assertFalse(manager.enqueueAction(() -> { }));
    }
}
