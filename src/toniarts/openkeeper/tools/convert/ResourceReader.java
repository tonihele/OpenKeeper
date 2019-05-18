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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author archdemon
 */
public class ResourceReader implements IResourceReader {

    private final RandomAccessFile file;

    private static final Logger LOGGER = Logger.getLogger(ResourceReader.class.getName());

    public ResourceReader(File file) throws FileNotFoundException {
        this.file = new RandomAccessFile(file, "r");
    }

    public ResourceReader(String filename) throws FileNotFoundException {
        this(new File(filename));
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.file.read(b);
    }

    @Override
    public byte[] read(int length) throws IOException {
        byte[] bytes = new byte[length];

        int result = this.file.read(bytes);
        if (result != length) {
            String message = "Error reading byte array. Expect %s bytes and %s given";
            throw new IOException(String.format(message, length, result));
        }

        return bytes;
    }

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int<br>
     * This method returns long, which means the value is sure to fit
     *
     * @return JAVA native long
     * @throws IOException may fail
     */
    @Override
    public long readUnsignedIntegerAsLong() throws IOException {
        return this.readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int
     *
     * @return JAVA native int
     * @throws IOException may fail
     * @see #ResourceReader.readUnsignedIntegerAsLong()
     */
    @Override
    public int readUnsignedInteger() throws IOException {
        byte[] unsignedInt = this.read(4);

        return ConversionUtils.toUnsignedInteger(unsignedInt);
    }

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN int
     *
     * @return JAVA native int
     * @throws IOException may fail
     */
    @Override
    public int readInteger() throws IOException {
        byte[] signedInt = this.read(4);

        return ConversionUtils.toInteger(signedInt);
    }

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN unsigned short (needs to be
     * int in JAVA)
     *
     * @return JAVA native int
     * @throws IOException may fail
     */
    @Override
    public int readUnsignedShort() throws IOException {
        byte[] unsignedShort = this.read(2);

        return ConversionUtils.toUnsignedShort(unsignedShort);
    }

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN signed short
     *
     * @return JAVA native short
     * @throws IOException may fail
     */
    @Override
    public short readShort() throws IOException {
        byte[] signedShort = this.read(2);

        return ConversionUtils.toShort(signedShort);
    }

    @Override
    public short readUnsignedByte() throws IOException {
        return (short) this.file.readUnsignedByte();
    }

    @Override
    public byte readByte() throws IOException {
        return this.file.readByte();
    }

    @Override
    public float readIntegerAsFloat() throws IOException {
        return this.readInteger() / ConversionUtils.FLOAT;
    }

    @Override
    public float readShortAsFloat() throws IOException {
        return this.readShort() / ConversionUtils.FLOAT;
    }

    /**
     * Reads 4 bytes and converts it to JAVA float from LITTLE ENDIAN float
     *
     * @return JAVA native float
     * @throws IOException may fail
     */
    @Override
    public float readFloat() throws IOException {
        byte[] f = this.read(4);

        return ConversionUtils.toFloat(f);
    }

    /**
     * @see java.io.RandomAccessFile.readShort()
     * @return
     * @throws IOException
     */
    @Override
    public short readRealShort() throws IOException {
        return this.file.readShort();
    }

    @Override
    public float readIntegerAsDouble() throws IOException {
        return this.readInteger() / ConversionUtils.DOUBLE;
    }

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param length string length
     * @see #ConversionUtils.toString(byte[])
     * @return fresh String
     * @throws IOException the reading may fail
     */
    @Override
    public String readString(int length) throws IOException {
        byte[] bytes = this.read(length);

        return ConversionUtils.toString(bytes);
    }

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param length string length
     * @see #ConversionUtils.toStringUtf16(byte[])
     * @return fresh String
     * @throws IOException the reading may fail
     */
    @Override
    public String readStringUtf16(int length) throws IOException {
        byte[] bytes = this.read(length * 2);

        return ConversionUtils.toStringUtf16(bytes);
    }

    /**
     * Reads strings of varying length (UTF16 NULL terminated) from the file
     *
     * @param length max length of string
     * @return string read from the file
     * @throws IOException
     */
    @Override
    public String readVaryingLengthStringUtf16(int length) throws IOException {

        byte[] bytes = this.read(length * 2);
        List<Byte> result = new ArrayList<>();

        for (int i = 0; i < bytes.length; i += 2) {
            if (bytes[i] == 0 && bytes[i + 1] == 0) {
                break;
            }
            result.add(bytes[i]);
            result.add(bytes[i + 1]);
        }

        return ConversionUtils.toStringUtf16(ConversionUtils.toByteArray(result));
    }

    /**
     * Reads strings of varying length (ASCII NULL terminated) from the file
     *
     * @param numberOfStrings number of Strings to read
     * @return list of strings read from the file
     * @throws IOException
     */
    @Override
    public List<String> readVaryingLengthStrings(int numberOfStrings) throws IOException {
        List<String> strings = new ArrayList<>(numberOfStrings);

        for (int i = 0; i < numberOfStrings; i++) {

            // A bit tricky, read until 0 byte
            List<Byte> bytes = new ArrayList();
            byte b = 0;
            do {
                b = this.readByte();
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

    /**
     * Reads string of varying length (ASCII NULL terminated) from the file
     *
     * @param length bytes to reed from file
     * @return string read from the file
     * @throws java.io.IOException
     */
    @Override
    public String readVaryingLengthString(int length) throws IOException {
        byte[] bytes = this.read(length);
        List<Byte> string = new ArrayList();
        // A bit tricky, read until 0 byte
        for (byte b : bytes) {
            if (b == 0) {
                break;
            }
            string.add(b);
        }

        return ConversionUtils.toString(ConversionUtils.toByteArray(string));
    }

    /**
     * Reads a DK2 style timestamp
     *
     * @return the date in current locale
     * @throws IOException may fail
     */
    @Override
    public Date readTimestamp() throws IOException {
        // Dates are in UTC
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, this.readUnsignedShort());
        cal.set(Calendar.DAY_OF_MONTH, this.readUnsignedByte());
        cal.set(Calendar.MONTH, this.readUnsignedByte());
        this.skipBytes(2);
        cal.set(Calendar.HOUR_OF_DAY, this.readUnsignedByte());
        cal.set(Calendar.MINUTE, this.readUnsignedByte());
        cal.set(Calendar.SECOND, this.readUnsignedByte());
        this.skipBytes(1);

        return cal.getTime();
    }

    @Override
    public <E extends Enum & IValueEnum> E readIntegerAsEnum(Class<E> enumeration) throws IOException {
        return ConversionUtils.parseEnum(this.readUnsignedInteger(), enumeration);
    }

    @Override
    public <E extends Enum & IValueEnum> E readShortAsEnum(Class<E> enumeration) throws IOException {
        return ConversionUtils.parseEnum(this.readUnsignedShort(), enumeration);
    }

    @Override
    public <E extends Enum & IValueEnum> E readByteAsEnum(Class<E> enumeration) throws IOException {
        return ConversionUtils.parseEnum(this.readUnsignedByte(), enumeration);
    }

    @Override
    public <E extends Enum<E> & IFlagEnum> EnumSet<E> readIntegerAsFlag(Class<E> enumeration) throws IOException {
        return ConversionUtils.parseFlagValue(this.readUnsignedIntegerAsLong(), enumeration);
    }

    @Override
    public <E extends Enum<E> & IFlagEnum> EnumSet<E> readShortAsFlag(Class<E> enumeration) throws IOException {
        return ConversionUtils.parseFlagValue(this.readUnsignedShort(), enumeration);
    }

    @Override
    public <E extends Enum<E> & IFlagEnum> EnumSet<E> readByteAsFlag(Class<E> enumeration) throws IOException {
        return ConversionUtils.parseFlagValue(this.readUnsignedByte(), enumeration);
    }

    /**
     * Change this function to skipBytes when all resources will be decoded
     *
     * @param size number of bytes
     * @throws IOException
     * @deprecated
     */
    @Deprecated
    @Override
    public void readAndCheckNull(int size) throws IOException {
        byte[] bytes = this.read(size);

        for (byte b : bytes) {
            if (b != 0) {
                LOGGER.log(Level.WARNING, "Value not 0! Was {0}!", b);
            }
        }
    }

    @Override
    public int skipBytes(int size) throws IOException {
        int result = this.file.skipBytes(size);

        if (result != size) {
            String message = "Error skipping bytes. Expect %s bytes and %s given";
            throw new IOException(String.format(message, size, result));
        }

        return result;
    }

    @Override
    public long getFilePointer() throws IOException {
        return this.file.getFilePointer();
    }

    @Override
    public void seek(long pos) throws IOException {
        this.file.seek(pos);
    }

    @Override
    public long length() throws IOException {
        return this.file.length();
    }

    /**
     * Not all the data types are of the length that suits us, do our best to ignore it<br>
     * Skips the file to the correct position after an item is read<br>
     * <b>Use this directly with Things & Triggers!</b>
     *
     * @throws java.io.IOException
     * @see #toniarts.opendungeonkeeper.tools.convert.map.KwdFile.checkOffset(
     * toniarts.opendungeonkeeper.tools.convert.map.KwdFile.KwdHeader, java.io.RandomAccessFile,
     * long)
     * @param itemSize the item size
     * @param offset the file offset before the last item was read
     */
    @Override
    public void checkOffset(long itemSize, long offset) throws IOException {
        long expected = offset + itemSize;
        if (this.getFilePointer() != expected) {
            LOGGER.log(Level.WARNING, "Record size differs from expected! File offset is {0} and should be {1}!",
                    new Object[]{this.getFilePointer(), expected});
            this.seek(expected);
        }
    }

    /**
     * End of file
     *
     * @return true if filepointer >= length of file
     * @throws IOException
     */
    @Override
    public boolean isEof() throws IOException {
        return this.getFilePointer() >= this.length();
    }

    @Override
    public void close() {
        try {
            this.file.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Closing file error", ex);
        }
    }
}
