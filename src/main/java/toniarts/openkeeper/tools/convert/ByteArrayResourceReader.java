/*
 * Copyright (C) 2014-2020 OpenKeeper
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A version of Dungeon Keeper 2 resource reader that reads data directly from
 * given byte array. Convenient if we already read all the data and
 * decompressed/decrypted it. No need to swing it through a temp file etc.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ByteArrayResourceReader implements ISeekableResourceReader {

    private final ByteBuffer buffer;

    public ByteArrayResourceReader(byte[] data) {
        buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public long getFilePointer() throws IOException {
        return buffer.position();
    }

    @Override
    public boolean isEof() throws IOException {
        return !buffer.hasRemaining();
    }

    @Override
    public long length() throws IOException {
        return buffer.capacity();
    }

    @Override
    public int read(byte[] b) throws IOException {
        int length = Math.min(b.length, buffer.remaining());
        System.arraycopy(buffer.array(), buffer.position(), b, 0, length);

        // Advance marker
        skipBytes(length);

        return length;
    }

    @Override
    public byte[] read(int length) throws IOException {
        ResourceReaderSupport.checkLength(length);
        if (buffer.remaining() < length) {
            throw new EOFException("Expected " + length + " bytes, only " + buffer.remaining() + " remain");
        }

        byte[] bytes = new byte[length];
        System.arraycopy(buffer.array(), buffer.position(), bytes, 0, length);
        skipBytes(length);
        return bytes;
    }

    @Override
    public void seek(long pos) throws IOException {
        if (pos < 0 || pos > buffer.capacity()) {
            throw new IOException("Position outside byte array: " + pos);
        }
        buffer.position((int) pos);
    }

    @Override
    public IResourceChunkReader readChunk(int size) throws IOException {
        ResourceReaderSupport.checkLength(size);
        if (size == 0) {
            return new ResourceChunkReader(ByteBuffer.allocate(0).order(ByteOrder.LITTLE_ENDIAN));
        }
        if (!buffer.hasRemaining()) {
            return null;
        }

        int chunkSize = Math.min(size, buffer.remaining());
        ByteBuffer chunk = buffer.slice();
        chunk.limit(chunkSize);
        chunk = chunk.slice().asReadOnlyBuffer();
        chunk.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(buffer.position() + chunkSize);
        return new ResourceChunkReader(chunk);
    }

    @Override
    public IResourceChunkReader readAll() throws IOException {
        return readChunk(buffer.remaining());
    }

    @Override
    public void skipBytes(int size) throws IOException {
        ResourceReaderSupport.checkLength(size);
        if (buffer.remaining() < size) {
            throw new EOFException("Cannot skip " + size + " bytes, only " + buffer.remaining() + " remain");
        }
        buffer.position(buffer.position() + size);
    }

    @Override
    public void close() throws IOException {
        // NOP
    }

}
