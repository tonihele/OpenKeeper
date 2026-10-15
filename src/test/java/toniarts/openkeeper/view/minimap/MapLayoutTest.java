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
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapLayoutTest {

    /**
     * Picks the tile at the screen position where drawing puts the centre of tile (x, y)
     */
    private static Point pickCentreOf(MapLayout layout, int mapW, int mapH, int x, int y) {
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(mapW, mapH);
        float screenX = layout.getMap().x() + fit.pixelX(x + 0.5f) / MinimapRasteriser.RASTER_SIZE * layout.getMap().w();
        float screenY = layout.getMap().y() + fit.pixelY(y + 0.5f) / MinimapRasteriser.RASTER_SIZE * layout.getMap().h();
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
    void cornersRoundTripOnAnyScreenShape() {
        assertCornersRoundTrip(2560, 1080, 85, 85); // ultrawide
        assertCornersRoundTrip(1000, 1000, 85, 60);
        assertCornersRoundTrip(800, 1200, 40, 70); // portrait
    }

    @Test
    void borderOfNonSquareMapAndOutsideAreNotPickable() {
        MapLayout layout = new MapLayout(640, 480);
        float middleX = layout.getMap().x() + layout.getMap().w() / 2;
        // 3x1 map: the top and bottom thirds of the map area are rock border
        assertNull(layout.pickTile(middleX, layout.getMap().y() + 2, 90, 30));
        assertNull(layout.pickTile(middleX, layout.getMap().y() + layout.getMap().h() - 2, 90, 30));
        assertNotNull(layout.pickTile(middleX, layout.getMap().y() + layout.getMap().h() / 2, 90, 30));
        assertNull(layout.pickTile(5, 5, 50, 50));
    }

    @Test
    void referenceSizeIsUnscaled() {
        MapLayout layout = new MapLayout(640, 480);
        assertEquals(41f, layout.getMap().x(), 1e-4f);
        assertEquals(33f, layout.getMap().y(), 1e-4f);
        assertEquals(384f, layout.getMap().w(), 1e-4f);
    }

    @Test
    void layoutFillsTheScreenOnEachAxis() {
        MapLayout layout = new MapLayout(1920, 1080);
        assertEquals(41f * 3f, layout.getMap().x(), 1e-3f);
        assertEquals(33f * 2.25f, layout.getMap().y(), 1e-3f);
        assertEquals(384f * 3f, layout.getMap().w(), 1e-3f);
        assertEquals(384f * 2.25f, layout.getMap().h(), 1e-3f);
    }

    @Test
    void previewWindowStaysOnScreenAtAnyShape() {
        for (int[] size : new int[][]{{1024, 768}, {1280, 1024}, {1920, 1080}, {2560, 1080}}) {
            MapLayout.Rect preview = new MapLayout(size[0], size[1]).getPreview();
            assertTrue(preview.x() >= 0 && preview.x() + preview.w() <= size[0]);
            assertTrue(preview.y() >= 0 && preview.y() + preview.h() <= size[1]);
        }
    }

    @Test
    void previewWindowDoesNotOverlapTheMap() {
        MapLayout layout = new MapLayout(1920, 1080);
        assertTrue(layout.getPreview().x() >= layout.getMap().x() + layout.getMap().w());
    }

    @Test
    void screenToTileGivesFractionalCoordinates() {
        MapLayout layout = new MapLayout(640, 480);
        MinimapRasteriser.FitGeometry fit = MinimapRasteriser.FitGeometry.of(40, 40);
        float screenX = layout.getMap().x() + fit.pixelX(10.25f) / MinimapRasteriser.RASTER_SIZE * layout.getMap().w();
        float screenY = layout.getMap().y() + fit.pixelY(20.75f) / MinimapRasteriser.RASTER_SIZE * layout.getMap().h();
        com.jme3.math.Vector2f tile = layout.screenToTile(screenX, screenY, 40, 40);
        assertEquals(10.25f, tile.x, 1e-3f);
        assertEquals(20.75f, tile.y, 1e-3f);
    }

    @Test
    void tileToScreenIsTheInverseOfScreenToTile() {
        MapLayout layout = new MapLayout(1920, 1080);
        com.jme3.math.Vector2f screen = layout.tileToScreen(12.5f, 60.25f, 85, 85);
        com.jme3.math.Vector2f tile = layout.screenToTile(screen.x, screen.y, 85, 85);
        assertEquals(12.5f, tile.x, 1e-3f);
        assertEquals(60.25f, tile.y, 1e-3f);
    }

    @Test
    void legendIsBelowThePreviewAndSizesGrowWithTheResolution() {
        MapLayout small = new MapLayout(640, 480);
        MapLayout large = new MapLayout(1280, 960);
        assertTrue(small.getLegend().y() >= small.getPreview().y() + small.getPreview().h());
        assertEquals(2f * small.getScale(), large.getScale(), 1e-5f);
    }

    @Test
    void backdropCoversTheScreenAndHoldsEverythingElse() {
        for (int[] size : new int[][]{{1024, 768}, {1920, 1080}, {800, 1200}}) {
            MapLayout layout = new MapLayout(size[0], size[1]);
            MapLayout.Rect backdrop = layout.getBackdrop();
            assertEquals(0f, backdrop.x(), 1e-3f);
            assertEquals(0f, backdrop.y(), 1e-3f);
            assertEquals(size[0], backdrop.w(), 1e-3f);
            assertEquals(size[1], backdrop.h(), 1e-3f);
            for (MapLayout.Rect inner : new MapLayout.Rect[]{layout.getMap(), layout.getPreview(), layout.getLegend()}) {
                assertTrue(inner.x() >= 0 && inner.x() + inner.w() <= size[0] + 1e-3f);
                assertTrue(inner.y() >= 0 && inner.y() + inner.h() <= size[1] + 1e-3f);
            }
        }
    }

    @Test
    void sizesThatMustStaySquareUseTheSmallerScale() {
        assertEquals(2.25f, new MapLayout(1920, 1080).getScale(), 1e-5f);
        assertEquals(1.25f, new MapLayout(800, 720).getScale(), 1e-5f);
    }
}
