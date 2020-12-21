/*
 * Copyright (C) 2014-2019 OpenKeeper
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource reader is a convenience wrapper around any Dungeon Keeper 2 resource
 * file. Provides easy to use functions to read the binary files to java. The
 * resources are Little Endian.
 *
 * @author archdemon
 */
public class FileResourceReader implements IResourceReader {

    private final SeekableByteChannel file;
    private final ByteBuffer buf;

    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final Logger LOGGER = Logger.getLogger(FileResourceReader.class.getName());

    public FileResourceReader(String filename) throws IOException {
        this(Paths.get(filename));
    }

    public FileResourceReader(Path path) throws IOException {
        file = Files.newByteChannel(path, StandardOpenOption.READ);
        buf = ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE);
        buf.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public IResourceChunkReader readChunk(int size) throws IOException {
        ByteBuffer buffer = allocateBuffer(size);
        int sizeLeft = size;
        while (sizeLeft >= DEFAULT_BUFFER_SIZE) {
            int read = file.read(buf);
            if (read <= 0) {
                sizeLeft = 0;
                break;
            } else {
                sizeLeft -= read;
            }
            buf.flip();
            buffer.put(buf);
            buf.flip();
        }
        if (sizeLeft > 0) {

            // Read the rest
            file.read(buffer);
        }
        buffer.flip();

        return new ResourceChunkReader(buffer);
    }

    @Override
    public IResourceChunkReader readAll() throws IOException {
        long remainingBytes = file.size() - file.position();
        if (remainingBytes > Integer.MAX_VALUE) {
            throw new IOException("File is too big to be read!");
        }

        return readChunk((int) remainingBytes);
    }

    private ByteBuffer allocateBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        return buffer;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return file.read(ByteBuffer.wrap(b));
    }

    @Override
    public byte[] read(int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);

        int result = file.read(buffer);
        if (result != length) {
            String message = "Error reading byte array. Expect %s bytes and %s given";
            throw new IOException(String.format(message, length, result));
        }

        return buffer.array();
    }

    @Override
    public long getFilePointer() throws IOException {
        return file.position();
    }

    @Override
    public void seek(long pos) throws IOException {
        file.position(pos);
    }

    @Override
    public long length() throws IOException {
        return file.size();
    }

    /**
     * End of file
     *
     * @return true if file pointer >= length of file
     * @throws IOException
     */
    @Override
    public boolean isEof() throws IOException {
        return getFilePointer() >= length();
    }

    @Override
    public void close() {
        try {
            file.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Closing file error", ex);
        }
    }

    @Override
    public void skipBytes(int size) throws IOException {
        file.position(file.position() + 12);
    }
}
