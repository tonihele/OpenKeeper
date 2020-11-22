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
package toniarts.openkeeper.tools.convert.wad;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.IResourceReader;
import toniarts.openkeeper.tools.convert.ResourceReader;

/**
 * Stores the wad file structure and contains the methods to handle the WAD archive<br>
 * The file is LITTLE ENDIAN I might say<br>
 * Converted to JAVA from C code, C code by:
 * <li>Tomasz Lis</li>
 * <li>Anonymous</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WadFile {

    private final Path file;
    private final Map<String, WadFileEntry> wadFileEntries;
    private static final String WAD_HEADER_IDENTIFIER = "DWFB";
    private static final int WAD_HEADER_VERSION = 2;

    private static final Logger LOGGER = Logger.getLogger(WadFile.class.getName());

    /**
     * Constructs a new Wad file reader<br>
     * Reads the WAD file structure
     *
     * @param file the wad file to read
     */
    public WadFile(Path file) {
        this.file = file;

        // Read the file
        try (IResourceReader rawWad = new ResourceReader(file)) {

            // Check the header
            IResourceChunkReader reader = rawWad.readChunk(8);
            String header = reader.readString(4);
            if (!WAD_HEADER_IDENTIFIER.equals(header)) {
                throw new RuntimeException("Header should be " + WAD_HEADER_IDENTIFIER + " and it was " + header + "! Cancelling!");
            }

            // See the version
            int version = reader.readUnsignedInteger();
            if (WAD_HEADER_VERSION != version) {
                throw new RuntimeException("Version header should be " + WAD_HEADER_VERSION + " and it was " + version + "! Cancelling!");
            }

            // Seek
            rawWad.seek(0x48);
            reader = rawWad.readChunk(16);

            int files = reader.readUnsignedInteger();
            int nameOffset = reader.readUnsignedInteger();
            int nameSize = reader.readUnsignedInteger();
            int unknown = reader.readUnsignedInteger();

            // Loop through the file count
            reader = rawWad.readChunk(40 * files);
            List<WadFileEntry> entries = new ArrayList<>(files);
            for (int i = 0; i < files; i++) {
                WadFileEntry wadInfo = new WadFileEntry();
                wadInfo.setUnk1(reader.readUnsignedInteger());
                wadInfo.setNameOffset(reader.readUnsignedInteger());
                wadInfo.setNameSize(reader.readUnsignedInteger());
                wadInfo.setOffset(reader.readUnsignedInteger());
                wadInfo.setCompressedSize(reader.readUnsignedInteger());
                wadInfo.setType(reader.readIntegerAsEnum(WadFileEntry.WadFileEntryType.class));
                wadInfo.setSize(reader.readUnsignedInteger());
                int[] unknown2 = new int[3];
                unknown2[0] = reader.readUnsignedInteger();
                unknown2[1] = reader.readUnsignedInteger();
                unknown2[2] = reader.readUnsignedInteger();
                wadInfo.setUnknown2(unknown2);
                entries.add(wadInfo);
            }

            // Read the file names and put them to a hashmap
            // If the file has a path, carry that path all the way to next entry with path
            // The file names itself aren't unique, but with the path they are
            rawWad.seek(nameOffset);
            byte[] nameArray = rawWad.read(nameSize);
            wadFileEntries = new LinkedHashMap<>(files);
            String path = "";
            for (WadFileEntry entry : entries) {
                int offset = entry.getNameOffset() - nameOffset;
                String name = ConversionUtils.toString(Arrays.copyOfRange(nameArray, offset, offset + entry.getNameSize())).trim();

                // The path
                name = ConversionUtils.convertFileSeparators(name);
                int index = name.lastIndexOf(File.separator);
                if (index > -1) {
                    path = name.substring(0, index + 1);
                } else if (!path.isEmpty()) {
                    name = path + name;
                }

                wadFileEntries.put(name, entry);
            }
        } catch (IOException e) {

            //Fug
            throw new RuntimeException("Failed to open the file " + file + "!", e);
        }
    }

    /**
     * Get the individual file names as a list
     *
     * @return list of the file names
     */
    public List<String> getWadFileEntries() {
        return new ArrayList(wadFileEntries.keySet());
    }

    /**
     * Return the file count in this WAD archive
     *
     * @return file entries count
     */
    public int getWadFileEntryCount() {
        return wadFileEntries.size();
    }

    /**
     * Extract all the files to a given location
     *
     * @param destination destination directory
     */
    public void extractFileData(String destination) {

        // Open the WAD for extraction
        try (IResourceReader rawWad = new ResourceReader(file)) {

            for (String fileName : wadFileEntries.keySet()) {
                extractFileData(fileName, destination, rawWad);
            }
        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to read the WAD file!", e);
        }
    }

    /**
     * Extract a single file to a given location
     *
     * @param fileName file to extract
     * @param destination destination directory
     * @param rawWad the opened WAD file
     */
    private Path extractFileData(String fileName, String destination, IResourceReader rawWad) {

        // See that the destination is formatted correctly and create it if it does not exist
        Path destinationFile = Paths.get(destination, fileName);

        try {
            Files.createDirectories(destinationFile.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create destination folder to " + destinationFile + "!", e);
        }

        // Write to the file
        try (FileOutputStream out = new FileOutputStream(destinationFile.toFile());
                BufferedOutputStream bout = new BufferedOutputStream(out)) {
            getFileData(fileName, rawWad).writeTo(bout);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to " + destinationFile + "!", e);
        }

        return destinationFile;
    }

    /**
     * Extract a single file to a given location
     *
     * @param fileName file to extract
     * @param destination destination directory
     * @return the file for the extracted contents
     */
    public Path extractFileData(String fileName, String destination) {

        // Open the WAD for extraction
        try (IResourceReader rawWad = new ResourceReader(file)) {
            return extractFileData(fileName, destination, rawWad);
        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to read the WAD file!", e);
        }
    }

    /**
     * Extract a single file
     *
     * @param fileName the file to extract
     * @param rawWad the opened WAD file
     * @return the file data
     */
    private ByteArrayOutputStream getFileData(String fileName, IResourceReader rawWad) {
        ByteArrayOutputStream result = null;

        // Get the file
        WadFileEntry fileEntry = wadFileEntries.get(fileName);
        if (fileEntry == null) {
            throw new RuntimeException("File " + fileName + " not found from the WAD archive!");
        }

        try {

            // Seek to the file we want and read it
            rawWad.seek(fileEntry.getOffset());
            byte[] bytes = rawWad.read(fileEntry.getCompressedSize());

            result = new ByteArrayOutputStream();

            // See if the file is compressed
            if (fileEntry.isCompressed()) {
                result.write(decompressFileData(bytes, fileName));
            } else {
                result.write(bytes);
            }

        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to read the WAD file!", e);
        }

        return result;
    }

    /**
     * Extract a single file
     *
     * @param fileName the file to extract
     * @return the file data
     */
    public ByteArrayOutputStream getFileData(String fileName) {

        // Open the WAD for extraction
        try (IResourceReader rawWad = new ResourceReader(file)) {
            return getFileData(fileName, rawWad);
        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to read the WAD file!", e);
        }
    }

    /**
     * Some file entries in the WAD are compressed (type 4?), this decompresses the file data
     *
     * @param bytes the compressed bytes
     * @param fileName just for logging
     * @return the decompressed bytes
     */
    private byte[] decompressFileData(byte[] src, String fileName) {
        int i = 0, j = 0;
        if ((src[i++] & 1) != 0) {
            i += 3;
        }
        i++; // <<skip second byte
        // <decompressed size packed into 3 bytes

        int decsize = (ConversionUtils.toUnsignedByte(src[i]) << 16) + (ConversionUtils.toUnsignedByte(src[i + 1]) << 8) + ConversionUtils.toUnsignedByte(src[i + 2]);
        byte[] dest = new byte[decsize];
        i += 3;
        byte flag; // The flag byte read at the beginning of each main loop iteration
        int counter; // Counter for all loops
        boolean finished = false;
        while (!finished) {
            if (i >= src.length) {
                break;
            }
            flag = src[i++]; // Get flag byte
            if ((ConversionUtils.toUnsignedByte(flag) & 0x80) == 0) {
                byte tmp = src[i++];
                counter = ConversionUtils.toUnsignedByte(flag) & 3; // mod 4
                while (counter-- != 0) // Copy literally
                {
                    dest[j] = src[i++];
                    j++;
                }
                int k = j; // Get the destbuf position
                k -= (ConversionUtils.toUnsignedByte(flag) & 0x60) << 3;
                k -= ConversionUtils.toUnsignedByte(tmp);
                k--;

                counter = ((ConversionUtils.toUnsignedByte(flag) >> 2) & 7) + 2;
                do {
                    dest[j] = dest[k++];
                    j++;
                } while (counter-- != 0); // Correct decrement
            } else if ((ConversionUtils.toUnsignedByte(flag) & 0x40) == 0) {
                byte tmp = src[i++];
                byte tmp2 = src[i++];
                counter = (ConversionUtils.toUnsignedByte(tmp)) >> 6;
                while (counter-- != 0) // Copy literally
                {
                    dest[j] = src[i++];
                    j++;
                }
                int k = j;
                k -= (ConversionUtils.toUnsignedByte(tmp) & 0x3F) << 8;
                k -= ConversionUtils.toUnsignedByte(tmp2);
                k--;
                counter = (ConversionUtils.toUnsignedByte(flag) & 0x3F) + 3;
                do {
                    dest[j] = dest[k++];
                    j++;
                } while (counter-- != 0); // Correct postfix decrement
            } else if ((ConversionUtils.toUnsignedByte(flag) & 0x20) == 0) {
                byte localtemp = src[i++];
                byte tmp2 = src[i++];
                byte tmp3 = src[i++];
                counter = ConversionUtils.toUnsignedByte(flag) & 3;
                while (counter-- != 0) // Copy literally
                {
                    dest[j] = src[i++];
                    j++;
                }
                int k = j;
                k -= (ConversionUtils.toUnsignedByte(flag) & 0x10) << 12;
                k -= ConversionUtils.toUnsignedByte(localtemp) << 8;
                k -= ConversionUtils.toUnsignedByte(tmp2);
                k--;
                counter = ConversionUtils.toUnsignedByte(tmp3) + ((ConversionUtils.toUnsignedByte(flag) & 0x0C) << 6) + 4;
                do {
                    dest[j] = dest[k++];
                    j++;
                } while (counter-- != 0); // Correct
            } else {
                counter = (ConversionUtils.toUnsignedByte(flag) & 0x1F) * 4 + 4;
                if (ConversionUtils.toUnsignedByte(Integer.valueOf(counter).byteValue()) > 0x70) {
                    finished = true;

                    // Crepare to copy the last bytes
                    counter = ConversionUtils.toUnsignedByte(flag) & 3;
                }
                while (counter-- != 0) { // Copy literally
                    dest[j] = src[i++];
                    j++;
                }
            }
        } // Of while()
        if (!finished) {
            LOGGER.log(Level.WARNING, "File {0} might not be successfully extracted!", fileName);
        }
        return dest;
    }
}
