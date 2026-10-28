/* Copyright (C) 2003-2014 Michael Scheerer. All Rights Reserved. */

/*
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package toniarts.openkeeper.audio.plugins.decoder.tag.id3v2;

import java.io.IOException;
import java.io.InputStream;
import toniarts.openkeeper.audio.plugins.decoder.tag.TagException;

/**
 * The
 * <code>Header</code> class contains all parsing stuff of an IDv2 header. Note
 * that this class doesn't reset anything, if something gets wrong. This is the
 * task of the decoder implementation, which used this ID3v2 system.
 *
 * @author Michael Scheerer
 */
final class Header {

    private final static int MAXIMUM_TAGSIZE = 256000000;
    private final boolean unsynchronizated;
    private boolean extendedHeader;
    private boolean experimental;
    private boolean footer;
    private boolean tagIsUpdate;
    private boolean tagRestrictions;
    private final int version;
    private final int tagSize;
    private int extendedHeaderTagSize;
    private int paddingSize;
    private boolean protection;
    private long crc;
    private int skippedDataLength;
    private final ID3v2 tag;

    // Checks an ID3v2 header from an input stream.
    Header(InputStream stream, ID3v2 tag) throws TagException, IOException {
        this.tag = tag;

        byte[] b = new byte[5];

        int i;

        read(stream, b, 0, 3);

        if (((char) b[0] == 'I' && (char) b[1] == 'D' && (char) b[2] == '3')) {
            read(stream, b, 0, 2);
            if ((version = b[0]) != 255 || b[1] != 255) { // Version(e.g. 3) and Revision (e.g. 0)
                if (version < 2 || version > 4) {
                    tag.setSkippedDataLength(skippedDataLength);
                    throw new TagException("Wrong ID3v2 version");
                }
                i = read(stream) & 0xFF;
                unsynchronizated = (i >>> 7 & 0x1) == 1;
                if (version >= 3) {
                    extendedHeader = (i >>> 6 & 0x1) == 1;
                    experimental = (i >>> 5 & 0x1) == 1;
                    if (version == 4) {
                        footer = (i >>> 4 & 0x1) == 1;
                    }
                }
                read(stream, b, 0, 4);
                tagSize = b[0] << 21 | b[1] << 14 & 0xFF0000 | b[2] << 7 & 0xFF00 | b[3] & 0xFF;
                if (extendedHeader) {
                    int syncs = 0;

                    read(stream, b, 0, 4);
                    if (version == 3) {
                        extendedHeaderTagSize = b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;
                        extendedHeaderTagSize += 4;
                    } else {
                        extendedHeaderTagSize = b[0] << 21 | b[1] << 14 & 0xFF0000 | b[2] << 7 & 0xFF00 | b[3] & 0xFF;
                        skip(stream, 1);
                    }

                    read(stream, b, 0, 2);
                    i = b[0] << 8 & 0xFF00 | b[1] & 0xFF;

                    if (version == 3) {
                        protection = (i >>> 15 & 0x1) == 1;
                        read(stream, b, 0, 4);

                        paddingSize = b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;

                        if (protection) {
                            read(stream, b, 0, 4);
                            crc = b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;
                        }
                    } else {
                        tagIsUpdate = (i >>> 14 & 0x1) == 1;
                        if (tagIsUpdate) {
                            skip(stream, 1); // length always 0
                        }
                        protection = (i >>> 13 & 0x1) == 1;
                        if (protection) {
                            skip(stream, 1); // length always 5
                            read(stream, b, 0, 5);
                            // The CRC-32 is stored as an
                            // 35 bit synchsafe integer, leaving the upper four bits always
                            // zeroed.
                            crc = b[0] << 28 & 0x0F00000000L | b[1] << 21 & 0xFF000000 | b[2] << 14 & 0xFF0000 | b[3] << 7 & 0xFF00 | b[4] & 0xFF;
                        }
                        tagRestrictions = (i >>> 12 & 0x1) == 1;
                        if (tagRestrictions) {
                            skip(stream, 2); // length always 1
                        }
                    }
                }
            } else {
                tag.setSkippedDataLength(skippedDataLength);
                throw new TagException("Maleformed ID3v2 header");
            }
        } else {
            throw new TagException("No ID3v2 header");
        }
        if (getTagSize() > MAXIMUM_TAGSIZE) {
            tag.setSkippedDataLength(skippedDataLength);
            throw new TagException("Tag size overflow");
        }
    }

    boolean isUnsynchronizated() {
        return unsynchronizated;
    }

    int getTagSize() {
        if (extendedHeader) {
            return tagSize - extendedHeaderTagSize - paddingSize;
        }
        return tagSize;
    }

    boolean isProtected() {
        return protection;
    }

    long getCrc() {
        return crc;
    }

    int getVersion() {
        return version;
    }

    int getSkippedDataLength() {
        return skippedDataLength;
    }

    private int read(InputStream stream) throws IOException {
        skippedDataLength += 1;
        return stream.read();
    }

    private void read(InputStream stream, byte[] b, int i, int j) throws IOException {
        skippedDataLength += stream.read(b, i, j);
    }

    private void skip(InputStream stream, int i) throws IOException {
        skippedDataLength += stream.skip(i);
    }
}
