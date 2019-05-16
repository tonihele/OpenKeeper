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

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

/**
 *
 * @author archdemon
 */
public interface IResourceReader extends Closeable {

    /**
     * Not all the data types are of the length that suits us, do our best to ignore it<br>
     * Skips the file to the correct position after an item is read<br>
     * <b>Use this directly with Things & Triggers!</b>
     *
     * @throws java.io.IOException
     * @see #toniarts.opendungeonkeeper.tools.convert.map.KwdFile.checkOffset(
     * toniarts.opendungeonkeeper.tools.convert.map.KwdFile.KwdHeader, java.io.RandomAccessFile, long)
     * @param itemSize the item size
     * @param offset the file offset before the last item was read
     */
    void checkOffset(long itemSize, long offset) throws IOException;

    long getFilePointer() throws IOException;

    /**
     * End of file
     *
     * @return true if filepointer >= length of file
     * @throws IOException
     */
    boolean isEof() throws IOException;

    long length() throws IOException;

    int read(byte[] b) throws IOException;

    byte[] read(int length) throws IOException;

    /**
     * Change this function to skipBytes when all resources will be decoded
     *
     * @param size number of bytes
     * @throws IOException
     * @deprecated
     */
    @Deprecated
    void readAndCheckNull(int size) throws IOException;

    byte readByte() throws IOException;

    <E extends Enum & IValueEnum> E readByteAsEnum(Class<E> enumeration) throws IOException;

    <E extends Enum<E> & IFlagEnum> EnumSet<E> readByteAsFlag(Class<E> enumeration) throws IOException;

    /**
     * Reads 4 bytes and converts it to JAVA float from LITTLE ENDIAN float
     *
     * @return JAVA native float
     * @throws IOException may fail
     */
    float readFloat() throws IOException;

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN int
     *
     * @return JAVA native int
     * @throws IOException may fail
     */
    int readInteger() throws IOException;

    float readIntegerAsDouble() throws IOException;

    <E extends Enum & IValueEnum> E readIntegerAsEnum(Class<E> enumeration) throws IOException;

    <E extends Enum<E> & IFlagEnum> EnumSet<E> readIntegerAsFlag(Class<E> enumeration) throws IOException;

    float readIntegerAsFloat() throws IOException;

    /**
     * @see java.io.RandomAccessFile.readShort()
     * @return
     * @throws IOException
     */
    short readRealShort() throws IOException;

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN signed short
     *
     * @return JAVA native short
     * @throws IOException may fail
     */
    short readShort() throws IOException;

    <E extends Enum & IValueEnum> E readShortAsEnum(Class<E> enumeration) throws IOException;

    <E extends Enum<E> & IFlagEnum> EnumSet<E> readShortAsFlag(Class<E> enumeration) throws IOException;

    float readShortAsFloat() throws IOException;

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param length string length
     * @see #ConversionUtils.toString(byte[])
     * @return fresh String
     * @throws IOException the reading may fail
     */
    String readString(int length) throws IOException;

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param length string length
     * @see #ConversionUtils.toStringUtf16(byte[])
     * @return fresh String
     * @throws IOException the reading may fail
     */
    String readStringUtf16(int length) throws IOException;

    /**
     * Reads a DK2 style timestamp
     *
     * @return the date in current locale
     * @throws IOException may fail
     */
    Date readTimestamp() throws IOException;

    short readUnsignedByte() throws IOException;

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int
     *
     * @return JAVA native int
     * @throws IOException may fail
     * @see #ResourceReader.readUnsignedIntegerAsLong()
     */
    int readUnsignedInteger() throws IOException;

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int<br>
     * This method returns long, which means the value is sure to fit
     *
     * @return JAVA native long
     * @throws IOException may fail
     */
    long readUnsignedIntegerAsLong() throws IOException;

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN unsigned short (needs to be int in JAVA)
     *
     * @return JAVA native int
     * @throws IOException may fail
     */
    int readUnsignedShort() throws IOException;

    /**
     * Reads string of varying length (ASCII NULL terminated) from the file
     *
     * @param length bytes to reed from file
     * @return string read from the file
     * @throws java.io.IOException
     */
    String readVaryingLengthString(int length) throws IOException;

    /**
     * Reads strings of varying length (UTF16 NULL terminated) from the file
     *
     * @param length max length of string
     * @return string read from the file
     * @throws IOException
     */
    String readVaryingLengthStringUtf16(int length) throws IOException;

    /**
     * Reads strings of varying length (ASCII NULL terminated) from the file
     *
     * @param numberOfStrings number of Strings to read
     * @return list of strings read from the file
     * @throws IOException
     */
    List<String> readVaryingLengthStrings(int numberOfStrings) throws IOException;

    void seek(long pos) throws IOException;

    int skipBytes(int size) throws IOException;

}
