/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert.wad;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import toniarts.opendungeonkeeper.tools.convert.Utils;

/**
 * Stores the wad file structure and contains the methods to handle the WAD
 * archive<br>
 * The file is LITTLE ENDIAN I might say<br>
 * Converted to JAVA from C code, C code by:
 * <li>Tomasz Lis</li>
 * <li>Anonymous</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WadFile {

    private final File file;
    private final LinkedHashMap<String, WadFileEntry> wadFileEntries;
    private static final String WAD_HEADER_IDENTIFIER = "DWFB";
    private static final int WAD_HEADER_VERSION = 2;
    private String subdir = "";

    /**
     * Constructs a new Wad file reader<br>
     * Reads the WAD file structure
     *
     * @param file the wad file to read
     */
    public WadFile(File file) {
        this.file = file;

        //Read the file
        try (RandomAccessFile rawWad = new RandomAccessFile(file, "r")) {

            //Check the header
            byte[] header = new byte[4];
            rawWad.read(header);
            if (!WAD_HEADER_IDENTIFIER.equals(Utils.bytesToString(header))) {
                throw new RuntimeException("Header should be " + WAD_HEADER_IDENTIFIER + " and it was " + header + "! Cancelling!");
            }

            //See the version
            int version = Utils.readUnsignedInteger(rawWad);
            if (WAD_HEADER_VERSION != version) {
                throw new RuntimeException("Version header should be " + WAD_HEADER_VERSION + " and it was " + version + "! Cancelling!");
            }

            //Seek
            rawWad.seek(0x48);

            int files = Utils.readUnsignedInteger(rawWad);
            int nameOffset = Utils.readUnsignedInteger(rawWad);
            int nameSize = Utils.readUnsignedInteger(rawWad);
            int unknown = Utils.readUnsignedInteger(rawWad);

            //Loop through the file count
            List<WadFileEntry> entries = new ArrayList<>(files);
            for (int i = 0; i < files; i++) {
                WadFileEntry wadInfo = new WadFileEntry();
                wadInfo.setUnk1(Utils.readUnsignedInteger(rawWad));
                wadInfo.setNameOffset(Utils.readUnsignedInteger(rawWad));
                wadInfo.setNameSize(Utils.readUnsignedInteger(rawWad));
                wadInfo.setOffset(Utils.readUnsignedInteger(rawWad));
                wadInfo.setCompressedSize(Utils.readUnsignedInteger(rawWad));
                int typeIndex = Utils.readUnsignedInteger(rawWad);
                switch (typeIndex) {
                    case 0: {
                        wadInfo.setType(WadFileEntry.WadFileEntryType.NOT_COMPRESSED);
                        break;
                    }
                    case 4: {
                        wadInfo.setType(WadFileEntry.WadFileEntryType.COMPRESSED);
                        break;
                    }
                    default: {
                        wadInfo.setType(WadFileEntry.WadFileEntryType.UNKOWN);
                    }
                }
                wadInfo.setSize(Utils.readUnsignedInteger(rawWad));
                int[] unknown2 = new int[3];
                unknown2[0] = Utils.readUnsignedInteger(rawWad);
                unknown2[1] = Utils.readUnsignedInteger(rawWad);
                unknown2[2] = Utils.readUnsignedInteger(rawWad);
                wadInfo.setUnknown2(unknown2);
                entries.add(wadInfo);
            }

            //Read the file names and put them to a hashmap
            rawWad.seek(nameOffset);
            byte[] nameArray = new byte[nameSize];
            rawWad.read(nameArray);
            int offset = 0;
            wadFileEntries = new LinkedHashMap<>(files);
            for (WadFileEntry entry : entries) {
                wadFileEntries.put(Utils.bytesToString(Arrays.copyOfRange(nameArray, offset, offset + entry.getNameSize())).trim(), entry);
                offset += entry.getNameSize();
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

        //Open the WAD for extraction
        try (RandomAccessFile rawWad = new RandomAccessFile(file, "r")) {

            for (String fileName : wadFileEntries.keySet()) {
                extractFileData(fileName, destination, rawWad);
            }
        } catch (Exception e) {

            //Fug
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
    private File extractFileData(String fileName, String destination, RandomAccessFile rawWad) {

        //See that the destination is formatted correctly and create it if it does not exist
        String dest = destination;
        if (!dest.endsWith(File.separator)) {
            dest = dest.concat(File.separator);
        }

        String mkdir = dest;
        if (fileName.contains(File.separator)) {
            this.subdir = fileName.substring(0, fileName.lastIndexOf(File.separator) + 1);
            mkdir += this.subdir;
        } else {
            dest += this.subdir;
        }

        File destinationFolder = new File(mkdir);
        destinationFolder.mkdirs();
        dest = dest.concat(fileName);

        //Write to the file
        try (OutputStream outputStream = new FileOutputStream(dest)) {
            getFileData(fileName, rawWad).writeTo(outputStream);
            return new File(dest);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to " + dest + "!", e);
        }
    }

    /**
     * Extract a single file to a given location
     *
     * @param fileName file to extract
     * @param destination destination directory
     */
    public File extractFileData(String fileName, String destination) {

        //Open the WAD for extraction
        try (RandomAccessFile rawWad = new RandomAccessFile(file, "r")) {
            return extractFileData(fileName, destination, rawWad);
        } catch (Exception e) {

            //Fug
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
    private ByteArrayOutputStream getFileData(String fileName, RandomAccessFile rawWad) {
        ByteArrayOutputStream result = null;

        //Get the file
        WadFileEntry fileEntry = wadFileEntries.get(fileName);
        if (fileEntry == null) {
            throw new RuntimeException("File " + fileName + " not found from the WAD archive!");
        }

        try {

            //Seek to the file we want and read it
            rawWad.seek(fileEntry.getOffset());
            byte[] bytes = new byte[fileEntry.getCompressedSize()];
            rawWad.read(bytes);

            result = new ByteArrayOutputStream();

            //See if the file is compressed
            if (fileEntry.isCompressed()) {
                result.write(decompressFileData(bytes, fileName));
            } else {
                result.write(bytes);
            }

        } catch (Exception e) {

            //Fug
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

        //Open the WAD for extraction
        try (RandomAccessFile rawWad = new RandomAccessFile(file, "r")) {
            return getFileData(fileName, rawWad);
        } catch (Exception e) {

            //Fug
            throw new RuntimeException("Failed to read the WAD file!", e);
        }
    }

    /**
     * Some file entries in the WAD are compressed (type 4?), this decompresses
     * the file data
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

        int decsize = (Utils.toUnsignedByte(src[i]) << 16) + (Utils.toUnsignedByte(src[i + 1]) << 8) + Utils.toUnsignedByte(src[i + 2]);
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
            if ((Utils.toUnsignedByte(flag) & 0x80) == 0) {
                byte tmp = src[i++];
                counter = Utils.toUnsignedByte(flag) & 3; // mod 4
                while (counter-- != 0) // Copy literally
                {
                    dest[j] = src[i++];
                    j++;
                }
                int k = j; // Get the destbuf position
                k -= (Utils.toUnsignedByte(flag) & 0x60) << 3;
                k -= Utils.toUnsignedByte(tmp);
                k--;

                counter = ((Utils.toUnsignedByte(flag) >> 2) & 7) + 2;
                do {
                    dest[j] = dest[k++];
                    j++;
                } while (counter-- != 0); // Correct decrement
            } else if ((Utils.toUnsignedByte(flag) & 0x40) == 0) {
                byte tmp = src[i++];
                byte tmp2 = src[i++];
                counter = (Utils.toUnsignedByte(tmp)) >> 6;
                while (counter-- != 0) // Copy literally
                {
                    dest[j] = src[i++];
                    j++;
                }
                int k = j;
                k -= (Utils.toUnsignedByte(tmp) & 0x3F) << 8;
                k -= Utils.toUnsignedByte(tmp2);
                k--;
                counter = (Utils.toUnsignedByte(flag) & 0x3F) + 3;
                do {
                    dest[j] = dest[k++];
                    j++;
                } while (counter-- != 0); // Correct postfix decrement
            } else if ((Utils.toUnsignedByte(flag) & 0x20) == 0) {
                byte localtemp = src[i++];
                byte tmp2 = src[i++];
                byte tmp3 = src[i++];
                counter = Utils.toUnsignedByte(flag) & 3;
                while (counter-- != 0) // Copy literally
                {
                    dest[j] = src[i++];
                    j++;
                }
                int k = j;
                k -= (Utils.toUnsignedByte(flag) & 0x10) << 12;
                k -= Utils.toUnsignedByte(localtemp) << 8;
                k -= Utils.toUnsignedByte(tmp2);
                k--;
                counter = Utils.toUnsignedByte(tmp3) + ((Utils.toUnsignedByte(flag) & 0x0C) << 6) + 4;
                do {
                    dest[j] = dest[k++];
                    j++;
                } while (counter-- != 0); // Correct
            } else {
                counter = (Utils.toUnsignedByte(flag) & 0x1F) * 4 + 4;
                if (Utils.toUnsignedByte(Integer.valueOf(counter).byteValue()) > 0x70) {
                    finished = true;

                    // Crepare to copy the last bytes
                    counter = Utils.toUnsignedByte(flag) & 3;
                }
                while (counter-- != 0) // Copy literally
                {
                    dest[j] = src[i++];
                    j++;
                }
            }
        } // Of while()
        if (!finished) {
            System.err.println("File " + fileName + " might not be successfully extracted!");
        }
        return dest;
    }
}
