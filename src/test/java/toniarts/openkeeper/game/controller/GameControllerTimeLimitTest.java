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
package toniarts.openkeeper.game.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The level time limit counts down and rests on zero. Running out of it is
 * noticed once, the level script decides what it means
 */
class GameControllerTimeLimitTest {

    @Test
    void timeLimitCountsDown() {
        assertEquals(9.75f, GameController.advanceTimeLimit(10f, 0.25f), 0.0001f);
    }

    @Test
    void timeLimitRestsOnZero() {
        assertEquals(0f, GameController.advanceTimeLimit(0.1f, 0.25f), 0.0001f);
        assertEquals(0f, GameController.advanceTimeLimit(0f, 0.25f), 0.0001f);
    }

    @Test
    void runningOutIsNoticedOnlyOnce() {
        assertTrue(GameController.isTimeLimitReached(0.1f, 0f));
        assertFalse(GameController.isTimeLimitReached(0f, 0f));
    }

    @Test
    void timeLeftIsNotRunningOut() {
        assertFalse(GameController.isTimeLimitReached(10f, 9.75f));
    }

}
