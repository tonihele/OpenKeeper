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
 * scaled to the real screen on each axis separately, so it fills any screen
 * shape. Coordinates are top-left origin, Y down.
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
    private final float scaleX;
    private final float scaleY;
    private final Rect backdrop;
    private final Rect map;
    private final Rect preview;
    private final Rect legend;

    public MapLayout(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        scaleX = screenWidth / REFERENCE_WIDTH;
        scaleY = screenHeight / REFERENCE_HEIGHT;
        backdrop = fromReference(0, 0, REFERENCE_WIDTH, REFERENCE_HEIGHT);
        map = fromReference(41, 33, 384, 384);
        preview = fromReference(466, 33, 128, 192);
        legend = fromReference(466, 266, 106, 151);
    }

    private Rect fromReference(float x, float y, float w, float h) {
        return new Rect(x * scaleX, y * scaleY, w * scaleX, h * scaleY);
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * The backdrop picture, which covers the whole screen
     */
    public Rect getBackdrop() {
        return backdrop;
    }

    public Rect getMap() {
        return map;
    }

    public Rect getPreview() {
        return preview;
    }

    public Rect getLegend() {
        return legend;
    }

    /**
     * Screen pixels per reference pixel, for sizes that must stay square
     * (text, dots, icons) and grow with the resolution
     */
    public float getScale() {
        return Math.min(scaleX, scaleY);
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
     * The screen position of fractional tile coordinates, the inverse of
     * {@link #screenToTile} (but not limited to the map)
     */
    public Vector2f tileToScreen(float tileX, float tileY, int mapWidth, int mapHeight) {
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(mapWidth, mapHeight);
        return new Vector2f(map.x() + fit.pixelX(tileX) / MinimapRasteriser.RASTER_SIZE * map.w(),
                map.y() + fit.pixelY(tileY) / MinimapRasteriser.RASTER_SIZE * map.h());
    }

    /**
     * The tile under a screen position, see {@link #screenToTile}
     */
    public Point pickTile(float screenX, float screenY, int mapWidth, int mapHeight) {
        Vector2f tile = screenToTile(screenX, screenY, mapWidth, mapHeight);
        return tile != null ? new Point((int) tile.x, (int) tile.y) : null;
    }
}
