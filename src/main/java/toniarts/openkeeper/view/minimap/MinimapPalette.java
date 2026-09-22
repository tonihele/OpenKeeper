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

import java.awt.image.BufferedImage;
import java.util.Arrays;
import toniarts.openkeeper.game.map.MapColourClass;

/**
 * {@code GUI\Map\MapColours.png} loaded into class -&gt; colour lookups
 * (minimap_design.md §4.1). Distinct from, and not to be confused with,
 * {@code Textures\Thumbnails\MapColours.png} - a different, 16-slot palette
 * for front-end level thumbnails ({@code utils.MapThumbnailGenerator}); the
 * two files happen to share a name and even a resolution but have unrelated
 * index layouts.
 *
 * <p>
 * Pure Java (only {@code java.awt.image.BufferedImage}, no jME) so this is
 * headlessly testable - {@link #load} takes an already-decoded image rather
 * than a path, matching how {@code MapThumbnailGenerator.readPalette}
 * resolves and decodes the file it reads.
 */
public final class MinimapPalette {

    /**
     * The original allocates 549 palette entries; matched here even though
     * only indices up to {@code 0x23 + 7 = 0x2A} are ever loaded or read.
     */
    public static final int SIZE = 549;

    private static final int OPAQUE_WHITE = 0xFFFFFFFF;

    private final int[] argb = new int[SIZE];

    private MinimapPalette() {
        Arrays.fill(argb, OPAQUE_WHITE);
    }

    /**
     * @param image a decoded {@code MapColours.png} - 64x16, 24-bit RGB, all
     * rows identical (only row 0 is read)
     * @throws IllegalArgumentException if {@code image} isn't 24-bit RGB
     * (matches the original's own reported failure: "Unable to read
     * MapColours file (GUI\map\MapColours.png)")
     */
    public static MinimapPalette load(BufferedImage image) {
        if (image.getColorModel().hasAlpha() || image.getColorModel().getPixelSize() != 24) {
            throw new IllegalArgumentException("Unable to read MapColours file (GUI\\map\\MapColours.png)");
        }

        MinimapPalette palette = new MinimapPalette();
        palette.argb[0] = opaque(image.getRGB(0, 0));
        for (int i = 1; i <= 15; i++) {
            // palette[1] is deliberately left white - px(1) lands at index 2.
            palette.argb[i + 1] = opaque(image.getRGB(i, 0));
        }
        for (int n = 1; n <= 7; n++) {
            palette.argb[MapColourClass.OWNED_SOLID_BASE + n] = opaque(image.getRGB(15 + n, 0)); // walls: px 16..22
            palette.argb[MapColourClass.OWNED_FLOOR_BASE + n] = opaque(image.getRGB(22 + n, 0)); // floors: px 23..29
            palette.argb[MapColourClass.DUNGEON_HEART_BASE + n] = opaque(image.getRGB(29 + n, 0)); // hearts: px 30..36
        }
        return palette;
    }

    private static int opaque(int rgb) {
        return rgb | 0xFF000000;
    }

    /**
     * Bug-for-bug parity (design §4.1): the neutral player's own wall/floor/
     * heart slots are aliased onto {@code n % 7}'s slot, so a neutral player
     * number of 7 (never true for shipped data - always 1) doesn't read
     * back as white. A no-op for every shipped level. Meant to be re-applied
     * before every raster pass, not just once at load - matching the
     * original's own redundant-looking but deliberate call pattern.
     *
     * @param neutralPlayerNumber {@code playerNumber(neutralPlayerId)}
     */
    public void applyNeutralGuard(short neutralPlayerNumber) {
        if (neutralPlayerNumber < 1 || neutralPlayerNumber > 7) {
            return;
        }
        int wrapped = neutralPlayerNumber % 7;
        argb[MapColourClass.DUNGEON_HEART_BASE + neutralPlayerNumber] = argb[MapColourClass.DUNGEON_HEART_BASE + wrapped];
        argb[MapColourClass.OWNED_SOLID_BASE + neutralPlayerNumber] = argb[MapColourClass.OWNED_SOLID_BASE + wrapped];
        argb[MapColourClass.OWNED_FLOOR_BASE + neutralPlayerNumber] = argb[MapColourClass.OWNED_FLOOR_BASE + wrapped];
    }

    /**
     * Used only by the full-screen map (minimap_design.md §6), not the
     * panel raster (which indexes {@link #argb} directly per-pixel via
     * {@link #rawArgb}). Class 1 (unexplored/rock) reads as white here
     * because the full-screen map skips class-1 tiles entirely rather than
     * painting them, so this case is unreachable in practice; included for
     * completeness per the design's own description.
     *
     * @param neutralPlayerNumber {@code playerNumber(neutralPlayerId)}, for
     * the same aliasing {@link #applyNeutralGuard} performs
     */
    public int colourFor(short colourClass, short neutralPlayerNumber) {
        if (colourClass == MapColourClass.UNEXPLORED_OR_IMPENETRABLE) {
            return OPAQUE_WHITE;
        }
        if (neutralPlayerNumber >= 1 && neutralPlayerNumber <= 7) {
            int wrapped = neutralPlayerNumber % 7;
            if (colourClass == (short) (MapColourClass.DUNGEON_HEART_BASE + neutralPlayerNumber)) {
                return argb[MapColourClass.DUNGEON_HEART_BASE + wrapped];
            }
            if (colourClass == (short) (MapColourClass.OWNED_SOLID_BASE + neutralPlayerNumber)) {
                return argb[MapColourClass.OWNED_SOLID_BASE + wrapped];
            }
            if (colourClass == (short) (MapColourClass.OWNED_FLOOR_BASE + neutralPlayerNumber)) {
                return argb[MapColourClass.OWNED_FLOOR_BASE + wrapped];
            }
        }
        return argb[colourClass];
    }

    /**
     * The raw loaded table, opaque ARGB, index = colour class. Read-only:
     * callers must not mutate the returned array.
     */
    public int[] rawArgb() {
        return argb;
    }

}
