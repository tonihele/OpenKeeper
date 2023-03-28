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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource reader is a convenience wrapper around any Dungeon Keeper 2 resource
 * file. Provides easy to use functions to read the binary files to java. The
 * resources are Little Endian.<br>
 * 
 * This is buffered forward reading resource reader with no seek nor position.
 * 
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class BufferedResourceReader implements IResourceReader {
    
    private static final Logger LOGGER = Logger.getLogger(BufferedResourceReader.class.getName());

    private final BufferedInputStream input;

    public BufferedResourceReader(String filename) throws IOException {
        this(Paths.get(filename));
    }

    public BufferedResourceReader(Path path) throws IOException {
        input = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
    }

    public BufferedResourceReader(InputStream inputStream) throws IOException {
        input = new BufferedInputStream(inputStream);
    }
    
    @Override
    public IResourceChunkReader readChunk(int size) throws IOException {
        byte[] bytes = new byte[size];
        int read = input.read(bytes);
        if(read == -1) {
            return null;
        }
        
        ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, read);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        return new ResourceChunkReader(buffer);
    }

    @Override
    public IResourceChunkReader readAll() throws IOException {
        byte[] bytes = input.readAllBytes();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
       
        return new ResourceChunkReader(buffer);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return input.read(b);
    }

    @Override
    public byte[] read(int length) throws IOException {
        byte[] bytes = new byte[length];

        int result = input.read(bytes);
        if (result != length) {
            String message = "Error reading byte array. Expect %s bytes and %s given";
            throw new IOException(String.format(message, length, result));
        }

        return bytes;
    }

    @Override
    public void close() {
        try {
            input.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Closing file error", ex);
        }
    }

    @Override
    public void skipBytes(int size) throws IOException {
        input.skip(size);
    }
}
