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
     * Reads a chunk of wanted size, wrapped into a IResourceChunkReader
     * 
     * @param size the size of chunk to read, the resulting chunk might be smaller if data is not available
     * @return returns null if end of file reached
     * @throws IOException 
     */
    IResourceChunkReader readChunk(int size) throws IOException;

    IResourceChunkReader readAll() throws IOException;

    void skipBytes(int size) throws IOException;
}
