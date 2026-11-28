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

import java.io.EOFException;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


/**
 * Resource reader is a convenience wrapper around any Dungeon Keeper 2 resource
 * file. Provides easy to use functions to read the binary files to java. The
 * resources are Little Endian.
 *
 * @author archdemon
 */
public final class FileResourceReader implements ISeekableResourceReader {
    
    private static final Logger logger = System.getLogger(FileResourceReader.class.getName());

    private final SeekableByteChannel file;

    public FileResourceReader(String filename) throws IOException {
        this(Paths.get(filename));
    }

    public FileResourceReader(Path path) throws IOException {
        file = Files.newByteChannel(path, StandardOpenOption.READ);
    }

    @Override
    public IResourceChunkReader readChunk(int size) throws IOException {
        ResourceReaderSupport.checkLength(size);
        if (size == 0) {
            return new ResourceChunkReader(allocateBuffer(0));
        }

        long remaining = file.size() - file.position();
        int capacity = (int) Math.min((long) size, Math.max(0L, remaining));
        if (capacity == 0) {
            return null;
        }

        ByteBuffer buffer = allocateBuffer(capacity);
        int read = readUpTo(buffer);
        buffer.flip();
        if (read == 0) {
            return null;
        }

        return new ResourceChunkReader(buffer);
    }

    @Override
    public IResourceChunkReader readAll() throws IOException {
        long remainingBytes = file.size() - file.position();
        if (remainingBytes > Integer.MAX_VALUE) {
            throw new IOException("File is too big to be read!");
        }
        if (remainingBytes <= 0) {
            return readChunk(0);
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
        ResourceReaderSupport.checkLength(length);
        ByteBuffer buffer = ByteBuffer.allocate(length);
        int read = readUpTo(buffer);
        if (read != length) {
            throw new EOFException("Expected " + length + " bytes, read " + read);
        }
        return buffer.array();
    }

    private int readUpTo(ByteBuffer buffer) throws IOException {
        int initialPosition = buffer.position();
        while (buffer.hasRemaining()) {
            int read = file.read(buffer);
            if (read < 0) {
                break;
            }
            if (read == 0) {
                Thread.yield();
            }
        }
        return buffer.position() - initialPosition;
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
            logger.log(Level.ERROR, "Closing file error", ex);
        }
    }

    @Override
    public void skipBytes(int size) throws IOException {
        ResourceReaderSupport.checkLength(size);
        long position = file.position();
        long fileSize = file.size();
        if (size > 0 && (position > fileSize || size > fileSize - position)) {
            throw new EOFException("Cannot skip " + size + " bytes from position " + position);
        }
        file.position(position + size);
    }
}
