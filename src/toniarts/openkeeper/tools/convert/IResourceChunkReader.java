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
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

/**
 * Offers our resource specific methods for translating binary data to our
 * native classes.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IResourceChunkReader extends Comparable<ByteBuffer> {

    /**
     * See if data still available for reading
     *
     * @return true if the chunk has still data available
     */
    boolean hasRemaining();

    int length();

    byte[] read(int length);

    /**
     * Change this function to skipBytes when all resources will be decoded
     *
     * @param size number of bytes
     * @deprecated
     */
    @Deprecated
    void readAndCheckNull(int size);

    byte readByte();

    <E extends Enum & IValueEnum> E readByteAsEnum(Class<E> enumeration);

    <E extends Enum<E> & IFlagEnum> EnumSet<E> readByteAsFlag(Class<E> enumeration);

    /**
     * Reads 4 bytes and converts it to JAVA float from LITTLE ENDIAN float
     *
     * @return JAVA native float
     */
    float readFloat();

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN int
     *
     * @return JAVA native int
     */
    int readInteger();

    float readIntegerAsDouble();

    <E extends Enum & IValueEnum> E readIntegerAsEnum(Class<E> enumeration);

    <E extends Enum<E> & IFlagEnum> EnumSet<E> readIntegerAsFlag(Class<E> enumeration);

    float readIntegerAsFloat();

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN signed short
     *
     * @return JAVA native short
     */
    short readShort();

    <E extends Enum & IValueEnum> E readShortAsEnum(Class<E> enumeration);

    <E extends Enum<E> & IFlagEnum> EnumSet<E> readShortAsFlag(Class<E> enumeration);

    float readShortAsFloat();

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param length string length
     * @see #ConversionUtils.toString(byte[])
     * @return fresh String
     */
    String readString(int length);

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param length string length
     * @see #ConversionUtils.toStringUtf16(byte[])
     * @return fresh String
     */
    String readStringUtf16(int length);

    /**
     * Reads a DK2 style timestamp
     *
     * @return the date in current locale
     * @throws java.io.IOException
     */
    Date readTimestamp() throws IOException;

    short readUnsignedByte();

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int
     *
     * @return JAVA native int
     * @see #ResourceReader.readUnsignedIntegerAsLong()
     */
    int readUnsignedInteger();

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int<br>
     * This method returns long, which means the value is sure to fit
     *
     * @return JAVA native long
     */
    long readUnsignedIntegerAsLong();

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN unsigned short (needs to be int in JAVA)
     *
     * @return JAVA native int
     */
    int readUnsignedShort();

    /**
     * Reads string of varying length (ASCII NULL terminated) from the file
     *
     * @param length bytes to reed from file
     * @return string read from the file
     */
    String readVaryingLengthString(int length);

    /**
     * Reads strings of varying length (UTF16 NULL terminated) from the file
     *
     * @param length max length of string
     * @return string read from the file
     */
    String readVaryingLengthStringUtf16(int length);

    /**
     * Reads strings of varying length (ASCII NULL terminated) from the file
     *
     * @param numberOfStrings number of Strings to read
     * @return list of strings read from the file
     */
    List<String> readVaryingLengthStrings(int numberOfStrings);

    void skipBytes(int size) throws IOException;

    /**
     * Get the underlying ByteBuffer. Any modification will affect the reader as
     * well.
     *
     * @return the ByteBuffer user by this reader
     */
    ByteBuffer getByteBuffer();

    /**
     * @see ByteBuffer#position()
     */
    int position();

    /**
     * @see ByteBuffer#position(int)
     */
    void position(int pos);

    /**
     * @see ByteBuffer#mark()
     */
    void mark();

    /**
     * @see ByteBuffer#reset()
     */
    void reset();

}
