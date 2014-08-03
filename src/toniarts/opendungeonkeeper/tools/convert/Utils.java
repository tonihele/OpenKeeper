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
     * short
     *
     * @param file the file to read from
     * @return JAVA native short
     * @throws IOException may fail
     */
    public static short readUnsignedShort(RandomAccessFile file) throws IOException {
        byte[] unsignedShort = new byte[2];
        file.read(unsignedShort);
        return readUnsignedShort(unsignedShort);
    }

    /**
     * Converts 2 bytes to JAVA short from LITTLE ENDIAN unsigned shoer
     * presented by a byte array
     *
     * @param unsignedInt the byte array
     * @return JAVA native short
     */
    public static short readUnsignedShort(byte[] unsignedShort) {
        ByteBuffer buffer = ByteBuffer.wrap(unsignedShort);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
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
     * @return unsigned byte
     */
    public static byte toUnsignedByte(byte b) {
        return (byte) (b & 0xFF);
    }
}
