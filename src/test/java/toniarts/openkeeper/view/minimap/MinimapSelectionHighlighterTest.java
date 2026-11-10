/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.FakeFogOfWarInformation;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.game.map.MapColourClass;
import toniarts.openkeeper.game.map.MapColourClassifier;
import toniarts.openkeeper.game.map.MapColourGrid;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimapSelectionHighlighterTest {

    @Test
    void appliesSelectedAndPendingHighlightsOnlyWhenFogAllowsThem() {
        FakeMapData mapData = new FakeMapData(3, 1);
        FakeMapTile selected = mapData.getTile(0, 0);
        selected.selected = true;
        Point selectedPoint = selected.getLocation();
        FakeMapTile pending = mapData.getTile(1, 0);
        Point pendingPoint = pending.getLocation();
        FakeMapTile blocked = mapData.getTile(2, 0);
        blocked.selected = true;

        FakeFogOfWarInformation fog = new FakeFogOfWarInformation();
        fog.pendingTagged.add(pendingPoint);
        fog.highlightable.add(selectedPoint);
        fog.highlightable.add(pendingPoint);
        FakeMapInformation mapInformation = new FakeMapInformation(mapData, Map.of());
        MapColourGrid grid = new MapColourGrid(3, 1,
                new MapColourClassifier(mapInformation, fog, null), fog);

        MinimapSelectionHighlighter.apply(grid, mapInformation, fog, (short) 1);

        assertEquals(MapColourClass.HIGHLIGHT, grid.get(0, 0));
        assertEquals(MapColourClass.HIGHLIGHT, grid.get(1, 0));
        assertEquals(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, grid.get(2, 0));
    }
}
