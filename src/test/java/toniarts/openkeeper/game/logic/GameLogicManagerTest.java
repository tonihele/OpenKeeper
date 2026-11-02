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

/**
 * Queued player actions run on the logic thread at the start of a tick, never
 * in the middle of one
 */
public class GameLogicManagerTest {

    private final List<String> events = new ArrayList<>();

    @Test
    public void actionQueuedDuringTickRunsAtStartOfNextTick() {
        GameLogicManager[] manager = new GameLogicManager[1];
        manager[0] = new GameLogicManager(new IGameLogicUpdatable() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void processTick(float tpf) {
                events.add("tick");
                if (events.size() == 1) {
                    manager[0].enqueueAction(() -> events.add("action"));
                }
            }
        });

        manager[0].processTick(0.25f);
        assertEquals(List.of("tick"), events);

        manager[0].processTick(0.25f);
        assertEquals(List.of("tick", "action", "tick"), events);
    }

    @Test
    public void actionQueuedWhileDrainingWaitsForTheFollowingTick() {
        GameLogicManager manager = new GameLogicManager();
        manager.enqueueAction(() -> {
            events.add("first");
            manager.enqueueAction(() -> events.add("second"));
        });

        manager.processTick(0.25f);
        assertEquals(List.of("first"), events);

        manager.processTick(0.25f);
        assertEquals(List.of("first", "second"), events);
    }
}
