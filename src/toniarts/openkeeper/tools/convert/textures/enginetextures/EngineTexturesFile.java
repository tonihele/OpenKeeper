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
package toniarts.openkeeper.tools.convert.textures.enginetextures;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.ConversionUtils;

/**
 * Reads Dungeon Keeper II EngineTextures.dat file to a structure<br>
 * Also reads EngineTextures.dir for the texture names<br>
 * The file is LITTLE ENDIAN I might say
 *
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EngineTexturesFile implements Iterable<String> {

    private static final Logger logger = Logger.getLogger(EngineTexturesFile.class.getName());
    private static final boolean DECOMPRESSION_ENABLED = true;
    private static final int CHESS_BOARD_GRID_SIZE = 8;
    private final File file;
    private EngineTextureDecoder decoder;
    private final HashMap<String, EngineTextureEntry> engineTextureEntries;

    public EngineTexturesFile(File file) {
        this.file = file;

        //Read the names from the DIR file in the same folder
        File dirFile = new File(file.toString().substring(0, file.toString().length() - 3).concat("dir"));
        try (RandomAccessFile rawDir = new RandomAccessFile(dirFile, "r")) {

            // File format:
            // HEADER:
            // TCHC
            // int size
            // int version
            // int numberOfEntries
            // ENTRY:
            // string name
            // int offset <- data offset in the DAT file

            //Read the entries
            rawDir.skipBytes(12);
            int numberOfEntries = ConversionUtils.readUnsignedInteger(rawDir);
            engineTextureEntries = new HashMap<>(numberOfEntries);

            try (RandomAccessFile rawTextures = new RandomAccessFile(file, "r")) {
                do {
                    String name = ConversionUtils.convertFileSeparators(ConversionUtils.readVaryingLengthStrings(rawDir, 1).get(0));
                    int offset = ConversionUtils.readUnsignedInteger(rawDir);

                    //Read the actual data from the DAT file from the offset specified by the DIR file
                    rawTextures.seek(offset);

                    //Read the header
                    EngineTextureEntry entry = new EngineTextureEntry();
                    entry.setResX(ConversionUtils.readUnsignedInteger(rawTextures));
                    entry.setResY(ConversionUtils.readUnsignedInteger(rawTextures));
                    entry.setSize(ConversionUtils.readUnsignedInteger(rawTextures) - 8); // - 8 since the size is from here now on
                    entry.setsResX(ConversionUtils.readUnsignedShort(rawTextures));
                    entry.setsResY(ConversionUtils.readUnsignedShort(rawTextures));
                    entry.setAlphaFlag(ConversionUtils.readUnsignedInteger(rawTextures) >> 7 != 0);
                    entry.setDataStartLocation(rawTextures.getFilePointer());

                    //Put the entry to the hash
                    engineTextureEntries.put(name, entry);
                } while (rawDir.getFilePointer() != rawDir.length());
            } catch (IOException e) {

                //Fug
                throw new RuntimeException("Failed to open the file " + file + "!", e);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + dirFile + "!", e);
        }
    }

    /**
     * Get the number of texture entries
     *
     * @return texture entry count
     */
    public int getFileCount() {
        return engineTextureEntries.size();
    }

    /**
     * Extract all the files to a given location
     *
     * @param destination destination directory
     */
    public void extractFileData(String destination) {

        //Open the Texture file for extraction
        try (RandomAccessFile rawTextures = new RandomAccessFile(file, "r")) {

            for (String textureEntry : engineTextureEntries.keySet()) {
                extractFileData(textureEntry, destination, rawTextures, true);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    /**
     * Extract a single to a given location
     *
     * @param textureEntry entry to extract
     * @param destination destination directory
     * @param overwrite overwrite destination file
     * @return returns the extracted file
     */
    public File extractFileData(String textureEntry, String destination, boolean overwrite) {

        //Open the Texture file for extraction
        try (RandomAccessFile rawTextures = new RandomAccessFile(file, "r")) {
            return extractFileData(textureEntry, destination, rawTextures, overwrite);
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    /**
     * Extract a single file to a given location
     *
     * @param textureEntry texture to extract
     * @param destination destination directory
     * @param rawTextures the opened EngineTextures file
     * @param overwrite overwrite destination file
     *
     */
    private File extractFileData(String textureEntry, String destination, RandomAccessFile rawTextures, boolean overwrite) {

        //See that the destination is formatted correctly and create it if it does not exist
        String dest = destination;
        if (!dest.endsWith(File.separator)) {
            dest = dest.concat(File.separator);
        }
        File destinationFile = new File(dest.concat(textureEntry).concat(".png"));
        if (!overwrite && destinationFile.exists()) {

            //Skip
            logger.log(Level.INFO, "File {0} already exists, skipping!", destinationFile);
            return destinationFile;
        }
        Path destinationFolder = destinationFile.toPath();
        destinationFolder.getParent().toFile().mkdirs();

        //Write to the file
        try (OutputStream outputStream = new FileOutputStream(destinationFile)) {
            getFileData(textureEntry, rawTextures).writeTo(outputStream);
            return destinationFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to " + destinationFile + "!", e);
        }
    }

    /**
     * Extract a single file
     *
     * @param textureEntry the texture to extract
     * @param rawTextures the opened EngineTextures file
     * @return the file data
     */
    private ByteArrayOutputStream getFileData(String textureEntry, RandomAccessFile rawTextures) {
        ByteArrayOutputStream result = null;

        //Get the file
        EngineTextureEntry engineTextureEntry = engineTextureEntries.get(textureEntry);
        if (engineTextureEntry == null) {
            throw new RuntimeException("File " + textureEntry + " not found from the texture archive!");
        }

        try {

            //We should decompress the texture
            BufferedImage image;
            if (DECOMPRESSION_ENABLED) {

                //Seek to the file we want and read it
                rawTextures.seek(engineTextureEntry.getDataStartLocation());
                int count = (engineTextureEntry.getSize()) / 4;
                long[] buf = new long[count];
                for (int i = 0; i < count; i++) {
                    buf[i] = ConversionUtils.readUnsignedIntegerAsLong(rawTextures);
                }

                // Use the monstrous decompression routine
                image = decompressTexture(buf, engineTextureEntry);
            } else {

                //Use our chess board texture
                image = generateChessBoard(engineTextureEntry);
            }
            result = new ByteArrayOutputStream();
            ImageIO.write(image, "png", result);
        } catch (Exception e) {

            //Fug
            throw new RuntimeException("Faile to read the engine texture file!", e);
        }

        return result;
    }

    /**
     * Creates a chess board texture for the given entry
     *
     * @param engineTextureEntry the texture entry
     * @return chess board texture
     */
    private BufferedImage generateChessBoard(EngineTextureEntry engineTextureEntry) {
        BufferedImage img = new BufferedImage(engineTextureEntry.getResX(), engineTextureEntry.getResY(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        g.setColor(Color.GRAY);

        //Draw the chess board
        for (int x = 0; x < engineTextureEntry.getResX(); x += CHESS_BOARD_GRID_SIZE) {
            for (int y = 0; y < engineTextureEntry.getResY(); y += CHESS_BOARD_GRID_SIZE) {
                if (g.getColor() == Color.GRAY) {
                    g.setColor(Color.DARK_GRAY);
                } else {
                    g.setColor(Color.GRAY);
                }
                g.fillRect(x, y, CHESS_BOARD_GRID_SIZE, CHESS_BOARD_GRID_SIZE);
            }

            //Change color for each row again so that the starting color is different
            if (g.getColor() == Color.GRAY) {
                g.setColor(Color.DARK_GRAY);
            } else {
                g.setColor(Color.GRAY);
            }
        }

        return img;
    }

    /**
     * Decompresses the texture from given entry
     *
     * @param buf the compressed texture data read as uint32 items
     * @param engineTextureEntry the texture entry
     * @return
     */
    private BufferedImage decompressTexture(long[] buf, EngineTextureEntry engineTextureEntry) {
        BufferedImage img = new BufferedImage(engineTextureEntry.getResX(), engineTextureEntry.getResY(), BufferedImage.TYPE_INT_ARGB);

        // Decompress the texture
        if (decoder == null) {
            decoder = new EngineTextureDecoder();
        }
        byte[] pixels = decoder.dd_texture(buf, engineTextureEntry.getResX() * (32 / 8)/*(bpp / 8 = bytes per pixel)*/, engineTextureEntry.getResX(), engineTextureEntry.getResY(), engineTextureEntry.isAlphaFlag());

        // Draw the image, pixel by pixel
        for (int x = 0; x < engineTextureEntry.getResX(); x++) {
            for (int y = 0; y < engineTextureEntry.getResY(); y++) {
                int base = engineTextureEntry.getResX() * y * 4 + x * 4;
                int r = ConversionUtils.toUnsignedByte(pixels[base]);
                int g = ConversionUtils.toUnsignedByte(pixels[base + 1]);
                int b = ConversionUtils.toUnsignedByte(pixels[base + 2]);
                int a = ConversionUtils.toUnsignedByte(pixels[base + 3]);
                int col = (a << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(x, y, col);
            }
        }
        return img;
    }

    @Override
    public Iterator<String> iterator() {
        return engineTextureEntries.keySet().iterator();
    }

    /**
     * Gets a texture entry by the texture key
     *
     * @param texture the texture key
     * @return the texture entry
     */
    public EngineTextureEntry getEntry(String texture) {
        return engineTextureEntries.get(texture);
    }
}
