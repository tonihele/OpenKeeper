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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.Arrays;
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
    private int[] dc_control_table_7af0e0 = {0x3F, 0x0, 0x37, 0x0, 0x3E, 0x0, 0x3D, 0x0,
        0x36, 0x0, 0x2F, 0x0, 0x27, 0x0, 0x2E, 0x0,
        0x35, 0x0, 0x3C, 0x0, 0x3B, 0x0, 0x34, 0x0,
        0x2D, 0x0, 0x26, 0x0, 0x1F, 0x0, 0x17, 0x0,
        0x1E, 0x0, 0x25, 0x0, 0x2C, 0x0, 0x33, 0x0,
        0x3A, 0x0, 0x39, 0x0, 0x32, 0x0, 0x2B, 0x0,
        0x24, 0x0, 0x1D, 0x0, 0x16, 0x0, 0x0F, 0x0,
        0x7, 0x0, 0x0E, 0x0, 0x15, 0x0, 0x1C, 0x0,
        0x23, 0x0, 0x2A, 0x0, 0x31, 0x0, 0x38, 0x0,
        0x30, 0x0, 0x29, 0x0, 0x22, 0x0, 0x1B, 0x0,
        0x14, 0x0, 0x0D, 0x0, 0x6, 0x0, 0x5, 0x0,
        0x0C, 0x0, 0x13, 0x0, 0x1A, 0x0, 0x21, 0x0,
        0x28, 0x0, 0x20, 0x0, 0x19, 0x0, 0x12, 0x0,
        0x0B, 0x0, 0x4, 0x0, 0x3, 0x0, 0x0A, 0x0,
        0x11, 0x0, 0x18, 0x0, 0x10, 0x0, 0x9, 0x0,
        0x2, 0x0, 0x1, 0x0, 0x8, 0x0, 0x102, 0x4,
        0x301, 0x4, 0x201, 0x3, 0x201, 0x3, 0x4100, 0x2,
        0x4100, 0x2, 0x4100, 0x2, 0x4100, 0x2, 0x101, 0x2,
        0x101, 0x2, 0x101, 0x2, 0x101, 0x2, 0x4200, 0x6,
        0x4200, 0x6, 0x4200, 0x6, 0x4200, 0x6, 0x302, 0x7,
        0x302, 0x7, 0x0A01, 0x7, 0x0A01, 0x7, 0x104, 0x7,
        0x104, 0x7, 0x901, 0x7, 0x901, 0x7, 0x801, 0x6,
        0x801, 0x6, 0x801, 0x6, 0x801, 0x6, 0x701, 0x6,
        0x701, 0x6, 0x701, 0x6, 0x701, 0x6, 0x202, 0x6,
        0x202, 0x6, 0x202, 0x6, 0x202, 0x6, 0x601, 0x6,
        0x601, 0x6, 0x601, 0x6, 0x601, 0x6, 0x0E01, 0x8,
        0x106, 0x8, 0x0D01, 0x8, 0x0C01, 0x8, 0x402, 0x8,
        0x203, 0x8, 0x105, 0x8, 0x0B01, 0x8, 0x103, 0x5
    }; //Unsigned, hmm, 007AF0E4
    private int[] magic_output_table = new int[64]; /* magic values computed from magic input */

    private long bs[];//Unsigned
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
                    entry.setDataStartLocation(rawTextures.getFilePointer());
                    entry.setUnknown(Utils.readUnsignedInteger(rawTextures));

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
            int count = (engineTextureEntry.getSize() - 4) / 4;
            long[] buf = new long[count]; // -4 since 2x shorts belongs to the size, but our start location is past those
            for (int i = 0; i < count; i++) {
                buf[i] = Utils.readUnsignedIntegerAsLong(rawTextures);
            }

            result = new ByteArrayOutputStream();

            //We should decompress the texture
            if (DECOMPRESSION_ENABLED) {
                dd_init();
                dd_texture(buf, engineTextureEntry.getResX() * (32 / 8), engineTextureEntry.getResX(), engineTextureEntry.getResY());
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

    private short[] dd_texture(long[] buf,
            int stride, int width, int height) {
        short flag;
        int x, y;
        ByteBuffer out = ByteBuffer.allocate(width * height * 4);

        initialize_dd(Arrays.copyOfRange(buf, 1, buf.length));
        flag = new Long(buf[0]).byteValue(); //
        alpha_flag = flag >> 7;

        for (y = 0; y < height; y += 8) {
            for (x = 0; x < width; x += 8) {
                out.position(y * stride + x * 4);
                decompress_block(out, stride);
            }
        }
        return out.asShortBuffer().array();
    }

    private void initialize_dd(long[] buf) {
        bs = buf;
        bs_index = 0;
        bs_red = 0;
        bs_blue = 0;
        bs_green = 0;
        bs_alpha = 0;
    }

    private void decompress_block(ByteBuffer out, int stride) {
        IntBuffer inp;
        double d;
        long xr, xg, xb;
        int ir, ig, ib;

        int a;
        float r, g, b;
        int i, j;

        decompress();

        inp = IntBuffer.wrap(decompress4_chunk);
        for (j = 0; j < 8; j++) {
            for (i = 0; i < 8; i++) {
                int value;
                r = inp.get(inp.position() + i + 0);
                g = inp.get(inp.position() + i + 18);
                b = inp.get(inp.position() + i + 9);
                a = inp.get(inp.position() + i + 27);
                d = float_7af014 * (g - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
                xr = (long) (d + (d > 0 ? 0.5f : -0.5f));
                ir = (int) xr;
                d = float_7af018 * (b - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
                xg = (long) (d + (d > 0 ? 0.5f : -0.5f));
                ig = (int) xg;
                d = float_7af010 * (b - float_7af004) + float_7af00c * (g - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
                xb = (long) (d + (d > 0 ? 0.5f : -0.5f));
                ib = (int) xb;

                value = clamp(ir >> 16, 0, 255);
                value |= clamp(ig >> 16, 0, 255) << 16;
                value |= clamp(ib >> 16, 0, 255) << 8;
                if (alpha_flag != 0) {
                    value |= clamp(a >> 16, 0, 255) << 24;
                } else {
                    value |= 0xff000000;
                }
                out.putInt(out.position() + i * 4, value);
                //memcpy( & out[i * 4],  & value, sizeof(value));
            }
            out.position(out.position() + stride);
            //out += stride;
            inp.position(inp.position() + 64);
            //inp += 64;
        }
    }

    private void decompress() {
        int jt_index, jt_value;
        int bs_pos = (int) bs_index;
        int value;
        int blanket_fill;

        /* red */
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_red += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i] = (int) bs_red << 16;
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_red, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
                //decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64, decompress4_chunk.length - i * 64));
                //decompress_func2( & decompress3_chunk[i * 9],  & decompress4_chunk[i * 64]);
            }
        }

        bs_pos = (int) bs_index;

        /* green */
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_green += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i + 9] = (int) (bs_green << 16);
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_green, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
//                decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64 + 9, decompress4_chunk.length - (i * 64 + 9)));
//                decompress_func2( & decompress3_chunk[i * 9],
//                         & decompress4_chunk[i * 64 + 9]);
            }
        }

        bs_pos = (int) bs_index;

        /* blue */
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_blue += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i + 18] = (int) (bs_blue << 16);
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_blue, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
//                decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64 + 18, decompress4_chunk.length - (i * 64 + 18)));
//                decompress_func2( & decompress3_chunk[i * 9],
//                         & decompress4_chunk[i * 64 + 18]);
            }
        }

        bs_pos = (int) bs_index;

        /* alpha */
        if (alpha_flag == 0) {
            return;
        }
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_alpha += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i + 27] = (int) (bs_alpha << 16);
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_alpha, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
//                decompress_func1( & decompress2_chunk[i * 8],  & decompress3_chunk[i]);
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64 + 27, decompress4_chunk.length - (i * 64 + 27)));
//                decompress_func2( & decompress3_chunk[i * 9],
//                         & decompress4_chunk[i * 64 + 27]);
            }
        }
    }

    private int clamp(int n, int min, int max) {
        if (n < min) {
            return min;
        }
        if (n > max) {
            return max;
        }
        return n;
    }

    private long bs_read(int pos, int bits) {
        int w1, w2;
        int word_index;
        int shamt;

        word_index = pos >> 5;
        shamt = pos & 0x1f;
        w1 = (int) bs[word_index] << shamt;
        w2 = shamt != 0 ? (int) bs[word_index + 1] >> (32 - shamt) : 0;
        w1 |= w2;
        w1 >>= (32 - bits);

        return w1;
    }

    private long prepare_decompress(int value, int pos) {
        int xindex = 0, index = 0, control_word = 0;
        short magic_index = 0x3f;
        boolean areWeDone = false;

        decompress2_chunk[0] = value * magic_output_table[0];
        Arrays.fill(decompress2_chunk, 1, decompress2_chunk.length, 0);
        //memset( & decompress2_chunk[1], 0,
        //        sizeof(decompress2_chunk) - sizeof(uint32_t));

        loop:
        {
            if (!areWeDone) {
                xindex = index = (int) bs_read(pos, 17);
            }
            if (index >= 0x8000 || areWeDone) {
                int out_index;
                if (!areWeDone) {
                    index >>= 13;
                    control_word = dc_control_table_7af0e0[60 + index];
                }
                are_we_done:
                {
                    areWeDone = false;
                    if ((control_word & 0xff00) == 0x4100) {
                        return pos + (control_word >> 16);
                        //goto done;
                    }
                    if ((control_word & 0xff00) > 0x4100) {
                        int unk14;
                        /* read next control */
                        pos += control_word >> 16;
                        unk14 = (int) bs_read(pos, 14);
                        pos += 14;
                        magic_index -= (unk14 & 0xff00) >> 8;
                        unk14 &= 0xff;
                        if (unk14 != 0) {
                            if (unk14 != 0x80) {
                                if (unk14 > 0x80) {
                                    unk14 -= 0x100;
                                }
                                magic_index--;
                            } else {
                                unk14 = (int) bs_read(pos, 8);
                                pos += 8;
                                unk14 -= 0x100;
                            }
                        } else {
                            unk14 = (int) bs_read(pos, 8);
                            pos += 8;
                        }
                        control_word = unk14;
                    } else {
                        int bit_to_test;
                        int rem = control_word >> 16;
                        int xoramt = 0;

                        magic_index -= (control_word & 0xff00) >> 8;
                        bit_to_test = 16 - rem;
                        if ((xindex & (1 << bit_to_test)) != 0) {
                            xoramt = ~0;
                        }
                        control_word &= 0xff;
                        control_word ^= xoramt;
                        pos++;
                        control_word -= xoramt;
                        pos += rem;
                    }
                    out_index = dc_control_table_7af0e0[magic_index + 1];
                    decompress2_chunk[out_index] = (control_word) * magic_output_table[out_index];
                    break loop;
                }
            } else if (index >= 0x800) {
                index >>= 9;
                control_word = dc_control_table_7af0e0[72 + index];
                areWeDone = true;
                break loop;
//                goto are_we_done;
            } else if (index >= 0x400) {
                index >>= 7;
                control_word = dc_control_table_7af0e0[128 + index];
                areWeDone = true;
                break loop;
//                goto are_we_done;
            } else if (index >= 0x200) {
                index >>= 5;
                control_word = dc_control_table_7af0e0[128 + index];
                areWeDone = true;
                break loop;
//                goto are_we_done;
            } else if (index >= 0x100) {
                index >>= 4;
                control_word = dc_control_table_7af0e0[144 + index];
                areWeDone = true;
                break loop;
//                goto are_we_done;
            } else if (index >= 0x80) {
                index >>= 3;
                control_word = dc_control_table_7af0e0[160 + index];
                areWeDone = true;
                break loop;
//                goto are_we_done;
            } else if (index >= 0x40) {
                index >>= 2;
                control_word = dc_control_table_7af0e0[176 + index];
                areWeDone = true;
                break loop;
//                goto are_we_done;
            } else if (index >= 0x20) {
                index >>= 1;
                control_word = dc_control_table_7af0e0[192 + index];
                areWeDone = true;
                break loop;
//                goto are_we_done;
            }
        }
        //done:
        //return pos + (control_word >> 16);
        return 0;
    }

    private void decompress_func1(IntBuffer in, IntBuffer out) {
        long rx;
        int sa;
        int b, a, c, d, i, p, s;
        long sc, sd, si;
        int ra, rb;
        double rxf, rxg, rxs;
        double xf, xg;

        if ((in.get(in.position() + 1) | in.get(in.position() + 2) | in.get(in.position() + 3) | in.get(in.position() + 4) | in.get(in.position() + 6) | in.get(in.position() + 7)) == 0) {
            a = in.get();
            out.put(out.position() + 0, a);
            out.put(out.position() + 9, a);
            out.put(out.position() + 18, a);
            out.put(out.position() + 27, a);
            out.put(out.position() + 36, a);
            out.put(out.position() + 45, a);
            out.put(out.position() + 54, a);
            out.put(out.position() + 63, a);
            return;
        }

        b = in.get(in.position() + 5) - in.get(in.position() + 3);
        c = in.get(in.position() + 1) - in.get(in.position() + 7);
        i = in.get(in.position() + 3) + in.get(in.position() + 5);
        a = in.get(in.position() + 7) + in.get(in.position() + 1);
        xf = b;
        xg = c;
        p = i + a;
        a -= i;

        rxs = xg + xf;
        rxf = xf * float_7af03c + float_7af044 * rxs;
        rxg = xg * float_7af040 - float_7af044 * rxs;
        ra = (int) (rxf + (rxf > 0 ? 0.5f : -0.5f));
        rb = (int) (rxg + (rxg > 0 ? 0.5f : -0.5f));

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        a = (int) rx;
        d = (int) (rx >> 32);

        b = in.get(in.position() + 6);
        d += d;
        a = in.get(in.position() + 2);

        c = ra;
        i = rb;
        c += d;
        d += i;
        i += p;
        sc = c;
        sd = d;
        si = i;
        c = in.get(in.position() + 0);
        d = in.get(in.position() + 4);
        s = b + a;
        a -= b;
        b = d + c;
        c -= d;

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        a = (int) rx;
        d = (int) (rx >> 32);

        d += d;
        out.put(out.position() + 18, (int) ((c - d) + sc));
        out.put(out.position() + 45, (int) ((c - d) - sc));
        out.put(out.position() + 27, (b - (s + d)) + ra);
        out.put(out.position() + 36, (b - (s + d)) - ra);
        out.put(out.position() + 0, (int) ((s + d) + b + si));
        out.put(out.position() + 9, (int) (sd + d + c));
        out.put(out.position() + 54, (int) (d + c - sd));
        out.put(out.position() + 63, (int) ((s + d) + b - si));
    }

    private void decompress_func2(IntBuffer in, IntBuffer out) {
        long rx;
        int sa;
        int b, a, c, d, i, p, s;
        long sc, sd, si;
        int ra, rb;
        double rxf, rxg, rxs;
        double xf, xg;

        b = in.get(in.position() + 5) - in.get(in.position() + 3);
        c = in.get(in.position() + 1) - in.get(in.position() + 7);
        i = in.get(in.position() + 3) + in.get(in.position() + 5);
        a = in.get(in.position() + 7) + in.get(in.position() + 1);
        xf = b;
        xg = c;
        p = i + a;
        a -= i;

        rxs = xg + xf;
        rxf = xf * float_7af03c + float_7af044 * rxs;
        rxg = xg * float_7af040 - float_7af044 * rxs;
        ra = (int) (rxf + (rxf > 0 ? 0.5f : -0.5f));
        rb = (int) (rxg + (rxg > 0 ? 0.5f : -0.5f));

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        a = (int) rx;
        d = (int) (rx >> 32);

        b = in.get(in.position() + 6);
        d += d;
        a = in.get(in.position() + 2);

        c = ra;
        i = rb;
        c += d;
        d += i;
        i += p;
        sc = c;
        sd = d;
        si = i;
        c = in.get(in.position() + 0);
        d = in.get(in.position() + 4);
        s = b + a;
        a -= b;
        b = d + c;
        c -= d;

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        a = (int) rx;
        d = (int) (rx >> 32);

        d += d;
        p = (int) sc;
        s += d;
        a = d + c;
        c -= d;
        d = s + b;
        b -= s;
        s = c + p;
        c -= p;
        p = ra;
        out.put(out.position() + 2, s);
        s = (int) sd;
        out.put(out.position() + 5, c);
        c = b + p;
        b -= p;
        p = (int) si;
        out.put(out.position() + 3, c);
        out.put(out.position() + 4, b);
        b = s + a;
        a -= s;
        c = d + p;
        d -= p;
        out.put(out.position() + 0, c);
        out.put(out.position() + 1, b);
        out.put(out.position() + 6, a);
        out.put(out.position() + 7, d);
    }

    @Override
    public Iterator<String> iterator() {
        return engineTextureEntries.keySet().iterator();
    }
}
