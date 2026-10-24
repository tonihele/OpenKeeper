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
package toniarts.openkeeper.game.controller.creature;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature that finds no way to a place doesn't look for one again at once
 */
class PathRetryBackoffTest {

    private static final Point GOAL = new Point(13, 44);
    private static final Point OTHER_GOAL = new Point(42, 44);

    private final PathRetryBackoff backoff = new PathRetryBackoff();

    @Test
    void firstSearchIsAlwaysAllowed() {
        assertFalse(backoff.isWaiting(GOAL, 100.0));
    }

    @Test
    void failedGoalIsNotSearchedAgainAtOnce() {
        backoff.failed(GOAL, 100.0);

        assertTrue(backoff.isWaiting(GOAL, 100.25));
        assertTrue(backoff.isWaiting(GOAL, 100.0 + PathRetryBackoff.RETRY_DELAY - 0.01));
    }

    @Test
    void failedGoalIsSearchedAgainAfterTheDelay() {
        backoff.failed(GOAL, 100.0);

        assertFalse(backoff.isWaiting(GOAL, 100.0 + PathRetryBackoff.RETRY_DELAY));
    }

    @Test
    void otherGoalsAreNotHeldBack() {
        backoff.failed(GOAL, 100.0);

        assertFalse(backoff.isWaiting(OTHER_GOAL, 100.25));
    }

    @Test
    void foundWayClearsTheWait() {
        backoff.failed(GOAL, 100.0);
        backoff.succeeded();

        assertFalse(backoff.isWaiting(GOAL, 100.25));
    }

}
