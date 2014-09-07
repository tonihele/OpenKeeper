/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.enginetextures;

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
import toniarts.opendungeonkeeper.tools.convert.Utils;

/**
 * Reads Dungeon Keeper II EngineTextures.dat file to a structure<br>
 * Also reads EngineTextures.dir for the texture names<br>
 * The file is LITTLE ENDIAN I might say<br>
 * Texture extraction code by George Gensure at
 * http://keeperklan.com/threads/220-DK2-texture-format/page5
 *
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EngineTexturesFile implements Iterable<String> {

    private static final Logger logger = Logger.getLogger(EngineTexturesFile.class.getName());
    private static final boolean DECOMPRESSION_ENABLED = false;
    private static final int CHESS_BOARD_GRID_SIZE = 8;
    private final File file;
    private final HashMap<String, EngineTextureEntry> engineTextureEntries;
    private boolean decompressionInitialized = false;
    //Decompression stuff I don't understand
    /* external buffers and data to be supplied with sizes */
    private int[] magic_input_table_6c10c0 = new int[64];
    private short[] jump_table_7af4e0 = new short[256];
    private int[] dc_control_table_7af0e0 = new int[224]; //Unsigned
    private int[] magic_output_table = new int[64]; /* magic values computed from magic input */

    private int bs;//Unsigned
    private int bs_index;//Unsigned
    private int bs_red = 0;//Unsigned
    private int bs_green = 0;//Unsigned
    private int bs_blue = 0;//Unsigned
    private int bs_alpha = 0;//Unsigned
    private int alpha_flag = 0; //Unsigned
    private int[] decompress2_chunk = new int[256]; /* buffers */

    private int[] decompress3_chunk = new int[288];
    private int[] decompress4_chunk = new int[512];
    private float float_7af000;
    private float float_7af004;
    private float float_7af008;
    private float float_7af00c;
    private float float_7af010;
    private float float_7af014;
    private float float_7af018;

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
            int numberOfEntries = Utils.readUnsignedInteger(rawDir);
            engineTextureEntries = new HashMap<>(numberOfEntries);

            try (RandomAccessFile rawTextures = new RandomAccessFile(file, "r")) {
                do {
                    String name = Utils.readVaryingLengthStrings(rawDir, 1).get(0);
                    int offset = Utils.readUnsignedInteger(rawDir);

                    //Read the actual data from the DAT file from the offset specified by the DIR file
                    rawTextures.seek(offset);

                    //Read the header
                    EngineTextureEntry entry = new EngineTextureEntry();
                    entry.setResX(Utils.readUnsignedInteger(rawTextures));
                    entry.setResY(Utils.readUnsignedInteger(rawTextures));
                    entry.setSize(Utils.readUnsignedInteger(rawTextures));
                    entry.setsResX(Utils.readUnsignedShort(rawTextures));
                    entry.setsResY(Utils.readUnsignedShort(rawTextures));
                    entry.setUnknown(Utils.readUnsignedInteger(rawTextures));
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
            logger.log(Level.INFO, "File " + destinationFile + " already exists, skipping!");
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

            //Seek to the file we want and read it
            rawTextures.seek(engineTextureEntry.getDataStartLocation());
            byte[] bytes = new byte[engineTextureEntry.getSize() - 8]; // -8 since 2x shorts and an unkown int belongs to the size, but our start location is past those
            rawTextures.read(bytes);

            result = new ByteArrayOutputStream();

            //We should decompress the texture
            if (DECOMPRESSION_ENABLED) {
                //dd_texture();
            } else {

                //Use our chess board texture
                ImageIO.write(generateChessBoard(engineTextureEntry), "png", result);
            }

        } catch (Exception e) {

            //Fug
            throw new RuntimeException("Faile to read the WAD file!", e);
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

    /* must be called prior to calling dd_texture */
    private void dd_init() {
        if (!decompressionInitialized) {
            int i;
            int d, a;

            for (i = 0; i < 64; i++) {
                d = (magic_input_table_6c10c0[i] & 0xfffe0000) >> 3;
                a = (magic_input_table_6c10c0[i] & 0x0001ffff) << 3;

                magic_output_table[i] = d + a;
            }
            decompressionInitialized = true;
        }
    }

    private short[] dd_texture(int[] buf,
            int stride, short width, short height) {
        short flag;
        int x, y;
        short[] out = new short[1000];

        initialize_dd(buf[1]);
        flag = new Integer(buf[0]).shortValue();
        alpha_flag = flag >> 7;

        for (y = 0; y < height; y += 8) {
            for (x = 0; x < width; x += 8) {
                decompress_block(out[y * stride + x * 4], stride);
            }
        }
        return out;
    }

    private void initialize_dd(int buf) {
        bs = buf;
        bs_index = 0;
        bs_red = 0;
        bs_blue = 0;
        bs_green = 0;
        bs_alpha = 0;
    }

    private void decompress_block(short out, int stride) {
//        int[] inp;
//        double d;
//        int xr, xg, xb;
//        int ir, ig, ib;
//
//        int a;
//        float r, g, b;
//        int i, j;
//
//        decompress();
//
//        inp = decompress4_chunk;
//        for (j = 0; j < 8; j++) {
//            for (i = 0; i < 8; i++) {
//                int value;
//                r = inp[i + 0];
//                g = inp[i + 18];
//                b = inp[i + 9];
//                a = inp[i + 27];
//                d = float_7af014 * (g - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
//                xr = d + (d > 0 ? 0.5f : -0.5f);
//                ir = xr;
//                d = float_7af018 * (b - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
//                xg = d + (d > 0 ? 0.5f : -0.5f);
//                ig = xg;
//                d = float_7af010 * (b - float_7af004) + float_7af00c * (g - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
//                xb = d + (d > 0 ? 0.5f : -0.5f);
//                ib = xb;
//
//                value = clamp(ir >> 16, 0, 255);
//                value |= clamp(ig >> 16, 0, 255) << 16;
//                value |= clamp(ib >> 16, 0, 255) << 8;
//                if (alpha_flag) {
//                    value |= clamp(a >> 16, 0, 255) << 24;
//                } else {
//                    value |= 0xff000000;
//                }
//                memcpy( & out[i * 4],  & value, sizeof(value));
//            }
//            out += stride;
//            inp += 64;
//        }
    }

    @Override
    public Iterator<String> iterator() {
        return engineTextureEntries.keySet().iterator();
    }
}
