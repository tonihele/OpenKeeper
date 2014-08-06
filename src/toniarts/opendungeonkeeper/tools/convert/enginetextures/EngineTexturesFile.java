/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.enginetextures;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
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
public class EngineTexturesFile {

    private final File file;
    private List<EngineTextureEntry> entries;
    private boolean decompressionInitialized = false;
    //Decompression stuff I don't understand
    /* external buffers and data to be supplied with sizes */
    private int[] magic_input_table_6c10c0 = new int[64];
    private short[] jump_table_7af4e0 = new short[256];
    private int[] dc_control_table_7af0e0 = new int[224]; //Unsigned
    private int[] magic_output_table = new int[64]; /* magic values computed from magic input */


    public EngineTexturesFile(File file) {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawTextures = new RandomAccessFile(file, "r")) {
            entries = new ArrayList<>();

            //Read the entries
            do {

                //Read the header
                EngineTextureEntry entry = new EngineTextureEntry();
                entry.setResX(Utils.readUnsignedInteger(rawTextures));
                entry.setResY(Utils.readUnsignedInteger(rawTextures));
                entry.setSize(Utils.readUnsignedInteger(rawTextures));
                entry.setsResX(Utils.readUnsignedShort(rawTextures));
                entry.setsResY(Utils.readUnsignedShort(rawTextures));
                entry.setUnknown(Utils.readUnsignedInteger(rawTextures));
                entry.setDataStartLocation(rawTextures.getFilePointer());
                entries.add(entry);

                //Skip the file
                rawTextures.skipBytes(entry.getSize() - 8);
            } while (rawTextures.getFilePointer() != rawTextures.length());
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }

        //Read the names from the DIR file in the same folder
        File dirFile = new File(file.toString().substring(0, file.toString().length() - 3).concat("dir"));
        try (RandomAccessFile rawDir = new RandomAccessFile(dirFile, "r")) {

            // File format:
            // TCHC
            // 8 bytes of unknown
            // int numberOfEntries
            // 1...n null terminated string + 4 bytes of unknown

            //Read the entries
            rawDir.skipBytes(16);
            int i = 0;
            do {
                entries.get(i).setName(Utils.readVaryingLengthStrings(rawDir, 1).get(0));
                rawDir.skipBytes(4);
                i++;
            } while (rawDir.getFilePointer() != rawDir.length());
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + dirFile + "!", e);
        }
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
}
