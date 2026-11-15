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
package toniarts.openkeeper.game.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManufactureProgressTest {

    @Test
    void idleProgressHasNoFraction() {
        assertEquals(0f, ManufactureProgress.IDLE.fraction());
    }

    @Test
    void fractionReflectsWorkCompleted() {
        assertEquals(0.25f, new ManufactureProgress(25, 100).fraction());
    }

    @Test
    void fractionIsClampedToValidRange() {
        assertEquals(0f, new ManufactureProgress(-1, 100).fraction());
        assertEquals(1f, new ManufactureProgress(101, 100).fraction());
        assertEquals(0f, new ManufactureProgress(10, 0).fraction());
    }
}
