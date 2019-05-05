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

/**
 * Simple interface for enabling game logic update
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IGameLogicUpdatable {

    /**
     * Signals start for the updatable
     */
    public void start();

    /**
     * Signals stop to the updatable
     */
    public void stop();

    /**
     * Process one game tick. Note that this is not likely run from a render
     * loop. So you can't modify the scene from here.
     *
     * @param tpf time since the last call to update(), in seconds. Our tick
     * rate.
     * @param gameTime elapsed game time
     */
    public void processTick(float tpf, double gameTime);

}
