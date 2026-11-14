/*
 * Copyright (C) 2014-2021 OpenKeeper
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

import java.io.Closeable;
import java.io.IOException;

/**
 *
 * @author archdemon
 */
public interface IResourceReader extends Closeable {

    int read(byte[] b) throws IOException;

    byte[] read(int length) throws IOException;

    /**
     * Reads up to the requested number of bytes into a chunk. Short underlying
     * reads are retried; a partial chunk is returned only when the input reaches
     * end of file.
     *
     * @param size maximum number of bytes to read; must be non-negative
     * @return {@code null} when end of file is reached before any bytes are read
     *     for a positive size; a zero-size request returns an empty chunk
     * @throws IOException if reading fails
     * @throws IllegalArgumentException if size is negative
     */
    IResourceChunkReader readChunk(int size) throws IOException;

    IResourceChunkReader readAll() throws IOException;

    /**
     * Skips exactly the requested number of bytes.
     *
     * @param size number of bytes to skip; must be non-negative
     * @throws IOException if the input ends before all bytes can be skipped
     * @throws IllegalArgumentException if size is negative
     */
    void skipBytes(int size) throws IOException;
}
