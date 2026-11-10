/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.map.MapColourGrid;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.fogofwar.IFogOfWarInformation;

/**
 * Applies selected and pending-tag highlights to the minimap colour grid.
 */
final class MinimapSelectionHighlighter {

    private MinimapSelectionHighlighter() {
    }

    static <T extends IMapTileInformation> void apply(MapColourGrid grid,
            IMapInformation<T> mapInformation, IFogOfWarInformation fogOfWarInformation,
            short playerId) {
        IMapDataInformation<T> mapData = mapInformation.getMapData();
        int width = grid.getWidth();
        int height = grid.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                T tile = mapData.getTile(x, y);
                if (tile == null) {
                    Point point = new Point(x, y);
                    if (mapInformation.isSelected(point, playerId)
                            || fogOfWarInformation.isPendingTagged(point)) {
                        grid.setHighlight(x, y, true);
                    }
                } else if (mapInformation.isSelected(tile, playerId)
                        || fogOfWarInformation.isPendingTagged(tile)) {
                    grid.setHighlight(x, y, true, tile);
                }
            }
        }
    }
}
