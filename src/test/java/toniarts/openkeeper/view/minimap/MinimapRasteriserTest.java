/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.minimap;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.FakeFogOfWarInformation;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeRoomsInformation;
import toniarts.openkeeper.game.map.MapColourClass;
import toniarts.openkeeper.game.map.MapColourClassifier;
import toniarts.openkeeper.game.map.MapColourGrid;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static toniarts.openkeeper.tools.convert.map.TerrainFixtures.flags;

class MinimapRasteriserTest {

    private static final short GOLD_ID = 1;
    private static final short WATER_ID = 2;

    /** Every tile a {@link FakeMapData} creates defaults to terrainId 0 - register it. */
    private static Map<Short, Terrain> terrainsWithDefault() {
        Map<Short, Terrain> terrains = new HashMap<>();
        terrains.put((short) 0, TerrainFixtures.terrain((short) 0, flags(Terrain.TerrainFlag.OWNABLE), 0, 10, 10, (short) 0));
        return terrains;
    }

    private static byte[] solidRockTexture() {
        byte[] rock = new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];
        for (int i = 0; i < rock.length; i += 3) {
            rock[i] = 0x11;
            rock[i + 1] = 0x22;
            rock[i + 2] = 0x33;
        }
        return rock;
    }

    /** A palette where every entry's colour is a distinctive, checkable value. */
    private static MinimapPalette markerPalette() {
        BufferedImage image = new BufferedImage(64, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 64; x++) {
                image.setRGB(x, y, (x << 16) | (x << 8) | x);
            }
        }
        return MinimapPalette.load(image);
    }

    @Test
    void fitModeMapsTileZeroZeroAndTheOppositeCornerToTheExpectedPixels() {
        // 4x2 map: width is the big axis.
        FakeMapData mapData = new FakeMapData(4, 2);
        Map<Short, Terrain> terrains = terrainsWithDefault();
        terrains.put(GOLD_ID, TerrainFixtures.terrain(GOLD_ID, flags(Terrain.TerrainFlag.SOLID), 100, 0, 0, GOLD_ID));
        terrains.put(WATER_ID, TerrainFixtures.terrain(WATER_ID, flags(Terrain.TerrainFlag.WATER), 0, 0, 0, WATER_ID));
        mapData.getTile(0, 0).terrainId = GOLD_ID;
        mapData.getTile(3, 1).terrainId = WATER_ID;

        FakeMapInformation mapInformation = new FakeMapInformation(mapData, terrains);
        FakeFogOfWarInformation fog = new FakeFogOfWarInformation();
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 2; y++) {
                fog.explored.add(new Point(x, y));
            }
        }
        MapColourClassifier classifier = new MapColourClassifier(mapInformation, fog, new FakeRoomsInformation());
        MapColourGrid grid = new MapColourGrid(4, 2, classifier, fog);
        grid.recomputeRect(0, 0, 4, 2);

        MinimapPalette palette = markerPalette();
        byte[] rock = solidRockTexture();
        byte[] out = new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];

        MinimapRasteriser.rebuildFitMode(grid, palette, rock, (short) 2, out);

        // tile (0,0) -> pixel (20,42); see the geometry derivation in this
        // test's design notes (offSmall=42 for a 4x2 map).
        assertPixel(out, 20, 42, palette.rawArgb()[MapColourClass.GOLD]);
        // tile (3,1) -> pixel (90,70).
        assertPixel(out, 90, 70, palette.rawArgb()[MapColourClass.WATER]);
    }

    @Test
    void pixelsOutsideTheMapRectangleAreTheRockTexture() {
        FakeMapData mapData = new FakeMapData(4, 2);
        FakeMapInformation mapInformation = new FakeMapInformation(mapData, terrainsWithDefault());
        FakeFogOfWarInformation fog = new FakeFogOfWarInformation();
        MapColourClassifier classifier = new MapColourClassifier(mapInformation, fog, new FakeRoomsInformation());
        MapColourGrid grid = new MapColourGrid(4, 2, classifier, fog);
        grid.recomputeRect(0, 0, 4, 2);

        byte[] rock = solidRockTexture();
        byte[] out = new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];

        MinimapRasteriser.rebuildFitMode(grid, markerPalette(), rock, (short) 2, out);

        // (0,0) is well outside [20,110) x [42,87).
        assertEquals(0x11, out[0] & 0xFF);
        assertEquals(0x22, out[1] & 0xFF);
        assertEquals(0x33, out[2] & 0xFF);
    }

    @Test
    void unexploredTilesInsideTheRectangleStayRockToo() {
        FakeMapData mapData = new FakeMapData(4, 2);
        FakeMapInformation mapInformation = new FakeMapInformation(mapData, terrainsWithDefault());
        FakeFogOfWarInformation fog = new FakeFogOfWarInformation(); // nothing explored
        MapColourClassifier classifier = new MapColourClassifier(mapInformation, fog, new FakeRoomsInformation());
        MapColourGrid grid = new MapColourGrid(4, 2, classifier, fog);
        grid.recomputeRect(0, 0, 4, 2);

        byte[] rock = solidRockTexture();
        byte[] out = new byte[MinimapRasteriser.RASTER_SIZE * MinimapRasteriser.RASTER_SIZE * 3];

        MinimapRasteriser.rebuildFitMode(grid, markerPalette(), rock, (short) 2, out);

        int i = (42 * MinimapRasteriser.RASTER_SIZE + 20) * 3;
        assertEquals(0x11, out[i] & 0xFF);
        assertEquals(0x22, out[i + 1] & 0xFF);
        assertEquals(0x33, out[i + 2] & 0xFF);
    }

    private static void assertPixel(byte[] bgr, int x, int y, int expectedArgb) {
        int i = (y * MinimapRasteriser.RASTER_SIZE + x) * 3;
        int expectedR = (expectedArgb >> 16) & 0xFF;
        int expectedG = (expectedArgb >> 8) & 0xFF;
        int expectedB = expectedArgb & 0xFF;
        assertEquals(expectedB, bgr[i] & 0xFF, "blue at (" + x + "," + y + ")");
        assertEquals(expectedG, bgr[i + 1] & 0xFF, "green at (" + x + "," + y + ")");
        assertEquals(expectedR, bgr[i + 2] & 0xFF, "red at (" + x + "," + y + ")");
    }

}
