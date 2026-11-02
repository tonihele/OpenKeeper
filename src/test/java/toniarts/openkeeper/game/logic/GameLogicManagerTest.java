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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A player action from another thread never runs in the middle of a game
 * logic tick
 */
public class GameLogicManagerTest {

    @Test
    public void playerActionWaitsForTheRunningTick() throws Exception {
        CountDownLatch tickStarted = new CountDownLatch(1);
        AtomicBoolean tickDone = new AtomicBoolean();
        GameLogicManager manager = new GameLogicManager(new IGameLogicUpdatable() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void processTick(float tpf) {
                tickStarted.countDown();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                tickDone.set(true);
            }
        });

        Thread logicThread = new Thread(() -> manager.processTick(0.25f));
        logicThread.start();
        assertTrue(tickStarted.await(5, TimeUnit.SECONDS));

        AtomicBoolean ranAfterTick = new AtomicBoolean();
        manager.runBetweenTicks(() -> ranAfterTick.set(tickDone.get()));
        logicThread.join();

        assertTrue(ranAfterTick.get());
    }
}
