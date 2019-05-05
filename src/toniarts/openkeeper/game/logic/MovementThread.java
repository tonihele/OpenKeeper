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
import toniarts.openkeeper.world.ThingLoader;
import toniarts.openkeeper.world.creature.steering.AbstractCreatureSteeringControl;

/**
 * Runs the creature movement. Implements runnable, so supports running from a
 * thread.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MovementThread implements Runnable {

    private final float tpf;
    private final Application app;
    private final ThingLoader thingLoader;
    private static final Logger LOGGER = Logger.getLogger(MovementThread.class.getName());

    public MovementThread(Application app, float tpf, ThingLoader thingLoader) {
        this.app = app;
        this.tpf = tpf;
        this.thingLoader = thingLoader;
    }

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();

            // Update movement
            for (AbstractCreatureSteeringControl steerable : thingLoader.getCreatures()) {
                steerable.processSteeringTick(tpf, app);
            }

            // Logging
            long tickTime = System.currentTimeMillis() - start;
            LOGGER.log(tickTime < tpf * 1000 ? Level.FINEST : Level.SEVERE, "Movement took {0}ms!", tickTime);
        } catch (Exception e) {
            LOGGER.severe(e.getLocalizedMessage());
        }
    }

}
