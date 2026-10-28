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

/**
 * Creature attributes are scaled by the level percentages of the creature
 * stats (e.g. health 125% on level 2). The percentage must not be truncated
 * to a whole multiplier.
 */
class CreatureLevelScalingTest {

    @Test
    void levelOneKeepsTheRecordValue() {
        assertEquals(1000, CreaturesController.scaleByLevel(1000, 100));
    }

    @Test
    void fractionalPercentagesAreNotTruncated() {
        assertEquals(1250, CreaturesController.scaleByLevel(1000, 125));
        assertEquals(1500, CreaturesController.scaleByLevel(1000, 150));
        assertEquals(1750, CreaturesController.scaleByLevel(1000, 175));
        assertEquals(2500, CreaturesController.scaleByLevel(1000, 250));
    }

    @Test
    void percentagesBelowHundredDoNotDropToZero() {
        assertEquals(80, CreaturesController.scaleByLevel(100, 80));
    }

    @Test
    void integerResultIsTruncatedAfterMultiplying() {
        assertEquals(43, CreaturesController.scaleByLevel(35, 125));
    }

    @Test
    void floatAttributesAreScaled() {
        assertEquals(1.5f, CreaturesController.scaleByLevel(1.0f, 150), 0.0001f);
        assertEquals(0.75f, CreaturesController.scaleByLevel(1.0f, 75), 0.0001f);
    }
}
