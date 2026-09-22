/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.map;

import java.util.List;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

/**
 * A simple grid-backed {@link IMapDataInformation} test double.
 */
final class FakeMapData implements IMapDataInformation<FakeMapTile> {

    private final int width;
    private final int height;
    private final FakeMapTile[][] tiles;

    FakeMapData(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new FakeMapTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                FakeMapTile tile = new FakeMapTile(new Point(x, y));
                // Every real, unclaimed tile is owned by the neutral player,
                // not player id 0 - match that default here.
                tile.ownerId = Player.NEUTRAL_PLAYER_ID;
                tiles[x][y] = tile;
            }
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public FakeMapTile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        return tiles[x][y];
    }

    @Override
    public void setTiles(List<FakeMapTile> mapTiles) {
        // Unused by the tests using this fake.
    }

}
