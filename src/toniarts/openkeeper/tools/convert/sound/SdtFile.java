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
package toniarts.openkeeper.tools.convert.sound;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.utils.PathUtils;

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
     * @param file the sdt file to read
     */
    public SdtFile(File file) {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawSdt = new RandomAccessFile(file, "r")) {

            //Header
            int files = ConversionUtils.readUnsignedInteger(rawSdt);

            //Read the files
            int offset = ConversionUtils.readUnsignedInteger(rawSdt);
            rawSdt.seek(offset);
            sdtFileEntries = new HashMap<>(files);
            for (int i = 0; i < files; i++) {

                //Entries have 40 byte header
                //ISize: integer;
                //Size: integer;
                //Filename: array[0..15] of char;
                //SamplingRate: ushort;
                //Unknown2: byte;
                //type: byte;
                //Unknown3: integer;
                //nSamples: integer;
                //Unknown4: integer;
                SdtFileEntry entry = new SdtFileEntry();
                entry.setIndexSize(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setSize(ConversionUtils.readUnsignedInteger(rawSdt));
                String filename = ConversionUtils.convertFileSeparators(ConversionUtils.bytesToString(rawSdt, 16).trim());
                entry.setSamplingRate(ConversionUtils.readUnsignedShort(rawSdt));
                entry.setUnknown2(ConversionUtils.toUnsignedByte(rawSdt.readByte()));
                entry.setType(ConversionUtils.parseEnum(ConversionUtils.toUnsignedByte(rawSdt.readByte()), SdtFileEntry.SoundType.class));
                entry.setUnknown3(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setnSamples(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setUnknown4(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setDataOffset(rawSdt.getFilePointer());

                //Skip entries of size 0, there seems to be these BLANKs
                if (entry.getSize() == 0) {
                    continue;
                }

                //Fix file extension
                filename = fixFileExtension(filename);

                sdtFileEntries.put(filename, entry);

                //Skip to next one (or to end of file)
                rawSdt.skipBytes(entry.getSize());
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    /**
     * Extract all the files to a given location
     *
     * @param destination destination directory
     */
    public void extractFileData(String destination) {

        //Open the WAD for extraction
        try (RandomAccessFile rawSdt = new RandomAccessFile(file, "r")) {

            for (String fileName : sdtFileEntries.keySet()) {
                extractFileData(fileName, destination, rawSdt);
            }
        } catch (Exception e) {

            //Fug
            throw new RuntimeException("Faile to read the WAD file!", e);
        }
    }

    /**
     * Extract a single file to a given location
     *
     * @param fileName file to extract
     * @param destination destination directory
     * @param rawSdt the opened SDT file
     */
    private void extractFileData(String fileName, String destination, RandomAccessFile rawSdt) {

        //See that the destination is formatted correctly and create it if it does not exist
        String dest = PathUtils.fixFilePath(destination);
        File destinationFolder = new File(dest);
        destinationFolder.mkdirs();
        dest = dest.concat(fileName);

        //Write to the file
        try (OutputStream outputStream = new FileOutputStream(dest)) {
            getFileData(fileName, rawSdt).writeTo(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to " + dest + "!", e);
        }
    }

    /**
     * Extract a single file
     *
     * @param fileName the file to extract
     * @param rawSdt the opened SDT file
     * @return the file data
     */
    private ByteArrayOutputStream getFileData(String fileName, RandomAccessFile rawSdt) {
        ByteArrayOutputStream result = null;

        //Get the file
        SdtFileEntry fileEntry = sdtFileEntries.get(fileName);
        if (fileEntry == null) {
            throw new RuntimeException("File " + fileName + " not found from the SDT archive!");
        }

        try {

            //Seek to the file we want and read it
            rawSdt.seek(fileEntry.getDataOffset());
            byte[] bytes = new byte[fileEntry.getSize()];
            rawSdt.read(bytes);

            result = new ByteArrayOutputStream();
            result.write(bytes);

        } catch (Exception e) {

            //Fug
            throw new RuntimeException("Faile to read the SDT file!", e);
        }

        return result;
    }

    /**
     * Fix the file extensions, since the 16 char limit, it seems that there are
     * broken extensions<br>
     * And WAV extensions, don't change these, however I was unable to play them
     * anyhow
     *
     * @param filename the file name to fix
     * @return fixed file name
     */
    private String fixFileExtension(String filename) {
        if (!filename.toLowerCase().endsWith(".mp2")) {
            if (filename.contains(".")) {

                //Partial extension found
                if (filename.toLowerCase().contains(".w")) {

                    //It is supposed to be a WAV?
                    filename = filename.substring(0, filename.indexOf(".")) + ".wav";
                } else {

                    //Replace with MP2
                    filename = filename.substring(0, filename.indexOf(".")) + ".mp2";
                }
            } else {

                //No extension, make it a MP2
                filename += ".mp2";
            }
        }
        return filename;
    }

    /**
     * Get list of different objects
     *
     * @return list of objects
     */
    public List<String> getFileNamesList() {
        return Arrays.asList(sdtFileEntries.keySet().toArray(new String[sdtFileEntries.size()]));
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
