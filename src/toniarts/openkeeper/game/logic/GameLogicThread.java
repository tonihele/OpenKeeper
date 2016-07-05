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
package toniarts.openkeeper.game.logic;

import com.jme3.app.Application;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs the game logic. Implements runnable, so supports running from a thread.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class GameLogicThread implements Runnable {

    private final float tpf;
    private final IGameLogicUpdateable[] updatables;
    private long ticks = 0;
    private final Application app;
    private static final Logger logger = Logger.getLogger(GameLogicThread.class.getName());

    public GameLogicThread(Application app, float tpf, IGameLogicUpdateable... updatables) {
        this.app = app;
        this.tpf = tpf;
        this.updatables = updatables;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();

        // Update updatables
        for (IGameLogicUpdateable updatable : updatables) {
            updatable.processTick(tpf, app);
        }

        // Increase ticks
        ticks++;

        // Logging
        long tickTime = System.currentTimeMillis() - start;
        logger.log(tickTime < tpf * 1000 ? Level.FINEST : Level.SEVERE, "Tick took {0}ms!", tickTime);
    }

    /**
     * Get the elapsed game time, in seconds
     *
     * @return the game time
     */
    public double getGameTime() {
        return ticks * tpf;
    }

}
