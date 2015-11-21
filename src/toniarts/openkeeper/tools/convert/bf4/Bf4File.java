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
package toniarts.openkeeper.tools.convert.bf4;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.imageio.stream.MemoryCacheImageInputStream;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.bf4.Bf4Entry.FontEntryFlag;

/**
 * Reads the Dungeon Keeper 2 BF4 files, bitmap fonts that is<br>
 * Format reverse engineered by:
 * <li>George Gensure</li>
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Bf4File implements Iterable<Bf4Entry> {

    private static final String BF4_HEADER_IDENTIFIER = "F4FB";
    private final List<Bf4Entry> entries;
    private short maxWidth;
    private short maxHeight;
    private int maxCodePoint = 0;
    private int glyphCount = 0;
    private int avgWidth = 0;
    private static final int BITS_PER_PIXEL = 4;
    private static final IndexColorModel cm;

    static {

        // Create the palette
        byte[] levels = new byte[16];
        for (int c = 0; c < 16; c++) {
            levels[c] = (byte) ((c + 1) * 16 - 1);
        }
        cm = new IndexColorModel(BITS_PER_PIXEL, 16, levels, levels, levels, 0);
    }

    /**
     * Constructs a new BF4 file reader Reads the BF4 file structure
     *
     * @param file the bf4 file to read
     */
    public Bf4File(File file) {

        // Read the file
        try (RandomAccessFile rawBf4 = new RandomAccessFile(file, "r")) {

            // Check the header
            byte[] header = new byte[4];
            rawBf4.read(header);
            if (!BF4_HEADER_IDENTIFIER.equals(ConversionUtils.bytesToString(header))) {
                throw new RuntimeException("Header should be " + BF4_HEADER_IDENTIFIER + " and it was " + header + "! Cancelling!");
            }
            maxWidth = ConversionUtils.toUnsignedByte(rawBf4.readByte()); // This is know to be bogus value
            maxHeight = ConversionUtils.toUnsignedByte(rawBf4.readByte());
            int offsetsCount = ConversionUtils.readUnsignedShort(rawBf4);

            // Read the offsets
            List<Integer> offsets = new ArrayList<>(offsetsCount);
            for (int i = 0; i < offsetsCount; i++) {
                offsets.add(ConversionUtils.readUnsignedInteger(rawBf4));
            }

            // Read the font entries
            entries = new ArrayList<>(offsetsCount);
            for (Integer offset : offsets) {
                rawBf4.seek(offset);
                entries.add(readFontEntry(rawBf4));
            }

            // Sort them
            Collections.sort(entries);
            avgWidth = (int) Math.ceil((float) avgWidth / getGlyphCount());
        } catch (IOException e) {

            // Fug
            throw new RuntimeException("Failed to read the file " + file + "!", e);
        }
    }

    /**
     * Reads a single font entry from the file
     *
     * @param rawBf4 the file
     * @return the font entry
     * @throws IOException may fail
     */
    private Bf4Entry readFontEntry(RandomAccessFile rawBf4) throws IOException {
        Bf4Entry entry = new Bf4Entry();
        
        entry.setCharacter(ConversionUtils.bytesToStringUtf16(rawBf4, 1).charAt(0));
        entry.setUnknown1(ConversionUtils.readUnsignedShort(rawBf4));
        entry.setDataSize(ConversionUtils.readInteger(rawBf4));
        entry.setTotalSize(ConversionUtils.readUnsignedInteger(rawBf4));
        entry.setFlag(ConversionUtils.parseFlagValue(rawBf4.readUnsignedByte(), FontEntryFlag.class));
        entry.setUnknown2(ConversionUtils.toUnsignedByte(rawBf4.readByte()));
        entry.setUnknown3(ConversionUtils.toUnsignedByte(rawBf4.readByte()));
        entry.setUnknown4(ConversionUtils.toUnsignedByte(rawBf4.readByte()));
        entry.setWidth(ConversionUtils.readUnsignedShort(rawBf4));
        entry.setHeight(ConversionUtils.readUnsignedShort(rawBf4));
        entry.setOffsetX(rawBf4.readByte());
        entry.setOffsetY(rawBf4.readByte());
        entry.setOuterWidth(ConversionUtils.readShort(rawBf4));
        
        byte[] bytes;
        if (entry.getWidth() > 0 && entry.getHeight() > 0) {
            bytes = new byte[entry.getDataSize()];
            rawBf4.read(bytes, 0, entry.getDataSize());
            entry.setImage(decodeFontImage(entry, bytes));

            // Update the max values
            maxWidth = (short) Math.max(maxWidth, entry.getWidth());
            maxHeight = (short) Math.max(maxHeight, entry.getHeight());
            maxCodePoint = Math.max(maxCodePoint, entry.getCharacter());
            avgWidth += entry.getWidth();
            glyphCount++;
        }
        return entry;
    }

    /**
     * Decodes the given font image and stores returns it as a JAVA image. The
     * image has 16 color indexed grayscale palette, with 0 being totally
     * transparent. 4-bits per pixel.
     *
     * @param width width of the image
     * @param height height of the image
     * @param flag decoding/encoding flags
     * @param bytes tha data payload
     * @return image
     */
    private BufferedImage decodeFontImage(final Bf4Entry entry, final byte[] bytes) throws IOException {

        // Create the sample model
        MultiPixelPackedSampleModel sampleModel = new MultiPixelPackedSampleModel(DataBuffer.TYPE_BYTE,
                entry.getWidth(), entry.getHeight(),
                BITS_PER_PIXEL);

        // Create the image
        WritableRaster raster = Raster.createWritableRaster(sampleModel, null);
        BufferedImage bi = new BufferedImage(cm, raster, false, null);
        byte[] data = (byte[]) ((DataBufferByte) raster.getDataBuffer()).getData();

        // Compressions, the compressions might be applied in sequence, so just apply the decompressions one by one
        byte[] decodedBytes = new byte[Math.max(entry.getDataSize(), data.length)];
        System.arraycopy(bytes, 0, decodedBytes, 0, bytes.length);
        if (entry.getFlag().contains(FontEntryFlag.RLE4_DATA)) {
            decodeRLE4(decodedBytes);
        }
        if (entry.getFlag().contains(FontEntryFlag.UNKNOWN)) {
            throw new RuntimeException("The font uses unknown encoding!");
        }

        // Our images have no padding bits at the end of scanline strides, so write line by line if width is odd
        if (entry.getWidth() % 2 != 0) {
            MemoryCacheImageInputStream iis = new MemoryCacheImageInputStream(new ByteArrayInputStream(decodedBytes));
            iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            FourBitWriter writer = new FourBitWriter(data);
            for (int y = 0; y < entry.getHeight(); y++) {
                for (int x = 0; x < entry.getWidth(); x++) {
                    writer.write((int) iis.readBits(4));
                }

                // Write the padding
                writer.write(0);
            }
        } else {

            // Finally set the data to the image
            System.arraycopy(decodedBytes, 0, data, 0, data.length);
        }

        return bi;
    }

    /**
     * Decodes RLE 4-bit
     *
     * @param data data buffer, contains the compressed data, and target for the
     * decompressed data
     */
    private void decodeRLE4(byte[] data) throws IOException {
        int count;
        int value;
        byte[] values = new byte[data.length];
        System.arraycopy(data, 0, values, 0, data.length);
        MemoryCacheImageInputStream iis = new MemoryCacheImageInputStream(new ByteArrayInputStream(values));
        iis.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        FourBitWriter writer = new FourBitWriter(data);

        while (true) {

            // Read the value
            value = (int) iis.readBits(4);
            if (value == 0) {
                count = (int) iis.readBits(4);
                if (count != 0) {
                    value = (int) iis.readBits(4);
                    for (int i = 0; i < count; i++) {
                        writer.write(value);
                    }
                } else {
                    break; // End of stream
                }
            } else {

                // Just write it
                writer.write(value);
            }
        }
    }

    /**
     * Get average width of the image in pixels
     *
     * @return average image width
     */
    public int getAvgWidth() {
        return avgWidth;
    }

    @Override
    public Iterator<Bf4Entry> iterator() {
        return entries.iterator();
    }

    /**
     * Maximum font image height in pixels
     *
     * @return image height
     */
    public short getMaxHeight() {
        return maxHeight;
    }

    /**
     * Maximum font image width in pixels
     *
     * @return image width
     */
    public short getMaxWidth() {
        return maxWidth;
    }

    /**
     * Get the largest code point represented by this font file
     *
     * @return largest code point
     */
    public int getMaxCodePoint() {
        return maxCodePoint;
    }

    /**
     * Not all entries contain a font image
     *
     * @return the number of entries with font image
     */
    public int getGlyphCount() {
        return glyphCount;
    }

    /**
     * Get the color model of the font file
     *
     * @return the color model
     */
    public static IndexColorModel getCm() {
        return cm;
    }

    /**
     * Get the char count
     */
    public int getCount() {
        return entries.size();
    }

    /**
     * Small class to write in 4-bits
     */
    private class FourBitWriter {

        private final byte[] data;
        private int position = 0;
        private boolean wholeByte = true;

        public FourBitWriter(byte[] data) {
            this.data = data;
        }

        public void write(int value) {
            if (wholeByte) {
                data[position] = (byte) (value << 4);
            } else {
                data[position++] |= (value & 0x0F);
            }
            wholeByte = !wholeByte;
        }
    }
}
