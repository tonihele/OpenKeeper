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

import toniarts.openkeeper.game.logic.IGameLogicUpdatable;

/**
 * Simple interface for enabling game logic update
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
abstract public class GameTimeCounter implements IGameLogicUpdatable {

    protected double timeElapsed = 0.0;

    /**
     * Goes on counting from the given time. A loaded game goes on from the
     * game time it was saved at, and the times in its components are from
     * that clock.
     *
     * @param time the time to count from, in seconds
     */
    public void setTimeElapsed(double time) {
        timeElapsed = time;
    }

    @Override
    public void processTick(float tpf) {
        timeElapsed += tpf;
    }

}
