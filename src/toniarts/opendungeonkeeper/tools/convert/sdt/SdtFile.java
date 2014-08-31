/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.sdt;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import toniarts.opendungeonkeeper.tools.convert.Utils;

/**
 * Stores the SDT file structure and contains the methods to handle the SDT
 * archive<br>
 * SDT files contain the Dungeon Keeper II sounds as MP2 files<br>
 * The file is LITTLE ENDIAN I might say<br>
 * The file structure definition is extracted from Dragon UnPACKer
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SdtFile {

    private final File file;
    private final HashMap<String, SdtFileEntry> sdtFileEntries;

    /**
     * Constructs a new Sdt file reader<br>
     * Reads the SDT file structure
     *
     * @param file the wad file to read
     */
    public SdtFile(File file) {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawSdt = new RandomAccessFile(file, "r")) {

            //Header
            int files = Utils.readUnsignedInteger(rawSdt);

            //Read the files
            int offset = Utils.readUnsignedInteger(rawSdt);
            rawSdt.seek(offset);
            sdtFileEntries = new HashMap<>(files);
            for (int i = 0; i < files; i++) {

                //Entries have 40 byte header
                //ISize: integer;
                //Size: integer;
                //Filename: array[0..15] of char;
                //Unknow1: integer;
                //Unknow2: integer;
                //Unknow3: integer;
                //Unknow4: integer;
                SdtFileEntry entry = new SdtFileEntry();
                entry.setIndexSize(Utils.readUnsignedInteger(rawSdt));
                entry.setSize(Utils.readUnsignedInteger(rawSdt));
                byte[] nameBytes = new byte[16];
                rawSdt.read(nameBytes);
                String filename = Utils.bytesToString(nameBytes).trim();
                entry.setUnknown1(Utils.readUnsignedInteger(rawSdt));
                entry.setUnknown2(Utils.readUnsignedInteger(rawSdt));
                entry.setUnknown3(Utils.readUnsignedInteger(rawSdt));
                entry.setUnknown4(Utils.readUnsignedInteger(rawSdt));
                entry.setDataOffset(rawSdt.getFilePointer());
                sdtFileEntries.put(filename, entry);

                //Skip to next one (or to end of file)
                rawSdt.skipBytes(entry.getSize());
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }
}
