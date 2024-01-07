/*
 * Copyright (C) 2014-2015 OpenKeeper
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

import com.jme3.math.Vector3f;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.List;


/**
 * Contains static helper methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConversionUtils {

    private static final Logger logger = System.getLogger(ConversionUtils.class.getName());

    public static final float FLOAT = 4096f; // or DIVIDER_FLOAT Fixed Point Single Precision Divider
    public static final float DOUBLE = 65536f; // or DIVIDER_DOUBLE Fixed Point Double Precision Divider

    /**
     * Converts 4 bytes to JAVA int from LITTLE ENDIAN unsigned int presented by
     * a byte array
     *
     * @param unsignedInt the byte array
     * @return JAVA native int
     * @see toniarts.openkeeper.tools.convert.IResourceReader#readUnsignedIntegerAsLong()
     */
    public static int toUnsignedInteger(byte[] unsignedInt) {
        int result = toInteger(unsignedInt);
        if (result < 0) {

            // Yes, this should be long, however, in our purpose this might be sufficient as int
            // Safety measure
            logger.log(Level.WARNING, "This unsigned integer doesn't fit to JAVA integer! Use a different method!");
        }
        return result;
    }

    /**
     * Converts 4 bytes to JAVA int from LITTLE ENDIAN int presented by a byte
     * array
     *
     * @param signedInt the byte array
     * @return JAVA native int
     */
    public static int toInteger(byte[] signedInt) {
        ByteBuffer buffer = ByteBuffer.wrap(signedInt);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
    }

    /**
     * Converts 2 bytes to JAVA short from LITTLE ENDIAN unsigned short
     * presented by a byte array (needs to be int in JAVA)
     *
     * @param unsignedShort the byte array
     * @return JAVA native int
     */
    public static int toUnsignedShort(byte[] unsignedShort) {
        return toShort(unsignedShort) & 0xFFFF;
    }

    /**
     * Converts 2 bytes to JAVA short from LITTLE ENDIAN signed short presented
     * by a byte array
     *
     * @param signedShort the byte array
     * @return JAVA native short
     */
    public static short toShort(byte[] signedShort) {
        ByteBuffer buffer = ByteBuffer.wrap(signedShort);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    /**
     * Converts 4 bytes to JAVA float from LITTLE ENDIAN float presented by a
     * byte array
     *
     * @param f the byte array
     * @return JAVA native float
     */
    public static float toFloat(byte[] f) {
        ByteBuffer buffer = ByteBuffer.wrap(f);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getFloat();
    }

    /**
     * Converts a byte array to a JAVA String
     *
     * @param bytes the bytearray to convert
     * @see toniarts.openkeeper.tools.convert.IResourceReader#readString(int)
     * @return fresh String
     */
    public static String toString(byte[] bytes) {
        return new String(bytes, Charset.forName("windows-1252"));
    }

    /**
     * Converts a byte array to a JAVA String<br>
     * The byte array string is assumed UTF16 (wide strings in C), LITTLE ENDIAN
     *
     * @param bytes the bytearray to convert
     * @return fresh String
     */
    public static String toStringUtf16(byte[] bytes) {
        return new String(bytes, Charset.forName("UTF_16LE"));
    }

    /**
     * Converts JAVAX 3f vector to JME vector (also converts the coordinate
     * system)
     *
     * @param v vector
     * @return JME vector
     */
    public static Vector3f convertVector(javax.vecmath.Vector3f v) {
        return new Vector3f(v.x, -v.z, v.y);
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
     * Converts a list of bytes to an array of bytes
     *
     * @param bytes the list of bytes
     * @return the byte array
     */
    public static byte[] toByteArray(List<Byte> bytes) {
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        for (Byte b : bytes) {
            byteArray[i] = b;
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

    /**
     * Parse a flag to enumeration set of given class
     *
     * @param flag the flag value
     * @param <E> enumeration class
     * @param enumeration the enumeration class
     * @return the set
     */
    public static <E extends Enum<E> & IFlagEnum> EnumSet<E> parseFlagValue(long flag, Class<E> enumeration) {
        long leftOver = flag;
        EnumSet<E> set = EnumSet.noneOf(enumeration);
        for (E e : enumeration.getEnumConstants()) {
            long flagValue = e.getFlagValue();
            if ((flagValue & flag) == flagValue) {
                set.add(e);
                leftOver -= flagValue;
            }
        }
        if (leftOver > 0) {

            // Check the values not defined (there must be a better way to do this but me and numbers...)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 64; i++) {
                long val = (long) Math.pow(2, i);
                if (val > leftOver) {
                    break;
                } else if ((val & leftOver) == val) {
                    if (!sb.isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append(val);
                }
            }
            logger.log(Level.WARNING, "Value(s) {0} not specified for enum set class {1}!", new java.lang.Object[]{sb.toString(), enumeration.getName()});
        }
        return set;
    }

    /**
     * Parses a value to a enum of a wanted enum class
     *
     * @param <E> The enumeration class
     * @param value the id value
     * @param enumeration the enumeration class
     * @return Enum value, returns null if no enum is found with given value
     */
    public static <E extends Enum & IValueEnum> E parseEnum(int value, Class<E> enumeration) {
        for (E e : enumeration.getEnumConstants()) {
            if (e.getValue() == value) {
                return e;
            }
        }
        logger.log(Level.WARNING, "Value {0} not specified for enum class {1}!", new java.lang.Object[]{value, enumeration.getName()});
        return null;

    }
}
