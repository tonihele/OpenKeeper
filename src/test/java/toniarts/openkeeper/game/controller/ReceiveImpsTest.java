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

import com.jme3.math.Vector2f;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Receive Imps special puts its imps in a ring round the Dungeon Heart, as DK II does (0x4A4980)
 */
class ReceiveImpsTest {

    private static final Point HEART = new Point(10, 20);

    @Test
    void impsStandTwoTilesFromTheMiddleOfTheHeart() {
        for (int i = 0; i < 10; i++) {
            Vector2f spot = GameWorldController.getReceivedImpSpot(HEART, i, 10);

            assertEquals(2f, spot.distance(new Vector2f(HEART.x, HEART.y)), 0.0001f);
        }
    }

    @Test
    void firstImpStandsNorthAndTheRestGoRoundInWholeSteps() {
        assertSpot(10, 18, GameWorldController.getReceivedImpSpot(HEART, 0, 4));
        assertSpot(12, 20, GameWorldController.getReceivedImpSpot(HEART, 1, 4));
        assertSpot(10, 22, GameWorldController.getReceivedImpSpot(HEART, 2, 4));
        assertSpot(8, 20, GameWorldController.getReceivedImpSpot(HEART, 3, 4));

        // 10 imps are 204 of DK II's 2048 angle units apart, so the last one is short of the first
        Vector2f last = GameWorldController.getReceivedImpSpot(HEART, 9, 10);
        float angle = 9 * 204 * (float) Math.PI * 2 / 2048;
        assertSpot(HEART.x + 2 * (float) Math.sin(angle), HEART.y - 2 * (float) Math.cos(angle), last);
    }

    private static void assertSpot(float x, float y, Vector2f spot) {
        assertEquals(x, spot.x, 0.0001f);
        assertEquals(y, spot.y, 0.0001f);
    }
}
