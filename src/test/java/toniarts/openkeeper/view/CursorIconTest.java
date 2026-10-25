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
package toniarts.openkeeper.view;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The icons the original shows next to the hand (DKII.EXE 0x40CAE0)
 */
class CursorIconTest {

    @Test
    void buildingAndSellingHaveTheirIcons() {
        assertEquals(CursorIcon.Icon.PLACE_ROOM, CursorIcon.getIcon(Type.ROOM));
        assertEquals(CursorIcon.Icon.PLACE_DOOR, CursorIcon.getIcon(Type.DOOR));
        assertEquals(CursorIcon.Icon.PLACE_TRAP, CursorIcon.getIcon(Type.TRAP));
        assertEquals(CursorIcon.Icon.SELL, CursorIcon.getIcon(Type.SELL));
    }

    @Test
    void thePlainHandHasNoIcon() {

        // Digging included, the original never draws its dig icon next to the hand
        assertNull(CursorIcon.getIcon(Type.NONE));
        assertNull(CursorIcon.getIcon(Type.STUFF_IN_HAND));
        assertNull(CursorIcon.getIcon(Type.SPELL));
    }

    @Test
    void costIsLightRedIfTheKeeperCanAffordIt() {
        assertEquals(new CursorIcon.Price(900, CursorIcon.PriceKind.AFFORDABLE), CursorIcon.Price.cost(900, 900));
        assertEquals(new CursorIcon.Price(900, CursorIcon.PriceKind.TOO_EXPENSIVE), CursorIcon.Price.cost(900, 899));
    }

    @Test
    void nothingToPayOrToSellShowsNoPrice() {
        assertNull(CursorIcon.Price.cost(0, 1000));
        assertNull(CursorIcon.Price.sale(0));
    }

    @Test
    void saleShowsWhatSellingGivesBack() {
        assertEquals(new CursorIcon.Price(375, CursorIcon.PriceKind.SALE), CursorIcon.Price.sale(375));
    }

}
