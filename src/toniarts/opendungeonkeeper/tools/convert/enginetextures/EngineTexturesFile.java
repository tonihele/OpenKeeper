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
    private int[] magic_input_table_6c10c0 = {0x2000, 0x1712, 0x187E, 0x1B37, 0x2000, 0x28BA, 0x3B21, 0x73FC, 0x1712,
        0x10A2, 0x11A8, 0x139F, 0x1712, 0x1D5D, 0x2AA1, 0x539F, 0x187E, 0x11A8,
        0x12BF, 0x14D4, 0x187E, 0x1F2C, 0x2D41, 0x58C5, 0x1B37, 0x139F, 0x14D4,
        0x1725, 0x1B37, 0x22A3, 0x3249, 0x62A3, 0x2000, 0x1712, 0x187E, 0x1B37,
        0x2000, 0x28BA, 0x3B21, 0x73FC, 0x28BA, 0x1D5D, 0x1F2C, 0x22A3, 0x28BA,
        0x33D6, 0x4B42, 0x939F, 0x3B21, 0x2AA1, 0x2D41, 0x3249, 0x3B21, 0x4B42,
        0x6D41, 0x0D650, 0x73FC, 0x539F, 0x58C5, 0x62A3, 0x73FC, 0x939F, 0x0D650,
        0x1A463
    };
    private short[] jump_table_7af4e0 = {0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
        0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
        0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
        0x33, 0x33, 0x33, 0x33, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44,
        0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x55, 0x55,
        0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x66, 0x66, 0x66, 0x66, 0x77,
        0x77, 0x88, 0x0};
    private int[] dc_control_table_7af0e0 = {0x3F, 0x37, 0x3E, 0x3D, 0x36,
        0x2F, 0x27, 0x2E, 0x35, 0x3C,
        0x3B, 0x34, 0x2D, 0x26, 0x1F,
        0x17, 0x1E, 0x25, 0x2C, 0x33,
        0x3A, 0x39, 0x32, 0x2B, 0x24,
        0x1D, 0x16, 0x0F, 0x7, 0x0E,
        0x15, 0x1C, 0x23, 0x2A, 0x31,
        0x38, 0x30, 0x29, 0x22, 0x1B,
        0x14, 0x0D, 0x6, 0x5, 0x0C,
        0x13, 0x1A, 0x21, 0x28, 0x20,
        0x19, 0x12, 0x0B, 0x4, 0x3,
        0x0A, 0x11, 0x18, 0x10, 0x9,
        0x2, 0x1, 0x8, 0x40102, 0x40301,
        0x30201, 0x30201, 0x24100, 0x24100, 0x24100,
        0x24100, 0x20101, 0x20101, 0x20101, 0x20101,
        0x64200, 0x64200, 0x64200, 0x64200, 0x70302,
        0x70302, 0x70A01, 0x70A01, 0x70104, 0x70104,
        0x70901, 0x70901, 0x60801, 0x60801, 0x60801,
        0x60801, 0x60701, 0x60701, 0x60701, 0x60701,
        0x60202, 0x60202, 0x60202, 0x60202, 0x60601,
        0x60601, 0x60601, 0x60601, 0x80E01, 0x80106,
        0x80D01, 0x80C01, 0x80402, 0x80203, 0x80105,
        0x80B01, 0x50103, 0x50103, 0x50103, 0x50103,
        0x50103, 0x50103, 0x50103, 0x50103, 0x50501,
        0x50501, 0x50501, 0x50501, 0x50501, 0x50501,
        0x50501, 0x50501, 0x50401, 0x50401, 0x50401,
        0x50401, 0x50401, 0x50401, 0x50401, 0x50401,
        0x0A1101, 0x0A0602, 0x0A0107, 0x0A0303, 0x0A0204,
        0x0A1001, 0x0A0F01, 0x0A0502, 0x0C010B, 0x0C0902,
        0x0C0503, 0x0C010A, 0x0C0304, 0x0C0802, 0x0C1601,
        0x0C1501, 0x0C0109, 0x0C1401, 0x0C1301, 0x0C0205,
        0x0C0403, 0x0C0108, 0x0C0702, 0x0C1201, 0x0D0B02,
        0x0D0A02, 0x0D0603, 0x0D0404, 0x0D0305, 0x0D0207,
        0x0D0206, 0x0D010F, 0x0D010E, 0x0D010D, 0x0D010C,
        0x0D1B01, 0x0D1A01, 0x0D1901, 0x0D1801, 0x0D1701,
        0x0E011F, 0x0E011E, 0x0E011D, 0x0E011C, 0x0E011B,
        0x0E011A, 0x0E0119, 0x0E0118, 0x0E0117, 0x0E0116,
        0x0E0115, 0x0E0114, 0x0E0113, 0x0E0112, 0x0E0111,
        0x0E0110, 0x0F0128, 0x0F0127, 0x0F0126, 0x0F0125,
        0x0F0124, 0x0F0123, 0x0F0122, 0x0F0121, 0x0F0120,
        0x0F020E, 0x0F020D, 0x0F020C, 0x0F020B, 0x0F020A,
        0x0F0209, 0x0F0208, 0x100212, 0x100211, 0x100210,
        0x10020F, 0x100703, 0x101102, 0x101002, 0x100F02,
        0x100E02, 0x100D02, 0x100C02, 0x102001, 0x101F01,
        0x101E01, 0x101D01, 0x101C01, 0x12121212
    }; //Unsigned, hmm, 007AF0E4
    private int[] magic_output_table = new int[64]; /* magic values computed from magic input */

    private int bs[];//Unsigned
    private long bs_index;//Unsigned
    private long bs_red = 0;//Unsigned
    private long bs_green = 0;//Unsigned
    private long bs_blue = 0;//Unsigned
    private long bs_alpha = 0;//Unsigned
    private long alpha_flag = 0; //Unsigned
    private int[] decompress2_chunk = new int[256]; /* buffers */

    private int[] decompress3_chunk = new int[288];
    private int[] decompress4_chunk = new int[512];
    private float float_7af000 = 1.048576e6f;
    private float float_7af004 = 8.388608e6f;
    private float float_7af008 = 1.169f;
    private float float_7af00c = -8.1300002e-1f;
    private float float_7af010 = -3.91e-1f;
    private float float_7af014 = 1.602f;
    private float float_7af018 = 2.0250001f;
    private int norm_7af038 = 0x5A82799A;
    private float float_7af03c = 5.4119611e-1f;
    private float float_7af040 = 1.306563f;
    private float float_7af044 = 3.8268343e-1f;
    private double double_7af048 = 6.75539944108852e15;

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
//
//    private short[] dd_texture(int[] buf,
//            int stride, short width, short height) {
//        short flag;
//        int x, y;
//        short[] out = new short[1000];
//
//        initialize_dd(buf[1]);
//        flag = new Integer(buf[0]).shortValue();
//        alpha_flag = flag >> 7;
//
//        for (y = 0; y < height; y += 8) {
//            for (x = 0; x < width; x += 8) {
//                decompress_block(out[y * stride + x * 4], stride);
//            }
//        }
//        return out;
//    }
//
//    private void initialize_dd(int[] buf) {
//        bs = buf;
//        bs_index = 0;
//        bs_red = 0;
//        bs_blue = 0;
//        bs_green = 0;
//        bs_alpha = 0;
//    }
//
//    private void decompress_block(short out, int stride) {
//        int[] inp;
//        double d;
//        long xr, xg, xb;
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
//                xr = (long) (d + (d > 0 ? 0.5f : -0.5f));
//                ir = (int) xr;
//                d = float_7af018 * (b - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
//                xg = (long) (d + (d > 0 ? 0.5f : -0.5f));
//                ig = (int) xg;
//                d = float_7af010 * (b - float_7af004) + float_7af00c * (g - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
//                xb = (long) (d + (d > 0 ? 0.5f : -0.5f));
//                ib = (int) xb;
//
//                value = clamp(ir >> 16, 0, 255);
//                value |= clamp(ig >> 16, 0, 255) << 16;
//                value |= clamp(ib >> 16, 0, 255) << 8;
//                if (alpha_flag != 0) {
//                    value |= clamp(a >> 16, 0, 255) << 24;
//                } else {
//                    value |= 0xff000000;
//                }
//                memcpy( & out[i * 4],  & value, sizeof(value));
//            }
//            out += stride;
//            inp += 64;
//        }
//    }
//
//    private void decompress() {
//        short jt_index, jt_value;
//        long bs_pos = bs_index;
//        int value;
//        short blanket_fill;
//
//        /* red */
//        value = 0;
//        jt_index = bs_read(bs_pos, 8);
//
//        jt_value = jump_table_7af4e0[jt_index];
//        bs_pos += jt_value & 0xf;
//        jt_value >>= 4;
//        if (jt_value != 0) {
//            /* value is signed */
//            value = bs_read(bs_pos, jt_value);
//            if ((value & (1 << (jt_value - 1))) == 0) {
//                value -= (1 << jt_value) - 1;
//            }
//
//            bs_pos += jt_value;
//        }
//
//        bs_red += value;
//        blanket_fill = bs_read(bs_pos, 2);
//        if (blanket_fill == 2) {
//            int i, j;
//            bs_pos += 2;
//            for (j = 0; j < 8; j++) {
//                for (i = 0; i < 8; i++) {
//                    decompress4_chunk[j * 64 + i] = bs_red << 16;
//                }
//            }
//            bs_index = bs_pos;
//        } else {
//            int i;
//            bs_index = prepare_decompress(bs_red, bs_pos);
//            for (i = 0; i < 8; i++) {
//                decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
//            }
//            for (i = 0; i < 8; i++) {
//                decompress_func2( & decompress3_chunk[i * 9],  & decompress4_chunk[i * 64]);
//            }
//        }
//
//        bs_pos = bs_index;
//
//        /* green */
//        value = 0;
//        jt_index = bs_read(bs_pos, 8);
//
//        jt_value = jump_table_7af4e0[jt_index];
//        bs_pos += jt_value & 0xf;
//        jt_value >>= 4;
//        if (jt_value != 0) {
//            /* value is signed */
//            value = bs_read(bs_pos, jt_value);
//            if ((value & (1 << (jt_value - 1))) == 0) {
//                value -= (1 << jt_value) - 1;
//            }
//
//            bs_pos += jt_value;
//        }
//
//        bs_green += value;
//        blanket_fill = bs_read(bs_pos, 2);
//        if (blanket_fill == 2) {
//            int i, j;
//            bs_pos += 2;
//            for (j = 0; j < 8; j++) {
//                for (i = 0; i < 8; i++) {
//                    decompress4_chunk[j * 64 + i + 9] = bs_green << 16;
//                }
//            }
//            bs_index = bs_pos;
//        } else {
//            int i;
//            bs_index = prepare_decompress(bs_green, bs_pos);
//            for (i = 0; i < 8; i++) {
//                decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
//            }
//            for (i = 0; i < 8; i++) {
//                decompress_func2( & decompress3_chunk[i * 9],
//                         & decompress4_chunk[i * 64 + 9]);
//            }
//        }
//
//        bs_pos = bs_index;
//
//        /* blue */
//        value = 0;
//        jt_index = bs_read(bs_pos, 8);
//
//        jt_value = jump_table_7af4e0[jt_index];
//        bs_pos += jt_value & 0xf;
//        jt_value >>= 4;
//        if (jt_value != 0) {
//            /* value is signed */
//            value = bs_read(bs_pos, jt_value);
//            if ((value & (1 << (jt_value - 1))) == 0) {
//                value -= (1 << jt_value) - 1;
//            }
//
//            bs_pos += jt_value;
//        }
//
//        bs_blue += value;
//        blanket_fill = bs_read(bs_pos, 2);
//        if (blanket_fill == 2) {
//            int i, j;
//            bs_pos += 2;
//            for (j = 0; j < 8; j++) {
//                for (i = 0; i < 8; i++) {
//                    decompress4_chunk[j * 64 + i + 18] = bs_blue << 16;
//                }
//            }
//            bs_index = bs_pos;
//        } else {
//            int i;
//            bs_index = prepare_decompress(bs_blue, bs_pos);
//            for (i = 0; i < 8; i++) {
//                decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
//            }
//            for (i = 0; i < 8; i++) {
//                decompress_func2( & decompress3_chunk[i * 9],
//                         & decompress4_chunk[i * 64 + 18]);
//            }
//        }
//
//        bs_pos = bs_index;
//
//        /* alpha */
//        if (alpha_flag == 0) {
//            return;
//        }
//        value = 0;
//        jt_index = bs_read(bs_pos, 8);
//
//        jt_value = jump_table_7af4e0[jt_index];
//        bs_pos += jt_value & 0xf;
//        jt_value >>= 4;
//        if (jt_value != 0) {
//            /* value is signed */
//            value = bs_read(bs_pos, jt_value);
//            if ((value & (1 << (jt_value - 1))) == 0) {
//                value -= (1 << jt_value) - 1;
//            }
//
//            bs_pos += jt_value;
//        }
//
//        bs_alpha += value;
//        blanket_fill = bs_read(bs_pos, 2);
//        if (blanket_fill == 2) {
//            int i, j;
//            bs_pos += 2;
//            for (j = 0; j < 8; j++) {
//                for (i = 0; i < 8; i++) {
//                    decompress4_chunk[j * 64 + i + 27] = bs_alpha << 16;
//                }
//            }
//            bs_index = bs_pos;
//        } else {
//            int i;
//            bs_index = prepare_decompress(bs_alpha, bs_pos);
//            for (i = 0; i < 8; i++) {
//                decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
//            }
//            for (i = 0; i < 8; i++) {
//                decompress_func2( & decompress3_chunk[i * 9],
//                         & decompress4_chunk[i * 64 + 27]);
//            }
//        }
//    }

    private int clamp(int n, int min, int max) {
        if (n < min) {
            return min;
        }
        if (n > max) {
            return max;
        }
        return n;
    }

    @Override
    public Iterator<String> iterator() {
        return engineTextureEntries.keySet().iterator();
    }
}
