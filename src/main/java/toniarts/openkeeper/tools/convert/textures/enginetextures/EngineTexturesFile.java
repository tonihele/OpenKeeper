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
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.imageio.ImageIO;
import toniarts.openkeeper.tools.convert.FileResourceReader;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;
import toniarts.openkeeper.tools.convert.ISeekableResourceReader;
import toniarts.openkeeper.tools.convert.textures.ImageUtil;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Reads Dungeon Keeper II EngineTextures.dat file to a structure<br>
 * Also reads EngineTextures.dir for the texture names<br>
 * The file is LITTLE ENDIAN I might say
 *
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class EngineTexturesFile implements Iterable<String> {
    
    private static final Logger logger = System.getLogger(EngineTexturesFile.class.getName());

    private static final boolean DECOMPRESSION_ENABLED = true;
    private static final int CHESS_BOARD_GRID_SIZE = 8;
    private static final String ENGINE_TEXTURE_HEADER_IDENTIFIER = "TCHC";

    private final int version;

    private final Path file;
    private EngineTextureDecoder decoder;
    private final Map<String, EngineTextureEntry> engineTextureEntries;

    public EngineTexturesFile(Path file) {
        this.file = file;

        // Read the names from the DIR file in the same folder
        Path dirFile = Paths.get(file.toString().substring(0, file.toString().length() - 3) + "dir");
        try (IResourceReader rawDir = new FileResourceReader(dirFile)) {

            // File format:
            // HEADER:
            // TCHC
            // int size
            // int version
            // int numberOfEntries
            // ENTRY:
            // string name
            // int offset <- data offset in the DAT file
            IResourceChunkReader dirReader = rawDir.readChunk(16);
            String header = dirReader.readString(4);
            if (!ENGINE_TEXTURE_HEADER_IDENTIFIER.equals(header)) {
                throw new RuntimeException("Header should be " + ENGINE_TEXTURE_HEADER_IDENTIFIER + " and it was " + header + "! Cancelling!");
            }
            int size = dirReader.readInteger();
            version = dirReader.readInteger();

            // Read the entries
            int numberOfEntries = dirReader.readUnsignedInteger();
            engineTextureEntries = HashMap.newHashMap(numberOfEntries);

            dirReader = rawDir.readChunk(size);
            try (ISeekableResourceReader rawTextures = new FileResourceReader(file)) {
                do {
                    String name = PathUtils.convertFileSeparators(dirReader.readVaryingLengthStrings(1).get(0));
                    int offset = dirReader.readUnsignedInteger();

                    // Read the actual data from the DAT file from the offset specified by the DIR file
                    rawTextures.seek(offset);
                    IResourceChunkReader rawTexturesReader = rawTextures.readChunk(20);

                    // Read the header
                    EngineTextureEntry entry = new EngineTextureEntry();
                    entry.setResX(rawTexturesReader.readUnsignedInteger());
                    entry.setResY(rawTexturesReader.readUnsignedInteger());
                    entry.setSize(rawTexturesReader.readUnsignedInteger() - 8); // - 8 since the size is from here now on
                    entry.setsResX(rawTexturesReader.readUnsignedShort());
                    entry.setsResY(rawTexturesReader.readUnsignedShort());
                    entry.setAlphaFlag(rawTexturesReader.readUnsignedInteger() >> 7 != 0);
                    entry.setDataStartLocation(rawTextures.getFilePointer());

                    // Put the entry to the hash
                    engineTextureEntries.put(name, entry);
                } while (dirReader.hasRemaining());
            } catch (IOException e) {

                // Fug
                throw new RuntimeException("Failed to open the file " + file + "!", e);
            }
        } catch (IOException e) {

            // Fug
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

        // Open the Texture file for extraction
        try (ISeekableResourceReader rawTextures = new FileResourceReader(file)) {

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
    public Path extractFileData(String textureEntry, String destination, boolean overwrite) {

        // Open the Texture file for extraction
        try (ISeekableResourceReader rawTextures = new FileResourceReader(file)) {
            return extractFileData(textureEntry, destination, rawTextures, overwrite);
        } catch (IOException e) {

            // Fug
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
    private Path extractFileData(String textureEntry, String destination, ISeekableResourceReader rawTextures, boolean overwrite) {

        // See that the destination is formatted correctly and create it if it does not exist
        Path destinationFile = Paths.get(destination, textureEntry.concat(".png"));

        if (!overwrite && Files.exists(destinationFile)) {

            // Skip
            logger.log(Level.INFO, "File {0} already exists, skipping!", destinationFile);

            return destinationFile;
        }
        try {
            Files.createDirectories(destinationFile.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create destination folder to " + destinationFile + "!", e);
        }

        // Write to the file
        try (OutputStream out = Files.newOutputStream(destinationFile);
                BufferedOutputStream bout = new BufferedOutputStream(out)) {
            getFileData(textureEntry, rawTextures).writeTo(bout);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to " + destinationFile + "!", e);
        }

        return destinationFile;
    }

    /**
     * Extract a single file
     *
     * @param textureEntry the texture to extract
     * @param rawTextures the opened EngineTextures file
     * @return the file data
     */
    private ByteArrayOutputStream getFileData(String textureEntry, ISeekableResourceReader rawTextures) {
        ByteArrayOutputStream result = null;

        // Get the file
        EngineTextureEntry engineTextureEntry = engineTextureEntries.get(textureEntry);
        if (engineTextureEntry == null) {
            throw new RuntimeException("File " + textureEntry + " not found from the texture archive!");
        }

        ImageIO.setUseCache(false);
        try {

            // We should decompress the texture
            BufferedImage image;
            if (DECOMPRESSION_ENABLED) {

                // Seek to the file we want and read it
                rawTextures.seek(engineTextureEntry.getDataStartLocation());

                IResourceChunkReader rawTexturesReader = rawTextures.readChunk(engineTextureEntry.getSize());
                int count = (engineTextureEntry.getSize()) / 4;
                long[] buf = new long[count];
                for (int i = 0; i < count; i++) {
                    buf[i] = rawTexturesReader.readUnsignedIntegerAsLong();
                }

                // Use the monstrous decompression routine
                image = decompressTexture(buf, engineTextureEntry);
            } else {

                // Use our chess board texture
                image = generateChessBoard(engineTextureEntry);
            }
            result = new ByteArrayOutputStream();
            ImageIO.write(image, "png", result);
        } catch (IOException e) {

            // Fug
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

        // Draw the chess board
        for (int x = 0; x < engineTextureEntry.getResX(); x += CHESS_BOARD_GRID_SIZE) {
            for (int y = 0; y < engineTextureEntry.getResY(); y += CHESS_BOARD_GRID_SIZE) {
                if (g.getColor() == Color.GRAY) {
                    g.setColor(Color.DARK_GRAY);
                } else {
                    g.setColor(Color.GRAY);
                }
                g.fillRect(x, y, CHESS_BOARD_GRID_SIZE, CHESS_BOARD_GRID_SIZE);
            }

            // Change color for each row again so that the starting color is different
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

        // Decompress the texture
        if (decoder == null) {
            decoder = new EngineTextureDecoder();
        }
        byte[] pixels = decoder.dd_texture(buf, engineTextureEntry.getResX() * (32 / 8)/*(bpp / 8 = bytes per pixel)*/, engineTextureEntry.getResX(), engineTextureEntry.getResY(), engineTextureEntry.isAlphaFlag());

        return ImageUtil.createImage(engineTextureEntry.getResX(), engineTextureEntry.getResY(), engineTextureEntry.isAlphaFlag(), pixels);
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
