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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains static helper methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ConversionUtils {

    private static final Logger logger = Logger.getLogger(ConversionUtils.class.getName());
    private static final HashMap<String, String> fileNameCache = new HashMap<>();
    private static final Object fileNameLock = new Object();

    public static final float FLOAT = 4096f; // or DIVIDER_FLOAT Fixed Point Single Precision Divider
    public static final float DOUBLE = 65536f; // or DIVIDER_DOUBLE Fixed Point Double Precision Divider

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned
     * int<br>
     * This method returns long, which means the value is sure to fit
     *
     * @param file the file to read from
     * @return JAVA native long
     * @throws IOException may fail
     */
    public static long readUnsignedIntegerAsLong(RandomAccessFile file) throws IOException {
        return ConversionUtils.readInteger(file) & 0xFFFFFFFFL;
    }

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN unsigned int
     *
     * @param file the file to read from
     * @return JAVA native int
     * @throws IOException may fail
     * @see #readUnsignedIntegerAsLong(java.io.RandomAccessFile)
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
     * @see #readUnsignedIntegerAsLong(java.io.RandomAccessFile)
     */
    public static int readUnsignedInteger(byte[] unsignedInt) {
        int result = readInteger(unsignedInt);
        if (result < 0) {

            // Yes, this should be long, however, in our purpose this might be sufficient as int
            // Safety measure
            logger.warning("This unsigned integer doesn't fit to JAVA integer! Use a different method!");
        }
        return result;
    }

    /**
     * Reads 4 bytes and converts it to JAVA int from LITTLE ENDIAN int
     *
     * @param file the file to read from
     * @return JAVA native int
     * @throws IOException may fail
     */
    public static int readInteger(RandomAccessFile file) throws IOException {
        byte[] signedInt = new byte[4];
        file.read(signedInt);
        return readInteger(signedInt);
    }

    /**
     * Converts 4 bytes to JAVA int from LITTLE ENDIAN int presented by a byte
     * array
     *
     * @param signedInt the byte array
     * @return JAVA native int
     */
    public static int readInteger(byte[] signedInt) {
        ByteBuffer buffer = ByteBuffer.wrap(signedInt);
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
     * @param unsignedShort the byte array
     * @return JAVA native int
     */
    public static int readUnsignedShort(byte[] unsignedShort) {
        return readShort(unsignedShort) & 0xFFFF;
    }

    /**
     * Reads 2 bytes and converts it to JAVA short from LITTLE ENDIAN signed
     * short
     *
     * @param file the file to read from
     * @return JAVA native short
     * @throws IOException may fail
     */
    public static short readShort(RandomAccessFile file) throws IOException {
        byte[] signedShort = new byte[2];
        file.read(signedShort);
        return readShort(signedShort);
    }

    /**
     * Converts 2 bytes to JAVA short from LITTLE ENDIAN signed short presented
     * by a byte array
     *
     * @param signedShort the byte array
     * @return JAVA native short
     */
    public static short readShort(byte[] signedShort) {
        ByteBuffer buffer = ByteBuffer.wrap(signedShort);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    /**
     * Reads 4 bytes and converts it to JAVA float from LITTLE ENDIAN float
     *
     * @param file the file to read from
     * @return JAVA native float
     * @throws IOException may fail
     */
    public static float readFloat(RandomAccessFile file) throws IOException {
        byte[] f = new byte[4];
        file.read(f);
        return readFloat(f);
    }

    /**
     * Converts 4 bytes to JAVA float from LITTLE ENDIAN float presented by a
     * byte array
     *
     * @param unsignedInt the byte array
     * @return JAVA native float
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
     * @see #bytesToString(java.io.RandomAccessFile, int)
     * @return fresh String
     */
    public static String bytesToString(byte[] bytes) {
        return new String(bytes, Charset.forName("US-ASCII"));
    }

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param file the file
     * @param length string length
     * @see #bytesToString(byte[])
     * @return fresh String
     * @throws IOException the reading may fail
     */
    public static String bytesToString(RandomAccessFile file, int length) throws IOException {
        byte[] bytes = new byte[length];
        file.read(bytes);
        return bytesToString(bytes);
    }

    /**
     * Reads bytes from a file and converts them to a string
     *
     * @param file the file
     * @param length string length
     * @see #bytesToStringUtf16(byte[])
     * @return fresh String
     * @throws IOException the reading may fail
     */
    public static String bytesToStringUtf16(RandomAccessFile file, int length) throws IOException {
        byte[] bytes = new byte[length * 2];
        file.read(bytes);
        return bytesToStringUtf16(bytes);
    }

    /**
     * Converts a byte array to a JAVA String<br>
     * The byte array string is assumed UTF16 (wide strings in C), LITTLE ENDIAN
     *
     * @param bytes the bytearray to convert
     * @return fresh String
     */
    public static String bytesToStringUtf16(byte[] bytes) {
        return new String(bytes, Charset.forName("UTF_16LE"));
    }

    /**
     * Reads strings of varying length (UTF16 NULL terminated) from the file
     *
     * @param file the file to read from
     * @param length max length of string
     * @return string read from the file
     * @throws IOException
     */
    public static String readVaryingLengthStringUtf16(RandomAccessFile file, int length) throws IOException {

        byte[] bytes = new byte[length * 2];
        file.read(bytes);

        List<Byte> result = new ArrayList<>();

        for (int i = 0; i < bytes.length; i += 2) {
            if (bytes[i] == 0 && bytes[i + 1] == 0) {
                break;
            }
            result.add(bytes[i]);
            result.add(bytes[i + 1]);
        }

        return ConversionUtils.bytesToStringUtf16(toByteArray(result));
    }

    /**
        * Reads a DK2 style timestamp
        *
        * @param file the file to read from
        * @return the date in current locale
        * @throws IOException may fail
        */
       public static Date readTimestamp(RandomAccessFile file) throws IOException {

           // Dates are in UTC
           Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
           cal.set(Calendar.YEAR, ConversionUtils.readUnsignedShort(file));
           cal.set(Calendar.DAY_OF_MONTH, file.readUnsignedByte());
           cal.set(Calendar.MONTH, file.readUnsignedByte());
           file.skipBytes(2);
           cal.set(Calendar.HOUR_OF_DAY, file.readUnsignedByte());
           cal.set(Calendar.MINUTE, file.readUnsignedByte());
           cal.set(Calendar.SECOND, file.readUnsignedByte());
           file.skipBytes(1);
           return cal.getTime();
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

    @Deprecated
    public static void checkNull(RandomAccessFile file, int size) throws IOException {
        byte[] bytes = new byte[size];
        file.read(bytes);
        for (byte b : bytes) {
            if (b != 0) {
                throw new RuntimeException("value not equal 0");
            }
        }
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
            strings.add(ConversionUtils.bytesToString(toByteArray(bytes)));
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

    /**
     * Strip file name clean from any illegal characters, replaces the illegal
     * characters with an underscore
     *
     * @param fileName the file name to be stripped
     * @return returns stripped down file name
     */
    public static String stripFileName(String fileName) {
        return fileName.replaceAll("[[^a-zA-Z0-9][\\.]]", "_");
    }

    /**
     * Returns case sensitive and valid asset key for loading the given asset
     *
     * @param asset the asset key, i.e. Textures\GUI/wrongCase.png
     * @return fully qualified and working asset key
     */
    public static String getCanonicalAssetKey(String asset) {
        String assetsFolder = AssetsConverter.getAssetsFolder();
        try {
            File file = new File(getRealFileName(assetsFolder, asset)).getCanonicalFile();
            return file.getPath().substring(assetsFolder.length()).replaceAll(Pattern.quote(File.separator), "/");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Can not locate asset " + asset + "!", e);
            return asset;
        }
    }

    /**
     * Converts all the file separators to current system separators
     *
     * @param fileName the file name to convert
     * @return the file name with native file separators
     */
    public static String convertFileSeparators(String fileName) {
        return fileName.replaceAll("[/\\\\]", Matcher.quoteReplacement(File.separator));
    }

    /**
     * Gets real file name for a file, this is to ignore file system case
     * sensitivity<br>
     * Does a recursive search
     *
     * @param realPath the real path that surely exists (<strong>case
     * sensitive!!</strong>), serves as a root for the searching
     * @param uncertainPath the file (and/or directory) to find from the real
     * path
     * @return the case sensitive fully working file name
     * @throws IOException if file is not found
     */
    public static String getRealFileName(final String realPath, String uncertainPath) throws IOException {

        // Make sure that the uncertain path's separators are system separators
        uncertainPath = convertFileSeparators(uncertainPath);

        String fileName = realPath.concat(uncertainPath);

        // If it exists as such, that is super!
        File testFile = new File(fileName);
        if (testFile.exists()) {
            return testFile.getCanonicalPath();
        } else {

            // See cache
            if (fileNameCache.containsKey(fileName)) {
                return fileNameCache.get(fileName);
            }
        }

        // Otherwise we need to do a recursive search
        synchronized (fileNameLock) {
            final String[] path = uncertainPath.split(Matcher.quoteReplacement(File.separator));
            final Path realPathAsPath = new File(realPath).toPath();
            FileFinder fileFinder = new FileFinder(realPathAsPath, path);
            Files.walkFileTree(realPathAsPath, fileFinder);
            fileNameCache.put(fileName, fileFinder.file);
            if (fileFinder.file == null) {
                throw new IOException("File not found " + testFile + "!");
            }
            return fileFinder.file;
        }
    }

    /**
     * Parse a flag to enumeration set of given class
     *
     * @param flag the flag value
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
                leftOver = leftOver - flagValue;
            }
        }
        if (leftOver > 0) {

            // Check the values not defined (there must be a better way to do this but me and numbers...)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 64; i++) {
                long val = (int) Math.pow(2, i);
                if (val > leftOver) {
                    break;
                } else if ((val & leftOver) == val) {
                    if (sb.length() > 0) {
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

    /**
     * File finder, recursively tries to find a file ignoring case
     */
    private static class FileFinder extends SimpleFileVisitor<Path> {

        private int level = 0;
        private String file;
        private final Path startingPath;
        private final String[] path;

        private FileFinder(Path startingPath, String[] path) {
            this.startingPath = startingPath;
            this.path = path;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (startingPath.equals(dir)) {
                return FileVisitResult.CONTINUE; // Just the root
            } else if (level < path.length - 1 && startingPath.relativize(dir).getName(level).toString().equalsIgnoreCase(path[level])) {
                level++;
                return FileVisitResult.CONTINUE;
            }
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

            // See if this is the file we are looking for
            if (level == path.length - 1 && file.getName(file.getNameCount() - 1).toString().equalsIgnoreCase(path[level])) {
                this.file = file.toFile().getCanonicalPath();
                return FileVisitResult.TERMINATE;
            }

            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            return FileVisitResult.TERMINATE; // We already missed our window here
        }
    }
}
