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
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.map.MapColourClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Exercises {@link MinimapPalette}'s index-mapping formula
 * against a small synthetic image with a distinct, checkable colour per
 * column - deterministic and independent of whatever the shipped
 * {@code MapColours.png} happens to contain.
 */
class MinimapPaletteTest {

    /**
     * Deterministic, distinct RGB per column so a mismatched index produces
     * an unmistakably wrong value rather than a coincidentally-close one.
     */
    private static int markerRgb(int column) {
        int r = (column * 3 + 1) & 0xFF;
        int g = (255 - column * 2) & 0xFF;
        int b = (column * 5 + 7) & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private static BufferedImage syntheticPaletteImage() {
        BufferedImage image = new BufferedImage(64, 16, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 64; x++) {
                image.setRGB(x, y, markerRgb(x));
            }
        }
        return image;
    }

    private static int opaque(int column) {
        return markerRgb(column) | 0xFF000000;
    }

    @Test
    void rejectsNonTwentyFourBitImages() {
        BufferedImage argbImage = new BufferedImage(64, 16, BufferedImage.TYPE_INT_ARGB);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> MinimapPalette.load(argbImage));
        assertEquals("Unable to read MapColours file (GUI\\map\\MapColours.png)", ex.getMessage());
    }

    @Test
    void px0LoadsIntoIndexZero() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        assertEquals(opaque(0), palette.rawArgb()[0]);
    }

    @Test
    void index1IsDeliberatelyLeftWhite() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        assertEquals(0xFFFFFFFF, palette.rawArgb()[1]);
    }

    @Test
    void px1Through15LoadIntoIndex2Through16() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        for (int px = 1; px <= 15; px++) {
            assertEquals(opaque(px), palette.rawArgb()[px + 1], "px" + px);
        }
    }

    @Test
    void wallFloorHeartLoopsLandAtTheDesignsIndices() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        for (int n = 1; n <= 7; n++) {
            assertEquals(opaque(15 + n), palette.rawArgb()[MapColourClass.OWNED_SOLID_BASE + n], "wall n=" + n);
            assertEquals(opaque(22 + n), palette.rawArgb()[MapColourClass.OWNED_FLOOR_BASE + n], "floor n=" + n);
            assertEquals(opaque(29 + n), palette.rawArgb()[MapColourClass.DUNGEON_HEART_BASE + n], "heart n=" + n);
        }
    }

    @Test
    void unloadedEntriesStayOpaqueWhite() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        // Nothing is ever loaded past DUNGEON_HEART_BASE + 7 (0x2A = 42).
        assertEquals(0xFFFFFFFF, palette.rawArgb()[50]);
        assertEquals(0xFFFFFFFF, palette.rawArgb()[MinimapPalette.SIZE - 1]);
    }

    @Test
    void neutralGuardIsANoOpForPlayerNumbersUnderSeven() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        int before = palette.rawArgb()[MapColourClass.OWNED_FLOOR_BASE + 2];

        palette.applyNeutralGuard((short) 2);

        assertEquals(before, palette.rawArgb()[MapColourClass.OWNED_FLOOR_BASE + 2]);
    }

    @Test
    void neutralGuardAliasesSlotSevenOntoSlotZero() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        int slotZero = palette.rawArgb()[MapColourClass.OWNED_FLOOR_BASE]; // never loaded, still white

        palette.applyNeutralGuard((short) 7);

        assertEquals(slotZero, palette.rawArgb()[MapColourClass.OWNED_FLOOR_BASE + 7]);
    }

    @Test
    void colourForUnexploredIsWhite() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        assertEquals(0xFFFFFFFF, palette.colourFor(MapColourClass.UNEXPLORED_OR_IMPENETRABLE, (short) 2));
    }

    @Test
    void colourForOrdinaryClassReturnsThePaletteEntryDirectly() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        // GEMS = index 5 = argb[i+1] for i=4, i.e. px4.
        assertEquals(opaque(4), palette.colourFor(MapColourClass.GEMS, (short) 2));
    }

    @Test
    void colourForNeutralsOwnSlotWrapsToPlayerNumberZero() {
        MinimapPalette palette = MinimapPalette.load(syntheticPaletteImage());
        short neutralHeartClass = (short) (MapColourClass.DUNGEON_HEART_BASE + 7);

        int result = palette.colourFor(neutralHeartClass, (short) 7);

        assertEquals(palette.rawArgb()[MapColourClass.DUNGEON_HEART_BASE], result);
    }

}
