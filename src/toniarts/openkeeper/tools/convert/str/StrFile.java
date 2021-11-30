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
package toniarts.openkeeper.tools.convert.str;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.IResourceChunkReader;
import toniarts.openkeeper.tools.convert.FileResourceReader;
import toniarts.openkeeper.tools.convert.ISeekableResourceReader;

/**
 * Reads the Dungeon Keeper 2 STR files<br>
 * Converted to JAVA from C code, C code by:
 * <li>Tomasz Lis</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class StrFile {

    private static final String STR_HEADER_IDENTIFIER = "BFST";
    private static final int STR_HEADER_SIZE = 12;
    // Codepage chunk types
    private static final int CHUNK_TYPE_END = 0;
    private static final int CHUNK_TYPE_STRING = 1;
    private static final int CHUNK_TYPE_PARAM = 2;
    //
    private final MbToUniFile codePage;
    private final int fileId;
    private final LinkedHashMap<Integer, String> entries;

    private static final Logger LOGGER = Logger.getLogger(StrFile.class.getName());

    /**
     * Constructs a new STR file reader<br>
     * Reads the STR file structure
     *
     * @param file the str file to read
     */
    public StrFile(Path file) {
        this(readCodePage(file), file);
    }

    /**
     * Constructs a new STR file reader using given code page. Particularly
     * useful for batch runs, no need to read the code page all over again<br>
     * Reads the STR file structure
     *
     * @param codePage the code page
     * @param file the str file to read
     */
    public StrFile(MbToUniFile codePage, Path file) {
        this.codePage = codePage;

        // Read the file
        try (ISeekableResourceReader rawStr = new FileResourceReader(file)) {

            // Check the header
            IResourceChunkReader rawStrReader = rawStr.readChunk(12);
            String header = rawStrReader.readString(4);
            if (!STR_HEADER_IDENTIFIER.equals(header)) {
                throw new RuntimeException("Header should be " + STR_HEADER_IDENTIFIER + " and it was " + header + "! Cancelling!");
            }

            // Header... 12 bytes, must be added to offsets
            fileId = rawStrReader.readUnsignedInteger();
            int offsetsCount = rawStrReader.readUnsignedInteger();

            // Read the offsets
            rawStrReader = rawStr.readChunk(offsetsCount * 4);
            List<Integer> offsets = new ArrayList<>(offsetsCount);
            for (int i = 0; i < offsetsCount; i++) {
                offsets.add(rawStrReader.readUnsignedInteger());
            }

            // Make a copy because offsets in some languages (like german) are not sorted!
            List<Integer> offsetsCopy = new ArrayList<>(offsets);
            Collections.sort(offsetsCopy);

            // Decode the entries
            entries = new LinkedHashMap<>(offsetsCount);
            for (int i = 0; i < offsetsCount; i++) {

                // Seek to the data and read it
                rawStr.seek(offsets.get(i) + STR_HEADER_SIZE);
                int j = Collections.binarySearch(offsetsCopy, offsets.get(i));
                int dataLength = (int) (j < offsetsCopy.size() - 1 ? offsetsCopy.get(j + 1) - offsets.get(i) : rawStr.length() - offsets.get(i) - STR_HEADER_SIZE);

                IResourceChunkReader data = rawStr.readChunk(dataLength);
                // FIXME should we throws Exception?
                if (data.length() < dataLength) {
                    LOGGER.log(Level.WARNING, "Entry {0} was supposed to be {1} but only {2} could be read!", new Object[]{i, dataLength, data.length()});
                }

                // Encode the string
                String entry = decodeEntry(data);
                if (entry == null) {
                    throw new RuntimeException("Failed to encode entry #" + i + "!");
                }
                entries.put(i, entry);
            }
        } catch (IOException e) {

            // Fug
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
    }

    /**
     * Reads the code page file "MBToUni.dat". It is assumed to be in the same
     * directory than the file
     *
     * @param file STR file
     * @return code page as char buffer
     * @throws RuntimeException may fail miserably
     */
    private static MbToUniFile readCodePage(Path file) throws RuntimeException {

        // We also need the codepage, assume it is in the same directory
        return new MbToUniFile(file.getParent().resolve("MBToUni.dat"));
    }

    /**
     * Decodes one entry in STR file
     *
     * @param data the entry bytes
     * @return returns null if error occurred, otherwise the decoded string
     */
    private String decodeEntry(final IResourceChunkReader data) {
        ByteBuffer byteBuffer = data.getByteBuffer();
        StringBuilder buffer = new StringBuilder(data.length());

        // Loop through the bytes
        int chunkType;
        do {

            // Read chunk
            chunkType = byteBuffer.getInt();
            int chunkLength = (chunkType >> 8);
            chunkType &= 0xff;

            // Check the type
            switch (chunkType) {
                case CHUNK_TYPE_END: { // End
                    if (chunkLength != 0) {
                        LOGGER.severe("End chunk has non-zero length!");
                        return null;
                    }
                    break;
                }
                case CHUNK_TYPE_PARAM: { // Param
                    buffer.append('%');
                    int val = (chunkLength + 1) / 10;
                    if (val > 0) {
                        buffer.append(val);
                    }
                    val = (chunkLength + 1) % 10;
                    buffer.append(val);
                    break;
                }
                case CHUNK_TYPE_STRING: { // String
                    if (chunkLength > byteBuffer.remaining()) {
                        LOGGER.severe("Chunk length exceeds the remaining bytes length!");
                        return null;
                    }

                    // Decode the chunk
                    byte[] chunk = new byte[chunkLength];
                    byteBuffer.get(chunk);
                    buffer.append(decodeChunk(ByteBuffer.wrap(chunk)));
                    break;
                }
                default: {
                    LOGGER.severe("Invalid chunk type!");
                    return null;
                }
            }

            // Position needs to be divisible by 4
            if (byteBuffer.position() % 4 != 0) {
                byteBuffer.position(byteBuffer.position() + (4 - byteBuffer.position() % 4));
            }
        } while (chunkType != CHUNK_TYPE_END && byteBuffer.hasRemaining());

        // Return the string
        return buffer.toString();
    }

    /**
     * Decodes one string chunk inside of an STR entry
     *
     * @param chunk chuck data
     * @return decoded string
     */
    private String decodeChunk(final ByteBuffer chunk) {

        // Go through each byte
        StringBuilder buffer = new StringBuilder(chunk.limit());
        while (chunk.hasRemaining()) {
            int codePageIndex = ConversionUtils.toUnsignedByte(chunk.get()) - 1;
            if (codePageIndex >= codePage.getThreshold()) {
                codePageIndex = (codePageIndex * 255) - codePage.getThreshold() * 254 + ConversionUtils.toUnsignedByte(chunk.get()) - 1;
            }

            // Read the character from the code page
            char character = codePage.getCharacter(codePageIndex);

            // Escapes
            if (character == '%') {
                buffer.append("%");
            } else if ((character == '\\') || (character == '\n') || (character == '\t')) {
                buffer.append("\\");
                switch (character) {
                    case '\\':
                        break;
                    case '\n':
                        character = 'n';
                        break;
                    case '\t':
                        character = 't';
                        break;
                }
            }

            // Add the character to the buffer
            buffer.append(character);
        }

        // Return the decoded string
        return buffer.toString();
    }

    /**
     * Get the string entries<br>
     * The entries are returned in order (by the id) for your inconvenience
     *
     * @return set of entries
     */
    public Set<Entry<Integer, String>> getEntriesAsSet() {
        return entries.entrySet();
    }

    /**
     * Get the mapped entries<br>
     * The entries are returned in order (by the id) for your inconvenience
     *
     * @return the entries
     */
    public LinkedHashMap<Integer, String> getEntries() {
        return entries;
    }

    /**
     * Get the used code page for reuse in batch runs
     *
     * @return the code page
     */
    public MbToUniFile getCodePage() {
        return codePage;
    }

    /**
     * Get file ID
     *
     * @return file ID
     */
    public int getFileId() {
        return fileId;
    }

}
