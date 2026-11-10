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

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Offers our resource specific methods for translating binary data to our native classes. Backed by a
 * ByteBuffer.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ResourceChunkReader implements IResourceChunkReader {

    private static final Logger logger = System.getLogger(ResourceChunkReader.class.getName());

    private final ByteBuffer buffer;

    public ResourceChunkReader(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public boolean hasRemaining() {
        return buffer.hasRemaining();
    }

    @Override
    public int length() {
        return buffer.capacity();
    }

    @Override
    public byte[] read(int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes, 0, length);

        return bytes;
    }

    @Override
    public void readAndCheckNull(int size) {
        for (int i = 0; i < size; i++) {
            byte b = buffer.get();
            if (b != 0) {
                logger.log(Level.WARNING, "Value not 0! Was {0}!", b);
            }
        }
    }

    @Override
    public byte readByte() {
        return buffer.get();
    }

    @Override
    public <E extends Enum & IValueEnum> E readByteAsEnum(Class<E> enumeration) {
        return ConversionUtils.parseEnum(readUnsignedByte(), enumeration);
    }

    @Override
    public <E extends Enum<E> & IFlagEnum> EnumSet<E> readByteAsFlag(Class<E> enumeration) {
        return ConversionUtils.parseFlagValue(readUnsignedByte(), enumeration);
    }

    @Override
    public float readFloat() {
        return buffer.getFloat();
    }

    @Override
    public int readInteger() {
        return buffer.getInt();
    }

    @Override
    public float readIntegerAsDouble() {
        return buffer.getInt() / ConversionUtils.DOUBLE;
    }

    @Override
    public <E extends Enum & IValueEnum> E readIntegerAsEnum(Class<E> enumeration) {
        return ConversionUtils.parseEnum(readUnsignedInteger(), enumeration);
    }

    @Override
    public <E extends Enum<E> & IFlagEnum> EnumSet<E> readIntegerAsFlag(Class<E> enumeration) {
        return ConversionUtils.parseFlagValue(readUnsignedIntegerAsLong(), enumeration);
    }

    @Override
    public float readIntegerAsFloat() {
        return buffer.getInt() / ConversionUtils.FLOAT;
    }

    @Override
    public short readShort() {
        return buffer.getShort();
    }

    @Override
    public <E extends Enum & IValueEnum> E readShortAsEnum(Class<E> enumeration) {
        return ConversionUtils.parseEnum(readUnsignedShort(), enumeration);
    }

    @Override
    public <E extends Enum<E> & IFlagEnum> EnumSet<E> readShortAsFlag(Class<E> enumeration) {
        return ConversionUtils.parseFlagValue(readUnsignedShort(), enumeration);
    }

    @Override
    public float readShortAsFloat() {
        return buffer.getShort() / ConversionUtils.FLOAT;
    }

    @Override
    public String readString(int length) {
        byte[] bytes = new byte[length];
        buffer.get(bytes, 0, length);

        return ConversionUtils.toString(bytes);
    }

    @Override
    public String readStringUtf16(int length) {
        byte[] bytes = new byte[length * 2];
        buffer.get(bytes, 0, bytes.length);

        return ConversionUtils.toStringUtf16(bytes);
    }

    @Override
    public LocalDateTime readTimestamp() throws IOException {
        int year = readUnsignedShort();
        int dayOfMonth = readUnsignedByte();
        int month = readUnsignedByte();

        skipBytes(2);

        int hour = readUnsignedByte();
        int minute = readUnsignedByte();
        int second = readUnsignedByte();

        skipBytes(1);

        if (year == 0) {

            // Null timestamp
            return null;
        }

        return LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, 0);
    }

    @Override
    public short readUnsignedByte() {
        return (short) (buffer.get() & 0xFF);
    }

    @Override
    public int readUnsignedInteger() {
        int result = buffer.getInt();
        if (result < 0) {

            // Yes, this should be long, however, in our purpose this might be sufficient as int
            // Safety measure
            logger.log(Level.WARNING, "This unsigned integer doesn't fit to JAVA integer! Use a different method!");
        }
        return result;
    }

    @Override
    public long readUnsignedIntegerAsLong() {
        return buffer.getInt() & 0xFFFFFFFFL;
    }

    @Override
    public int readUnsignedShort() {
        return buffer.getShort() & 0xFFFF;
    }

    @Override
    public String readVaryingLengthString(int length) {
        byte[] bytes = read(length);

        // A bit tricky, read until 0 byte
        int i = 0;
        for (byte b : bytes) {
            if (b == 0) {
                break;
            }
            i++;
        }

        return ConversionUtils.toString(Arrays.copyOf(bytes, i));
    }

    @Override
    public String readVaryingLengthStringUtf16(int length) {
        byte[] bytes = this.read(length * 2);

        int i;
        for (i = 0; i < bytes.length; i += 2) {
            if (bytes[i] == 0 && bytes[i + 1] == 0) {
                break;
            }
        }

        return ConversionUtils.toStringUtf16(Arrays.copyOf(bytes, i));
    }

    @Override
    public List<String> readVaryingLengthStrings(int numberOfStrings) {
        List<String> strings = new ArrayList<>(numberOfStrings);

        for (int i = 0; i < numberOfStrings; i++) {

            // A bit tricky, read until 0 byte
            List<Byte> bytes = new ArrayList();
            byte b;
            do {
                b = buffer.get();
                if (b != 0) {
                    bytes.add(b);
                } else {
                    break;
                }
            } while (true);
            strings.add(ConversionUtils.toString(ConversionUtils.toByteArray(bytes)));
        }

        return strings;
    }

    @Override
    public void skipBytes(int size) throws IOException {
        if (buffer.remaining() < size) {
            String message = "Error skipping bytes. Expect %s bytes and %s given";
            throw new IOException(String.format(message, size, buffer.remaining()));
        }

        buffer.position(buffer.position() + size);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return buffer;
    }

    @Override
    public int position() {
        return buffer.position();
    }

    @Override
    public void position(int pos) {
        buffer.position(pos);
    }

    @Override
    public void mark() {
        buffer.mark();
    }

    @Override
    public void reset() {
        buffer.reset();
    }

    @Override
    public int compareTo(ByteBuffer that) {
        return buffer.compareTo(that);
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ByteBuffer) {
            return Objects.equals(this.buffer, obj);
        }
        if (obj instanceof ResourceChunkReader resourceChunkReader) {
            return Objects.equals(this.buffer, resourceChunkReader.buffer);
        }

        return false;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
