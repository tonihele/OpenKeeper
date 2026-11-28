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
package toniarts.openkeeper.tools.convert;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Shared validation and stream operations for resource readers.
 */
final class ResourceReaderSupport {

    private ResourceReaderSupport() {
    }

    static void checkLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length must be non-negative: " + length);
        }
    }

    static int readUpTo(InputStream input, byte[] bytes) throws IOException {
        int total = 0;
        while (total < bytes.length) {
            int read = input.read(bytes, total, bytes.length - total);
            if (read < 0) {
                break;
            }
            if (read == 0) {
                int value = input.read();
                if (value < 0) {
                    break;
                }
                bytes[total++] = (byte) value;
            } else {
                total += read;
            }
        }
        return total;
    }

    static void readFully(InputStream input, byte[] bytes) throws IOException {
        int read = readUpTo(input, bytes);
        if (read != bytes.length) {
            throw new EOFException("Expected " + bytes.length + " bytes, read " + read);
        }
    }

    static void skipFully(InputStream input, int length) throws IOException {
        checkLength(length);
        long remaining = length;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
            } else if (input.read() < 0) {
                throw new EOFException("Reached end of input with " + remaining + " bytes left to skip");
            } else {
                remaining--;
            }
        }
    }
}
