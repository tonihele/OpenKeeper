/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ClearanceFieldTest {

    @Test
    void mapPerimeterConstrainsTheCeilingEvenWhenTheOuterTilesAreOpen() {
        int size = 9;
        ClearanceField field = new ClearanceField(size, size, new boolean[size * size]);

        assertEquals(0f, field.clearanceAt(-0.5f, 4f), 1e-6f, "the west map edge");
        assertEquals(2f, field.clearanceAt(0f, 4f), 1e-6f, "half a tile from the west map edge");
        assertEquals(ClearanceField.MAX_CLEARANCE, field.clearanceAt(4f, 4f), 1e-6f,
                "the center of a large open map retains a full ceiling dome");
    }

    @Test
    void nearbySolidTerrainStillSetsTheLowerCeilingLimit() {
        int size = 9;
        boolean[] solid = new boolean[size * size];
        solid[4 * size + 1] = true;
        ClearanceField field = new ClearanceField(size, size, solid);

        assertEquals(2f, field.clearanceAt(2f, 4f), 1e-6f,
                "nearby terrain remains the closest ceiling support");
    }
}
