/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Pauseable single thread executor. Copied from:
 * https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html.
 * FIXME: This solution is not working, since after pause it runs to execute the
 * delayed stuff
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PauseableScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private boolean paused;
    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition unpaused = pauseLock.newCondition();

    /**
     * Creates an executor with two threads
     *
     * @param corePoolSize the initial number of threads
     * @param paused start as paused
     */
    public PauseableScheduledThreadPoolExecutor(int corePoolSize, boolean paused) {
        super(corePoolSize);

        if (paused) {
            pause();
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        pauseLock.lock();
        try {
            while (paused) {
                unpaused.await();
            }
        } catch (InterruptedException ie) {
            t.interrupt();
        } finally {
            pauseLock.unlock();
        }
    }

    public final void pause() {
        pauseLock.lock();
        try {
            paused = true;
        } finally {
            pauseLock.unlock();
        }
    }

    public final void resume() {
        pauseLock.lock();
        try {
            paused = false;
            unpaused.signalAll();
        } finally {
            pauseLock.unlock();
        }
    }
}
