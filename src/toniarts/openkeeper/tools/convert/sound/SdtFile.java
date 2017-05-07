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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.sound.SdtFileEntry.SoundType;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Stores the SDT file structure and contains the methods to handle the SDT
 * archive<br>
 * SDT files contain the Dungeon Keeper II sounds as MP2 and WAV files<br>
 * The file is LITTLE ENDIAN I might say<br>
 * The file structure definition is extracted from Dragon UnPACKer
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class SdtFile {

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("([^\\s]+(\\.(?i)(mp2|wav))$)");

    private final File file;
    private final int count;
    private final int[] entriesOffsets;
    private final SdtFileEntry[] entries;

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
            count = ConversionUtils.readUnsignedInteger(rawSdt);

            //Read the files
            entriesOffsets = new int[count];
            for (int i = 0; i < entriesOffsets.length; i++) {
                entriesOffsets[i] = ConversionUtils.readUnsignedInteger(rawSdt);
            }

            entries = new SdtFileEntry[count];
            for (int i = 0; i < entries.length; i++) {

                SdtFileEntry entry = new SdtFileEntry();
                entry.setHeaderSize(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setDataSize(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setName(ConversionUtils.readString(rawSdt, 16).trim());
                entry.setSampleRate(ConversionUtils.readUnsignedShort(rawSdt));
                entry.setBitsPerSample(ConversionUtils.toUnsignedByte(rawSdt.readByte()));
                entry.setType(ConversionUtils.parseEnum(ConversionUtils.toUnsignedByte(rawSdt.readByte()),
                        SdtFileEntry.SoundType.class));
                entry.setUnknown3(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setnSamples(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setUnknown4(ConversionUtils.readUnsignedInteger(rawSdt));
                entry.setDataOffset(rawSdt.getFilePointer());

                //Skip entries of size 0, there seems to be these BLANKs
                if (entry.getDataSize() == 0) {
                    continue;
                }

                entries[i] = entry;

                //Skip to next one (or to end of file)
                rawSdt.skipBytes(entry.getDataSize());
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

        //Open the SDT for extraction
        try (RandomAccessFile rawSdt = new RandomAccessFile(file, "r")) {
            for (SdtFileEntry entry : entries) {
                extractFileData(entry, destination, rawSdt);
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
    private void extractFileData(SdtFileEntry entry, String destination, RandomAccessFile rawSdt) {
        if (entry == null) {
            return;
        }
        //Fix file extension
        String filename = fixFileExtension(entry);
        //See that the destination is formatted correctly and create it if it does not exist
        String dest = PathUtils.fixFilePath(destination);
        File destinationFolder = new File(dest);
        destinationFolder.mkdirs();

        dest = dest.concat(filename);

        //Write to the file
        try (OutputStream outputStream = new FileOutputStream(dest)) {
            getFileData(entry, rawSdt).writeTo(outputStream);
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
    private ByteArrayOutputStream getFileData(SdtFileEntry fileEntry, RandomAccessFile rawSdt) throws IOException {

        //Get the file
        if (fileEntry == null) {
            throw new RuntimeException("File entry is null");
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        // add file header if needed
        result.write(getFileHeader(fileEntry));
        //Seek to the file we want and read it
        rawSdt.seek(fileEntry.getDataOffset());
        byte[] bytes = new byte[fileEntry.getDataSize()];
        rawSdt.read(bytes);
        result.write(bytes);

        return result;
    }

    /**
     * Fix the file extensions, since the 16 char limit, it seems that there are
     * broken extensions
     *
     * @param entry the SDT entry
     * @return fixed file name
     */
    public static String fixFileExtension(SdtFileEntry entry) {
        String filename = ConversionUtils.convertFileSeparators(entry.getName());
        Matcher m = FILE_EXTENSION_PATTERN.matcher(filename.toLowerCase());
        if (!m.find()) {
            int index = filename.lastIndexOf(".");
            if (index > -1) {
                // Strip the partial extension
                filename = filename.substring(0, index);
            }
            // Add the extension
            filename += "." + entry.getType().getExtension();
        }

        return filename;
    }

    private byte[] getBytes(int value, int size) {
        byte[] result = new byte[size];

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) ((value >> (i * Byte.SIZE)) & 0xFF);
        }

        return result;
    }

    private byte[] getFileHeader(SdtFileEntry entry) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        short numChannels;
        if (entry.getType() == SoundType.WAV_MONO) {
            numChannels = 1;
        } else if (entry.getType() == SoundType.WAV_STEREO) {
            numChannels = 2;
        } else {
            // no need header
            return new byte[0];
        }
        short audioFormat = 1; // 1 = PCM
        int chunkSize = 44 - 8 + entry.getDataSize(); //chunkSize
        int subchunkFmtSize = 16; // subchunk1Size. For format PCM

        stream.write("RIFF".getBytes()); // chunkId
        stream.write(getBytes(chunkSize, 4));
        stream.write("WAVE".getBytes()); // format
        stream.write("fmt ".getBytes()); // subchunk1Id
        stream.write(getBytes(subchunkFmtSize, 4));
        stream.write(getBytes(audioFormat, 2));
        stream.write(getBytes(numChannels, 2));
        stream.write(getBytes(entry.getSampleRate(), 4)); // sampleRate
        stream.write(getBytes(entry.getSampleRate() * numChannels * entry.getBitsPerSample() / Byte.SIZE, 4)); // byteRate
        stream.write(getBytes(numChannels * entry.getBitsPerSample() / Byte.SIZE, 2)); // blockAlign
        stream.write(getBytes(entry.getBitsPerSample(), 2)); // bitsPerSample
        stream.write("data".getBytes()); // subchunk2Id
        stream.write(getBytes(entry.getDataSize(), 4)); // subchunk2Size

        return stream.toByteArray();
    }

    /**
     * Get array of entires
     *
     * @return array of entires
     */
    public SdtFileEntry[] getEntires() {
        return entries;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return file.getName();
    }
}
