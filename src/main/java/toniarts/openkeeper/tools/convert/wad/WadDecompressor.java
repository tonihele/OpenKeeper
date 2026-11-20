/*
 * Copyright (C) 2026 OpenKeeper
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

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

/**
 * Decodes the compressed payload used by DK2 WAD entries.
 */
final class WadDecompressor {

    private static final Logger logger = System.getLogger(WadDecompressor.class.getName());

    private WadDecompressor() {
    }

    static byte[] decompress(byte[] source, String fileName) throws IOException {
        try {
            return decode(source, fileName);
        } catch (IndexOutOfBoundsException exception) {
            throw new IOException("Malformed compressed WAD entry: " + fileName, exception);
        }
    }

    private static byte[] decode(byte[] source, String fileName) throws IOException {
        int sourcePosition = 0;
        int destinationPosition = 0;
        if ((source[sourcePosition++] & 1) != 0) {
            sourcePosition += 3;
        }
        sourcePosition++; // second header byte

        int decompressedSize = ((source[sourcePosition] & 0xFF) << 16)
                | ((source[sourcePosition + 1] & 0xFF) << 8)
                | (source[sourcePosition + 2] & 0xFF);
        byte[] destination = new byte[decompressedSize];
        sourcePosition += 3;

        boolean finished = false;
        while (!finished && sourcePosition < source.length) {
            int flag = source[sourcePosition++] & 0xFF;
            if ((flag & 0x80) == 0) {
                int lowOffset = source[sourcePosition++] & 0xFF;
                int literalCount = flag & 0x03;
                destinationPosition = copyLiterals(source, sourcePosition, destination,
                        destinationPosition, literalCount);
                sourcePosition += literalCount;
                int backReference = destinationPosition - ((flag & 0x60) << 3) - lowOffset - 1;
                int copyCount = ((flag >> 2) & 0x07) + 3;
                destinationPosition = copyBackReference(destination, backReference,
                        destinationPosition, copyCount);
            } else if ((flag & 0x40) == 0) {
                int highOffset = source[sourcePosition++] & 0xFF;
                int lowOffset = source[sourcePosition++] & 0xFF;
                int literalCount = highOffset >> 6;
                destinationPosition = copyLiterals(source, sourcePosition, destination,
                        destinationPosition, literalCount);
                sourcePosition += literalCount;
                int backReference = destinationPosition - ((highOffset & 0x3F) << 8) - lowOffset - 1;
                int copyCount = (flag & 0x3F) + 4;
                destinationPosition = copyBackReference(destination, backReference,
                        destinationPosition, copyCount);
            } else if ((flag & 0x20) == 0) {
                int highOffset = source[sourcePosition++] & 0xFF;
                int middleOffset = source[sourcePosition++] & 0xFF;
                int lengthByte = source[sourcePosition++] & 0xFF;
                int literalCount = flag & 0x03;
                destinationPosition = copyLiterals(source, sourcePosition, destination,
                        destinationPosition, literalCount);
                sourcePosition += literalCount;
                int backReference = destinationPosition - ((flag & 0x10) << 12)
                        - (highOffset << 8) - middleOffset - 1;
                int copyCount = lengthByte + ((flag & 0x0C) << 6) + 5;
                destinationPosition = copyBackReference(destination, backReference,
                        destinationPosition, copyCount);
            } else {
                int literalCount = (flag & 0x1F) * 4 + 4;
                if (literalCount > 0x70) {
                    finished = true;
                    literalCount = flag & 0x03;
                }
                destinationPosition = copyLiterals(source, sourcePosition, destination,
                        destinationPosition, literalCount);
                sourcePosition += literalCount;
            }
        }

        if (destinationPosition != destination.length) {
            throw new IOException("Compressed WAD entry " + fileName + " produced "
                    + destinationPosition + " of " + destination.length + " bytes");
        }
        if (!finished) {
            logger.log(Level.WARNING, "File {0} might not be successfully extracted!", fileName);
        }
        return destination;
    }

    private static int copyLiterals(byte[] source, int sourcePosition, byte[] destination,
            int destinationPosition, int count) throws IOException {
        requireRange(source.length, sourcePosition, count, "compressed input");
        requireRange(destination.length, destinationPosition, count, "decompressed output");
        System.arraycopy(source, sourcePosition, destination, destinationPosition, count);
        return destinationPosition + count;
    }

    private static int copyBackReference(byte[] destination, int sourcePosition,
            int destinationPosition, int count) throws IOException {
        requireRange(destination.length, destinationPosition, count, "decompressed output");
        if (sourcePosition < 0 || sourcePosition >= destinationPosition) {
            throw new IOException("Invalid WAD back-reference at output offset " + destinationPosition);
        }
        for (int i = 0; i < count; i++) {
            destination[destinationPosition++] = destination[sourcePosition++];
        }
        return destinationPosition;
    }

    private static void requireRange(int length, int offset, int count, String bufferName) throws IOException {
        if (offset < 0 || count < 0 || offset > length - count) {
            throw new IOException("Invalid " + bufferName + " range: offset=" + offset + ", count=" + count);
        }
    }
}
