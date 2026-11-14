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
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.FileResourceReader;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.ISeekableResourceReader;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Stores the wad file structure and contains the methods to handle the WAD archive<br>
 * The file is LITTLE ENDIAN I might say<br>
 * Converted to JAVA from C code, C code by:
 * <li>Tomasz Lis</li>
 * <li>Anonymous</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class WadFile {

    private final Path file;
    private final SequencedMap<String, WadFileEntry> wadFileEntries;
    private static final String WAD_HEADER_IDENTIFIER = "DWFB";
    private static final int WAD_HEADER_VERSION = 2;

    /**
     * Constructs a new Wad file reader<br>
     * Reads the WAD file structure
     *
     * @param file the wad file to read
     */
    public WadFile(Path file) {
        this.file = file;

        // Read the file
        try (ISeekableResourceReader rawWad = new FileResourceReader(file)) {

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
            wadFileEntries = LinkedHashMap.newLinkedHashMap(files);
            String path = "";
            for (WadFileEntry entry : entries) {
                int offset = entry.getNameOffset() - nameOffset;
                String name = ConversionUtils.toString(Arrays.copyOfRange(nameArray, offset, offset + entry.getNameSize())).trim();

                // The path
                name = PathUtils.convertFileSeparators(name);
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
        try (ISeekableResourceReader rawWad = new FileResourceReader(file)) {

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
    private Path extractFileData(String fileName, String destination, ISeekableResourceReader rawWad) {

        Path destinationFile;
        try {
            destinationFile = WadEntryPathResolver.resolve(Path.of(destination), fileName);
            Files.createDirectories(destinationFile.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to resolve destination for " + fileName + "!", e);
        }

        // Write to the file
        try (OutputStream out = Files.newOutputStream(destinationFile);
                BufferedOutputStream bout = new BufferedOutputStream(out)) {
            bout.write(getFileData(fileName, rawWad));
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
        try (ISeekableResourceReader rawWad = new FileResourceReader(file)) {
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
    private byte[] getFileData(String fileName, ISeekableResourceReader rawWad) throws IOException {
        WadFileEntry fileEntry = wadFileEntries.get(fileName);
        if (fileEntry == null) {
            throw new IOException("File " + fileName + " not found in the WAD archive");
        }

        rawWad.seek(fileEntry.getOffset());
        byte[] compressedData = rawWad.read(fileEntry.getCompressedSize());
        byte[] data = fileEntry.isCompressed()
                ? WadDecompressor.decompress(compressedData, fileName)
                : compressedData;

        // Some compressed DK2 entries leave the archive-table output size at zero.
        if (fileEntry.getSize() > 0 && data.length != fileEntry.getSize()) {
            throw new IOException("WAD entry " + fileName + " declared " + fileEntry.getSize()
                    + " uncompressed bytes but produced " + data.length);
        }
        return data;
    }

    /**
     * Extract a single file
     *
     * @param fileName the file to extract
     * @return the file data
     */
    public byte[] getFileData(String fileName) {

        // Open the WAD for extraction
        try (ISeekableResourceReader rawWad = new FileResourceReader(file)) {
            return getFileData(fileName, rawWad);
        } catch (Exception e) {

            // Fug
            throw new RuntimeException("Failed to read the WAD file!", e);
        }
    }


}
