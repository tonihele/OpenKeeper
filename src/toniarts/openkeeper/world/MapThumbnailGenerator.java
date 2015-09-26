/*
 * Copyright (C) 2014-2015 OpenKeeper
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
package toniarts.openkeeper.world;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.ArtResource;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.tools.convert.map.Map;
import toniarts.openkeeper.tools.convert.map.Terrain;

/**
 * This class creates thumbnails from KWD map files. Static helper class. The
 * images are created with quite simple indexed palette. So they are not
 * suitable for super sampling etc.<br>
 * Issue: https://github.com/tonihele/OpenKeeper/issues/70
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapThumbnailGenerator {

    private static final Object paletteLock = new Object();
    private static final String PALETTE_IMAGE = "Textures".concat(File.separator).concat("Thumbnails").concat(File.separator).concat("MapColours.png");
    private static volatile IndexColorModel cm;
    private static final Logger logger = Logger.getLogger(MapThumbnailGenerator.class.getName());

    private static boolean isRoom(Terrain tile) {
        ArtResource ceilingResource = MapLoader.getCeilingResource(tile);
        return (ceilingResource == null && tile.getCompleteResource() == null);
    }

    private MapThumbnailGenerator() {
        // Nope
    }

    /**
     * Get the map thumbnail, the map thumbnail is the size of the map itself
     *
     * @param kwd the map to thumbnail
     * @return the map thumbnail image
     */
    public static BufferedImage generateMap(KwdFile kwd) {

        // Get the palette if not gotten already
        if (cm == null) {
            synchronized (paletteLock) {
                if (cm == null) {
                    cm = readPalette();
                }
            }
        }

        // Ensure that the kwd is fully loaded
        kwd.load();

        // Format the data models
        int[] bandOffsets = new int[1];
        PixelInterleavedSampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                kwd.getWidth(), kwd.getHeight(),
                1,
                kwd.getWidth(),
                bandOffsets);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, null);
        BufferedImage bi = new BufferedImage(cm, raster, false, null);
        byte[] data = (byte[]) ((DataBufferByte) raster.getDataBuffer()).getData();

        // Draw the map itself
        drawMap(kwd, data);

        return bi;
    }

    private static IndexColorModel readPalette() {
        try {

            // Read the DK II palette image
            BufferedImage paletteImage = ImageIO.read(new File(ConversionUtils.getRealFileName(AssetsConverter.getAssetsFolder(), PALETTE_IMAGE)));

            // The palette image is generally an image where 1 column represents one color, column width is 1px
            // We know that is is 64x16, but just play along with "dynamic" (we'll fail if it is over 256)
            byte[] r = new byte[paletteImage.getWidth()];
            byte[] g = new byte[paletteImage.getWidth()];
            byte[] b = new byte[paletteImage.getWidth()];
            for (int x = 0; x < paletteImage.getWidth(); x++) {
                int color = paletteImage.getRGB(x, 0);
                r[x] = (byte) ((color & 0xff0000) >> 16);
                g[x] = (byte) ((color & 0xff00) >> 8);
                b[x] = (byte) (color & 0xff);
            }

            // Create the actual palette
            return new IndexColorModel(8, paletteImage.getWidth(), r, g, b);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create the map thumbnail palette!", e);

            // TODO: Create a random palette here?
            throw new RuntimeException("Failed to create the map thumbnail palette!", e);
        }
    }

    private static void drawMap(KwdFile kwd, byte[] data) {

        // For now this is very much hard coded, I couldn't find much logic
        for (int y = 0; y < kwd.getHeight(); y++) {
            for (int x = 0; x < kwd.getWidth(); x++) {
                Map tile = kwd.getTile(x, y);
                byte value = 0;

                // Water and lava
                Terrain terrainTile = kwd.getTerrain(tile.getTerrainId());
                if (x == 0 || y == 0 || y == kwd.getHeight() - 1 || x == kwd.getWidth() - 1) {
                    value = 46; // Edge of maps
                } else if (kwd.getLava().getTerrainId() == tile.getTerrainId()) {
                    value = 10;
                } else if (kwd.getWater().getTerrainId() == tile.getTerrainId()) {
                    value = 8;
                } // Other non-ownable tiles
                else if (terrainTile.getFlags().contains(Terrain.TerrainFlag.IMPENETRABLE)) {
                    if (terrainTile.getGoldValue() > 0) {
                        value = 4; // Gems
                    } else {
                        value = 2; // Impenetrable
                    }
                } else if (terrainTile.getGoldValue() > 0) {
                    value = 6; // Gold
                } else if (!terrainTile.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    if (terrainTile.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                        value = 3; // Rock
                    } else {
                        value = 1; // Dirt path
                    }
                } // Owned tiles & buildings
                else if (isRoom(terrainTile)) {
                    // Good == 1
                    // Neutral == 2
                    // Player 1 == 3
                    // And so on
                    // Good room == 36
                    // Neutral room == 37
                    // Player 1 room = 38
                    value = (byte) (35 + tile.getPlayerId()); // Building + owned color
                } else if (terrainTile.getFlags().contains(Terrain.TerrainFlag.SOLID)) {
                    value = (byte) (15 + tile.getPlayerId()); // Wall + owned color
                } else if (terrainTile.getFlags().contains(Terrain.TerrainFlag.OWNABLE)) {
                    value = (byte) (25 + tile.getPlayerId()); // Wall + owned color
                } else {

                    // Wat
                    logger.log(Level.WARNING, "Unkown tile on {0} at tile {1}, {2}!", new Object[]{kwd, x, y});
                }

                // Write the value
                data[y * kwd.getWidth() + x] = value;
            }
        }
    }
}
