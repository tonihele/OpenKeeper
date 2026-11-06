/*
 * Copyright (C) 2014-2025 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import toniarts.openkeeper.utils.GameTimeCounter;

/**
 *
 * @author ArchDemon
 */
public final class GameTimeController extends GameTimeCounter implements IGameTimer {

    private final double startTime;
    private long ticks;

    /**
     * A game timer that starts from zero, for a new game
     */
    public GameTimeController() {
        this(0.0);
    }

    /**
     * A game timer that starts from the given time, the time of a loaded game
     *
     * @param startTime the game time to start from, in seconds
     */
    public GameTimeController(double startTime) {
        this.startTime = startTime;
        timeElapsed = startTime;
    }

    @Override
    public void start() {
        ticks = 0;
        timeElapsed = startTime;
    }

    @Override
    public void stop() {
        // nope
    }

    @Override
    public void processTick(float tpf) {
        super.processTick(tpf);
        ticks++;
    }

    @Override
    public double getGameTime() {
        return timeElapsed;
    }
}
