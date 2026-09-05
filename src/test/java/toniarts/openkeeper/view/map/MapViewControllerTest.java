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
import toniarts.openkeeper.view.map.WallSection.WallDirection;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapViewControllerTest {

    @Test
    void evenXAndOddYDoNotAllowATorch() {
        assertEquals(List.of(), MapViewController.getTorchDirections(2, 3));
    }

    @Test
    void oddXAndOddYAllowNorthAndSouthWalls() {
        assertEquals(List.of(WallDirection.NORTH, WallDirection.SOUTH),
                MapViewController.getTorchDirections(3, 3));
    }

    @Test
    void evenXAndEvenYAllowWestAndEastWalls() {
        assertEquals(List.of(WallDirection.WEST, WallDirection.EAST),
                MapViewController.getTorchDirections(2, 4));
    }

    @Test
    void oddXAndEvenYAllowAllWallsInPriorityOrder() {
        assertEquals(List.of(WallDirection.NORTH, WallDirection.WEST, WallDirection.SOUTH, WallDirection.EAST),
                MapViewController.getTorchDirections(3, 4));
    }
}
