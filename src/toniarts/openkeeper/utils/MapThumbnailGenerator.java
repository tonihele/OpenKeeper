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
package toniarts.openkeeper.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Tile;

/**
 * This class creates thumbnails from KWD map files. Static helper class. The images can be acquired in
 * variety of high quality sizes. Always super sampled to fit your individual needs!<br>
 * Issue: https://github.com/tonihele/OpenKeeper/issues/70
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class MapThumbnailGenerator {

    private static final Logger logger = System.getLogger(MapThumbnailGenerator.class.getName());

    private static final String PALETTE_IMAGE = String.join(File.separator,
            List.of("Textures", "Thumbnails", "MapColours.png"));
    private static ColorModel cm;
    private static Map<Short, Color> playerColors;

    private MapThumbnailGenerator() {
        // Nope
    }

    private static ColorModel getColorModel() {
        if (cm == null) {
            cm = readPalette();
        }

        return cm;
    }

    /**
     * Get the map thumbnail, the map thumbnail is the size of the map itself. Tries to create the image in
     * manner that maximizes the space usage in your given dimensions.
     *
     * @param kwd the map to thumbnail
     * @param width wanted image width, can be null
     * @param height wanted image height, can be null
     * @param preserveAspectRatio whether to preserve the map aspect ratio
     * @return the map thumbnail image
     */
    public static BufferedImage generateMap(final IKwdFile kwd, final Integer width, final Integer height,
            final boolean preserveAspectRatio) {
        // Determine wanted width/height
        int imageWidth = kwd.getMap().getWidth();
        int imageHeight = kwd.getMap().getHeight();
        int drawWidth = kwd.getMap().getWidth();
        int drawHeight = kwd.getMap().getHeight();
        if (width != null || height != null) {
            imageWidth = (width != null ? width : imageWidth);
            imageHeight = (height != null ? height : imageHeight);

            // Aspect ratio
            if (preserveAspectRatio) {
                if (width != null && height == null) {

                    // Go by width
                    imageHeight = imageWidth * kwd.getMap().getHeight() / kwd.getMap().getWidth();
                } else if (height != null && width == null) {

                    // Go by height
                    imageWidth = imageHeight * kwd.getMap().getWidth() / kwd.getMap().getHeight();
                } else {

                    // Max out the image
                    int byWidthArea = (imageWidth * kwd.getMap().getHeight() / kwd.getMap().getWidth()) * imageWidth;
                    int byHeightArea = (imageHeight * kwd.getMap().getWidth() / kwd.getMap().getHeight()) * imageHeight;
                    if (byWidthArea > byHeightArea) {
                        imageHeight = imageWidth * kwd.getMap().getHeight() / kwd.getMap().getWidth();
                    } else {
                        imageWidth = imageHeight * kwd.getMap().getWidth() / kwd.getMap().getHeight();
                    }
                }
            }

            // We need to draw the map in even steven pixel scale so...
            drawWidth = kwd.getMap().getWidth() * (int) Math.ceil((float) imageWidth / kwd.getMap().getWidth());
            drawHeight = kwd.getMap().getHeight() * (int) Math.ceil((float) imageHeight / kwd.getMap().getHeight());
        }

        // Format the data models
        int[] bandOffsets = new int[1];
        PixelInterleavedSampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE,
                drawWidth, drawHeight,
                1,
                drawWidth,
                bandOffsets);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, null);
        BufferedImage bi = new BufferedImage(getColorModel(), raster, false, null);
        byte[] data = (byte[]) ((DataBufferByte) raster.getDataBuffer()).getData();

        // Draw the map itself
        drawMap(kwd, data, drawWidth / kwd.getMap().getWidth(), drawHeight / kwd.getMap().getHeight());

        // See if we should super sample
        if (drawWidth != imageWidth || drawHeight != imageHeight) {
            BufferedImage newImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = newImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(bi, 0, 0, newImage.getWidth(), newImage.getHeight(), 0, 0, bi.getWidth(), bi.getHeight(), null);
            g.dispose();
            return newImage;
        }

        return bi;
    }

    private static ColorModel readPalette() {
        try {
            Path palettePath = Paths.get(PathUtils.getRealFileName(AssetsConverter.getAssetsFolder(), PALETTE_IMAGE));

            // Read the DK II palette image
            BufferedImage paletteImage = readImageFromPath(palettePath);

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

    private static BufferedImage readImageFromPath(Path palettePath) throws IOException {
        ImageIO.setUseCache(false);
        try (InputStream is = Files.newInputStream(palettePath);
                BufferedInputStream bis = new BufferedInputStream(is)) {
            return ImageIO.read(bis);
        }
    }

    private static void drawMap(final IKwdFile kwd, byte[] data, int xScale, int yScale) {

        // For now this is very much hard coded, I couldn't find much logic
        for (int y = 0; y < kwd.getMap().getHeight(); y++) {
            for (int x = 0; x < kwd.getMap().getWidth(); x++) {
                Tile tile = kwd.getMap().getTile(x, y);
                byte value = 0;

                // Water and lava
                Terrain terrainTile = kwd.getTerrain(tile.getTerrainId());
                if (x == 0 || y == 0 || y == kwd.getMap().getHeight() - 1 || x == kwd.getMap().getWidth() - 1) {
                    value = 46; // Edge of maps
                } else if (kwd.getMap().getLava().getTerrainId() == tile.getTerrainId()) {
                    value = 10; // Lava
                } else if (kwd.getMap().getWater().getTerrainId() == tile.getTerrainId()) {
                    value = 8; // Water
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
                    value = (byte) (25 + tile.getPlayerId()); // Path + owned color
                } else {

                    // Wat
                    logger.log(Level.WARNING, "Unkown tile on {0} at tile {1}, {2}!", new Object[]{kwd, x, y});
                }

                // Write the value
                for (int yScaling = 0; yScaling < yScale; yScaling++) {
                    for (int xScaling = 0; xScaling < xScale; xScaling++) {
                        data[(y * yScale + yScaling) * kwd.getMap().getWidth() * xScale + (x * xScale + xScaling)] = value;
                    }
                }

            }
        }
    }

    private static boolean isRoom(Terrain tile) {
        return tile.getFlags().contains(Terrain.TerrainFlag.ROOM);
    }

    private static Map<Short, Color> getPlayerColors() {
        if (playerColors == null) {
            short[] playerIds = Utils.getPlayerIds();
            Map<Short, Color> pColors = HashMap.newHashMap(playerIds.length);
            for (short playerId : playerIds) {
                pColors.put(playerId, createPlayerColor(playerId));
            }

            playerColors = pColors;
        }

        return playerColors;
    }

    private static Color createPlayerColor(short playerId) {
        return new Color(getColorModel().getRGB(35 + playerId));
    }

    /**
     * Get player color
     *
     * @param playerId the player ID
     * @return the player color
     */
    public static Color getPlayerColor(short playerId) {
        return getPlayerColors().get(playerId);
    }
}
