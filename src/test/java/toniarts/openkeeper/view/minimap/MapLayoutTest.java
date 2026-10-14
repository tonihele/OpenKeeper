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

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MapLayoutTest {

    /**
     * Picks the tile at the screen position where drawing puts the centre of tile (x, y)
     */
    private static Point pickCentreOf(MapLayout layout, int mapW, int mapH, int x, int y) {
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(mapW, mapH);
        float screenX = layout.getMapX() + fit.pixelX(x + 0.5f) / MinimapRasteriser.RASTER_SIZE * layout.getMapSize();
        float screenY = layout.getMapY() + fit.pixelY(y + 0.5f) / MinimapRasteriser.RASTER_SIZE * layout.getMapSize();
        return layout.pickTile(screenX, screenY, mapW, mapH);
    }

    private static void assertCornersRoundTrip(int screenW, int screenH, int mapW, int mapH) {
        MapLayout layout = new MapLayout(screenW, screenH);
        for (int[] corner : new int[][]{{0, 0}, {mapW - 1, 0}, {0, mapH - 1}, {mapW - 1, mapH - 1}}) {
            Point picked = pickCentreOf(layout, mapW, mapH, corner[0], corner[1]);
            assertNotNull(picked);
            assertEquals(corner[0], picked.x);
            assertEquals(corner[1], picked.y);
        }
    }

    @Test
    void cornersRoundTripOnSquareMap() {
        assertCornersRoundTrip(640, 480, 85, 85);
        assertCornersRoundTrip(1280, 1024, 85, 85);
    }

    @Test
    void cornersRoundTripOnWideAndTallMaps() {
        assertCornersRoundTrip(1920, 1080, 90, 30);
        assertCornersRoundTrip(800, 600, 30, 90);
    }

    @Test
    void borderOfNonSquareMapAndOutsideAreNotPickable() {
        MapLayout layout = new MapLayout(640, 480);
        float middleX = layout.getMapX() + layout.getMapSize() / 2;
        // 3x1 map: the top and bottom thirds of the map area are rock border
        assertNull(layout.pickTile(middleX, layout.getMapY() + 2, 90, 30));
        assertNull(layout.pickTile(middleX, layout.getMapY() + layout.getMapSize() - 2, 90, 30));
        assertNotNull(layout.pickTile(middleX, layout.getMapY() + layout.getMapSize() / 2, 90, 30));
        assertNull(layout.pickTile(5, 5, 50, 50));
    }

    @Test
    void referenceSizeIsUnscaled() {
        MapLayout layout = new MapLayout(640, 480);
        assertEquals(41f, layout.getMapX(), 1e-4f);
        assertEquals(33f, layout.getMapY(), 1e-4f);
        assertEquals(384f, layout.getMapSize(), 1e-4f);
    }

    @Test
    void widescreenIsCentredNotStretched() {
        MapLayout layout = new MapLayout(1920, 1080);
        // 640x480 scaled by 2.25 is 1440 wide, 240 px bars on each side
        assertEquals(240f + 41f * 2.25f, layout.getMapX(), 1e-3f);
        assertEquals(33f * 2.25f, layout.getMapY(), 1e-3f);
        assertEquals(384f * 2.25f, layout.getMapSize(), 1e-3f);
    }
}
