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

import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.utils.IGameLoopManager;

/**
 * Skeleton for logic manager
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractLogicManager implements IGameLoopManager {

    protected final IGameLogicUpdatable[] updatables;
    private static final Logger LOGGER = Logger.getLogger(AbstractLogicManager.class.getName());

    public AbstractLogicManager(IGameLogicUpdatable... updatables) {
        this.updatables = updatables;
    }

    @Override
    public void start() {
        for (IGameLogicUpdatable updatable : updatables) {
            updatable.start();
        }
    }

    @Override
    public void processTick(long delta) {
        long start = System.nanoTime();

        // Update updatables
        float tpf = delta / 1000000000f;
        for (IGameLogicUpdatable updatable : updatables) {
            try {
                updatable.processTick(tpf);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in game logic tick on " + updatable.getClass() + "!", e);
            }
        }

        // Logging
        long tickTime = System.nanoTime() - start;
        LOGGER.log(tickTime < delta ? Level.FINEST : Level.SEVERE, "Tick took {0} ms!", tickTime);
    }

    @Override
    public void stop() {
        for (IGameLogicUpdatable updatable : updatables) {
            updatable.stop();
        }
    }

}
