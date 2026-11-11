/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.view.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CeilingCoverageTest {

    @Test
    void ceilingCoversOpenTilesPillarTilesAndTheExposedSolidRim() {
        assertTrue(MapViewController.shouldBuildCeilingPatch(false, false, true, false), "open tile");
        assertTrue(MapViewController.shouldBuildCeilingPatch(false, true, true, false), "pillar tile");
        assertTrue(MapViewController.shouldBuildCeilingPatch(true, false, true, true), "solid rim");
        assertFalse(MapViewController.shouldBuildCeilingPatch(true, false, true, false), "buried solid tile");
        assertFalse(MapViewController.shouldBuildCeilingPatch(false, false, false, true), "fogged tile");
    }
}
