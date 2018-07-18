/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.utils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A game loop. This is a fork of Paul Speeds class of a same name.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameLoop {

    public static final long INTERVAL_FPS_60 = 16666667L;
    private static final String DEFAULT_NAME = "GameLoopThread";

    private final IGameLoopManager gameLoopManager;
    private final Runner loop;

    private long updateRate;
    private long idleSleepTime = 0;
    private final AtomicBoolean pauseFlag = new AtomicBoolean(false);

    public GameLoop(IGameLoopManager gameLoopManager) {
        this(gameLoopManager, INTERVAL_FPS_60); // 60 FPS
    }

    public GameLoop(IGameLoopManager gameLoopManager, long updateRateNanos) {
        this(gameLoopManager, updateRateNanos, DEFAULT_NAME);
    }

    public GameLoop(IGameLoopManager gameLoopManager, long updateRateNanos, String name) {
        this(gameLoopManager, updateRateNanos, name, null);
    }

    public GameLoop(IGameLoopManager gameLoopManager, long updateRateNanos, String name, Long idleSleepTime) {
        this.gameLoopManager = gameLoopManager;
        this.updateRate = updateRateNanos;
        this.loop = new Runner(name);
        setIdleSleepTime(idleSleepTime);
    }

    /**
     * Starts the background game loop thread and initializes and starts the
     * game system manager (if it hasn't been initialized or started already).
     * The systems will be initialized and started on the game loop background
     * thread.
     */
    public void start() {
        loop.start();
    }

    /**
     * Stops the background game loop thread, stopping and terminating the game
     * systems. This method will wait until the thread has been fully shut down
     * before returning. The systems will be stopped and terminated on the game
     * loop background thread.
     */
    public void stop() {
        loop.close();
    }

    /**
     * Sets the period of time in milliseconds that the update loop will wait
     * between time interval checks. This defaults to 0 because on Windows the
     * default 60 FPS update rate will cause frame drops for a idle sleep time
     * of 1 or more. However, 0 causes the loop to consume a noticable amount of
     * CPU. For lower framerates, 1 is recommended and will be set automically
     * based on the current frame rate interval if null is specified.
     *
     * @param millis
     */
    public final void setIdleSleepTime(Long millis) {
        if (millis == null) {

            // Configure a reasonable default
            if (updateRate > INTERVAL_FPS_60) {

                // Can probably get away with more sleep
                this.idleSleepTime = 1;
            } else {

                // Else on Windows, sleep > 0 will take almost as long as
                // a frame
                this.idleSleepTime = 0;
            }
        } else {
            this.idleSleepTime = millis;
        }
    }

    public Long getIdleSleepTime() {
        return idleSleepTime;
    }

    public void pause() {
        pauseFlag.set(true);
    }

    public void resume() {
        pauseFlag.set(false);
        synchronized (pauseFlag) {
            pauseFlag.notify();
        }
    }

    /**
     * Use our own thread instead of a java executor because we need more
     * control over the update loop. ScheduledThreadPoolExecutor will try to
     * call makeup frames if it gets behind and we'd rather just drop them.
     * Furthermore, this allows us to 'busy wait' for the next 'frame'.
     */
    protected class Runner extends Thread {

        private final AtomicBoolean go = new AtomicBoolean(true);

        public Runner(String name) {
            super(name);
        }

        public void close() {
            go.set(false);
            GameLoop.this.resume();
            try {
                join();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for game loop thread to complete.", e);
            }
        }

        @Override
        public void run() {
            gameLoopManager.start();

            long lastTime = System.nanoTime();
            gameLoop:
            while (go.get()) {

                // Check pause
                if (pauseFlag.get()) {
                    synchronized (pauseFlag) {

                        // We are in a while loop here to protect against spurious interrupts
                        while (pauseFlag.get()) {
                            try {
                                pauseFlag.wait();
                                lastTime = System.nanoTime();
                                continue gameLoop;
                            } catch (InterruptedException e) {
                                throw new RuntimeException("Interrupted sleeping", e);
                            }
                        }
                    }
                }

                long time = System.nanoTime();
                long delta = time - lastTime;

                if (delta >= updateRate) {

                    // Time to update
                    lastTime = time;
                    gameLoopManager.processTick(delta);
                    continue;
                }

                // Wait just a little.  This is an important enough thread
                // that we'll poll instead of smart-sleep but we also don't
                // want to consume 100% of a CPU core.
                // Thread.sleep(0) relies on the operating systems thread
                // scheduler which will have a minimum granularity.  On Windows,
                // Thread.sleep(0) can take as long as 15 ms to return.  Thus
                // note that if we devolve into a case where the delta is more
                // than the update rate then we never hit this code.
                //
                // And if the delta since last poll was kind of large
                // we won't sleep at all... In this case, if the delta was
                // more than half the updateRate... we'll skip sleeping.
                // Which probably means that on Windows, we'll be responsive
                // every other loop iteration.  It's an imperfect world and
                // the only alternative is true CPU heating busy-waiting.
                if (delta * 2 > updateRate) {
                    continue;
                }
                try {

                    // More then 0 here and we underflow constantly.
                    // 0 and we gobble up noticable CPU.  Slower update
                    // rates could probably get away with the longer sleep
                    // but 60 FPS can't keep up on Windows.  So we'll let
                    // the sleep time be externally defined for cases
                    // where the caller knows  better.
                    Thread.sleep(idleSleepTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted sleeping", e);
                }
            }

            gameLoopManager.stop();
        }
    }
}
