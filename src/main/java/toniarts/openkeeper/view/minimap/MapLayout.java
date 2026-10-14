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
package toniarts.openkeeper.view.minimap;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.utils.Point;

/**
 * Where the full-screen map and its preview window sit on the screen, and
 * which tile is under a screen position. Drawing and picking both use this
 * class, so they cannot disagree. The layout is authored for 640x480 and
 * fitted (not stretched) into the real screen, centred. Coordinates are
 * top-left origin, Y down.
 */
public final class MapLayout {

    private static final float REFERENCE_WIDTH = 640;
    private static final float REFERENCE_HEIGHT = 480;

    /**
     * A rectangle on the screen
     */
    public record Rect(float x, float y, float w, float h) {

    }

    private final int screenWidth;
    private final int screenHeight;
    private final float scale;
    private final Rect map;
    private final Rect preview;

    public MapLayout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        scale = Math.min(screenWidth / REFERENCE_WIDTH, screenHeight / REFERENCE_HEIGHT);
        map = fromReference(41, 33, 384, 384);
        preview = fromReference(466, 33, 128, 192);
    }

    private Rect fromReference(float x, float y, float w, float h) {
        return new Rect((screenWidth - REFERENCE_WIDTH * scale) / 2f + x * scale,
                (screenHeight - REFERENCE_HEIGHT * scale) / 2f + y * scale, w * scale, h * scale);
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public Rect getMap() {
        return map;
    }

    public Rect getPreview() {
        return preview;
    }

    /**
     * The fractional tile coordinates under a screen position, or {@code
     * null} if that is not on the map (outside the map area, or in the rock
     * border of a non-square map)
     */
    public Vector2f screenToTile(float screenX, float screenY, int mapWidth, int mapHeight) {
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(mapWidth, mapHeight);
        float tileX = fit.tileX((screenX - map.x()) / map.w() * MinimapRasteriser.RASTER_SIZE);
        float tileY = fit.tileY((screenY - map.y()) / map.h() * MinimapRasteriser.RASTER_SIZE);
        if (tileX < 0 || tileY < 0 || tileX >= mapWidth || tileY >= mapHeight) {
            return null;
        }
        return new Vector2f(tileX, tileY);
    }

    /**
     * The tile under a screen position, see {@link #screenToTile}
     */
    public Point pickTile(float screenX, float screenY, int mapWidth, int mapHeight) {
        Vector2f tile = screenToTile(screenX, screenY, mapWidth, mapHeight);
        return tile != null ? new Point((int) tile.x, (int) tile.y) : null;
    }
}
