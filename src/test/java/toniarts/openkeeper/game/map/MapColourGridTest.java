/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.map;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

class MapColourGridTest {

    private static final short GOLD_ID = 1;
    private static final short WATER_ID = 2;

    private FakeMapData mapData;
    private FakeFogOfWarInformation fog;
    private MapColourGrid grid;

    @BeforeEach
    void setUp() {
        mapData = new FakeMapData(5, 5);

        Map<Short, Terrain> terrains = new HashMap<>();
        terrains.put(GOLD_ID, TerrainFixtures.terrain(GOLD_ID, flags(Terrain.TerrainFlag.SOLID), 100, 0, 0, GOLD_ID));
        terrains.put(WATER_ID, TerrainFixtures.terrain(WATER_ID, flags(Terrain.TerrainFlag.WATER), 0, 0, 0, WATER_ID));

        FakeMapInformation mapInformation = new FakeMapInformation(mapData, terrains);
        fog = new FakeFogOfWarInformation();
        FakeRoomsInformation rooms = new FakeRoomsInformation();
        MapColourClassifier classifier = new MapColourClassifier(mapInformation, fog, rooms);
        grid = new MapColourGrid(5, 5, classifier, fog);

        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                mapData.getTile(x, y).terrainId = GOLD_ID;
            }
        }
    }

    @Test
    void freshGridIsAllUnexploredBeforeAnyRecompute() {
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(2, 2));
    }

    @Test
    void getOutOfBoundsReturnsUnexploredRatherThanThrowing() {
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(-1, 0));
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(0, 100));
    }

    @Test
    void recomputeStoresTheClassifiedValueForExactlyThatTile() {
        fog.explored.add(new Point(2, 2));

        grid.recompute(2, 2);

        assertEquals(MapColourClass.GOLD, grid.get(2, 2));
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(2, 3));
    }

    @Test
    void recomputeOutOfBoundsIsANoOp() {
        grid.recompute(-1, -1);
        grid.recompute(99, 99);
        // No exception, nothing to assert beyond "didn't throw".
    }

    @Test
    void recomputeRectFillsExactlyTheClampedRectangle() {
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                fog.explored.add(new Point(x, y));
            }
        }

        grid.recomputeRect(-2, -2, 5, 5); // clamps to [0,3) x [0,3)

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                assertEquals(MapColourClass.GOLD, grid.get(x, y), "(" + x + "," + y + ")");
            }
        }
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(3, 3));
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(4, 4));
    }

    @Test
    void setHighlightOnStoresHighlightOnlyWhenTheFogRuleAllowsIt() {
        fog.highlightable.add(new Point(1, 1));
        // (2,2) is deliberately left out of `highlightable`.

        grid.setHighlight(1, 1, true);
        grid.setHighlight(2, 2, true);

        assertEquals(MapColourClass.HIGHLIGHT, grid.get(1, 1));
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(2, 2));
    }

    @Test
    void setHighlightOffRecomputesTheTileInstead() {
        fog.explored.add(new Point(1, 1));
        fog.highlightable.add(new Point(1, 1));
        grid.setHighlight(1, 1, true);
        assertEquals(MapColourClass.HIGHLIGHT, grid.get(1, 1));

        grid.setHighlight(1, 1, false);

        assertEquals(MapColourClass.GOLD, grid.get(1, 1));
    }

    @Test
    void colourClassesReflectsWhatRecomputeStored() {
        fog.explored.add(new Point(0, 0));
        grid.recompute(0, 0);

        assertEquals(MapColourClass.GOLD, grid.colourClasses()[0]);
    }

    @Test
    void classifyIsPureAndDoesNotStore() {
        fog.explored.add(new Point(1, 1));

        short classified = grid.classify(1, 1);

        assertEquals(MapColourClass.GOLD, classified);
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(1, 1));
    }

}
