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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManufactureOrdersTest {

    private static final short SENTRY = 3;
    private static final short FEAR = 8;
    private static final short BOULDER = 2;

    @Test
    void oldestBlueprintFirst() {
        assertEquals(FEAR, ManufactureOrders.next(List.of(FEAR, SENTRY, BOULDER), Map.of()));
    }

    @Test
    void crateFulfillsTheOldestOrderOfItsType() {

        // A fear trap crate exists already, next is the sentry
        assertEquals(SENTRY, ManufactureOrders.next(List.of(FEAR, SENTRY, FEAR), Map.of(FEAR, 1)));

        // Two sentries placed, one crate: the second sentry still needs one, before the boulder
        assertEquals(SENTRY, ManufactureOrders.next(List.of(SENTRY, SENTRY, BOULDER), Map.of(SENTRY, 1)));
    }

    @Test
    void cratesOfOtherTypesDontCount() {
        assertEquals(SENTRY, ManufactureOrders.next(List.of(SENTRY), Map.of(BOULDER, 3)));
    }

    @Test
    void nothingToDoWhenEveryBlueprintHasItsCrate() {
        assertNull(ManufactureOrders.next(List.of(SENTRY, FEAR), Map.of(SENTRY, 1, FEAR, 2)));
        assertNull(ManufactureOrders.next(List.of(), Map.of()));
    }

}
