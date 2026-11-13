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
package toniarts.openkeeper.game.fogofwar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SightOfEvilRevealTest {

    private static final int DURATION = 64;

    private final SightOfEvilReveal reveal = new SightOfEvilReveal(20, 20, 7, DURATION, 40, 40);

    @Test
    void nothingIsRevealedAtTheStart() {
        assertFalse(reveal.isRevealed(20, 20, 0));
    }

    @Test
    void discGrowsToFiveTilesOverTheFirstTicks() {

        // Half way through the growth (24 ticks for a 64 tick spell) the disc is 2.5 tiles
        assertTrue(reveal.isRevealed(20, 20, 12));
        assertTrue(reveal.isRevealed(22, 20, 12));
        assertTrue(reveal.isRevealed(22, 21, 12));
        assertFalse(reveal.isRevealed(23, 20, 12));

        // Fully grown it is 5 tiles, minus the faded edge
        assertTrue(reveal.isRevealed(24, 20, 24));
        assertTrue(reveal.isRevealed(17, 17, 24));
        assertFalse(reveal.isRevealed(25, 20, 24));
        assertFalse(reveal.isRevealed(24, 23, 24));
    }

    @Test
    void discStaysFullSizeThenShrinksAway() {
        assertTrue(reveal.isRevealed(24, 20, 32));
        assertTrue(reveal.isRevealed(24, 20, 40));
        assertTrue(reveal.isRevealed(22, 20, 52));
        assertFalse(reveal.isRevealed(23, 20, 52));
        assertFalse(reveal.isRevealed(20, 20, DURATION));
        assertTrue(reveal.isOver(DURATION));
        assertFalse(reveal.isOver(DURATION - 1));
    }

    @Test
    void areaIsTheRadiusAroundTheCastTileWithTheFarEdgesLeftOut() {
        SightOfEvilReveal small = new SightOfEvilReveal(20, 20, 2, DURATION, 40, 40);

        assertEquals(18, small.getMinX());
        assertEquals(18, small.getMinY());
        assertEquals(22, small.getMaxX());
        assertEquals(22, small.getMaxY());
        assertTrue(small.isRevealed(18, 20, 24));
        assertFalse(small.isRevealed(22, 20, 24));
        assertFalse(small.isRevealed(20, 22, 24));
    }

    @Test
    void radiusIsCappedAtSevenTiles() {
        SightOfEvilReveal big = new SightOfEvilReveal(20, 20, 9, DURATION, 40, 40);

        assertEquals(13, big.getMinX());
        assertEquals(27, big.getMaxX());
    }

    @Test
    void nearTheMapEdgeTheDiscIsCentredOnTheClippedArea() {

        // The area is clipped to x 0..5, so the disc is centred on x 3, not on the cast tile
        SightOfEvilReveal edge = new SightOfEvilReveal(1, 20, 5, DURATION, 40, 40);

        assertEquals(0, edge.getMinX());
        assertEquals(6, edge.getMaxX());
        assertTrue(edge.isRevealed(1, 20, 12));
        assertFalse(edge.isRevealed(0, 20, 12));
    }

    @Test
    void lastColumnAndRowOfTheMapCanBeRevealed() {
        SightOfEvilReveal corner = new SightOfEvilReveal(38, 38, 5, DURATION, 40, 40);

        assertEquals(40, corner.getMaxX());
        assertEquals(40, corner.getMaxY());
        assertTrue(corner.isRevealed(39, 38, 24));
        assertTrue(corner.isRevealed(38, 39, 24));
        assertTrue(corner.isRevealed(39, 39, 24));
    }

    @Test
    void zeroRadiusRevealsNothing() {
        SightOfEvilReveal none = new SightOfEvilReveal(20, 20, 0, DURATION, 40, 40);

        assertFalse(none.isRevealed(20, 20, 24));
    }

}
