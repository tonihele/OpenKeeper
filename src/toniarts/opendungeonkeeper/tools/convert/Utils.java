/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package toniarts.opendungeonkeeper.tools.convert;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains static helper methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Utils {

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int
     *
     * @param file the file to read from
     * @return JAVA native int
     * @throws IOException may fail
     */
    public static int readUnsignedInteger(RandomAccessFile file) throws IOException {
        byte[] unsignedInt = new byte[4];
        file.read(unsignedInt);
        return readUnsignedInteger(unsignedInt);
    }

    /**
     * Converts 4 bytes to JAVA int from LITTLE ENDIAN unsigned int presented by
     * a byte array
     *
     * @param unsignedInt the byte array
     * @return JAVA native int
     */
    public static int readUnsignedInteger(byte[] unsignedInt) {
        ByteBuffer buffer = ByteBuffer.wrap(unsignedInt);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
    }

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN unsigned
     * short (needs to be int in JAVA)
     *
     * @param file the file to read from
     * @return JAVA native int
     * @throws IOException may fail
     */
    public static int readUnsignedShort(RandomAccessFile file) throws IOException {
        byte[] unsignedShort = new byte[2];
        file.read(unsignedShort);
        return readUnsignedShort(unsignedShort);
    }

    /**
     * Converts 2 bytes to JAVA short from LITTLE ENDIAN unsigned short
     * presented by a byte array (needs to be int in JAVA)
     *
     * @param unsignedInt the byte array
     * @return JAVA native int
     */
    public static int readUnsignedShort(byte[] unsignedShort) {
        ByteBuffer buffer = ByteBuffer.wrap(unsignedShort);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort() & 0xFFFF;
    }

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int
     *
     * @param file the file to read from
     * @return JAVA native int
     * @throws IOException may fail
     */
    public static float readFloat(RandomAccessFile file) throws IOException {
        byte[] f = new byte[4];
        file.read(f);
        return readFloat(f);
    }

    /**
     * Converts 4 bytes to JAVA int from LITTLE ENDIAN unsigned int presented by
     * a byte array
     *
     * @param unsignedInt the byte array
     * @return JAVA native int
     */
    public static float readFloat(byte[] f) {
        ByteBuffer buffer = ByteBuffer.wrap(f);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getFloat();
    }

    /**
     * Converts a byte array to a JAVA String
     *
     * @param bytes the bytearray to convert
     * @return fresh String
     */
    public static String bytesToString(byte[] bytes) {
        return new String(bytes, Charset.forName("US-ASCII"));
    }

    /**
     * Convert a byte to unsigned byte
     *
     * @param b byte
     * @return unsigned byte (needs to be short in JAVA)
     */
    public static short toUnsignedByte(byte b) {
        return Integer.valueOf(b & 0xFF).shortValue();
    }

    /**
     * Reads strings of varying length (ASCII NULL terminated) from the file
     *
     * @param rawKmf the file to read from
     * @param numberOfStrings number of Strings to read
     * @return list of strings read from the file
     * @throws IOException
     */
    public static List<String> readVaryingLengthStrings(RandomAccessFile rawKmf, int numberOfStrings) throws IOException {
        List<String> strings = new ArrayList<>(numberOfStrings);

        for (int i = 0; i < numberOfStrings; i++) {

            // A bit tricky, read until 0 byte
            List<Byte> bytes = new ArrayList();
            byte b = 0;
            do {
                b = rawKmf.readByte();
                if (b != 0) {
                    bytes.add(b);
                } else {
                    break;
                }
            } while (true);
            strings.add(Utils.bytesToString(toByteArray(bytes)));
        }
        return strings;
    }

    /**
     * Converts a list of bytes to an array of bytes
     *
     * @param bytes the list of bytes
     * @return the byte array
     */
    public static byte[] toByteArray(List<Byte> bytes) {
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        for (Byte b : bytes) {
            byteArray[i] = b.byteValue();
            i++;
        }
        return byteArray;
    }

    /**
     * Bit play<br>
     * http://stackoverflow.com/questions/11419501/converting-bits-in-to-integer
     *
     * @param n bytes converted to int
     * @param offset from where to read (bits index)
     * @param length how many bits to read
     * @return integer represented by the bits
     */
    public static int bits(int n, int offset, int length) {

        //Shift the bits rightward, so that the desired chunk is at the right end
        n = n >> (31 - offset - length);

        //Prepare a mask where only the rightmost `length`  bits are 1's
        int mask = ~(-1 << length);

        //Zero out all bits but the right chunk
        return n & mask;
    }
}
