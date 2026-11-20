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

import java.io.*;

/**
 * The
 * <code>Converter</code> class can be used to transform binary data to
 * characters.
 *
 * @author	Michael Scheerer
 */
final class Converter {

    private final static char BIG_ENDIAN = '\uFEFF';
    private final static char LITTLE_ENDIAN = '\uFFFE';
    final static int ISO8859_1 = 0;
    final static int UTF16 = 1;
    final static int UTF16BE = 2;
    final static int UTF8 = 3;

    String convert(byte b[], boolean unincoding, int coding) throws UnsupportedEncodingException {
        return convert(b, 0, b.length, unincoding, coding);
    }

    String convert(byte b[], int i, int j, boolean unincoding, int coding) throws UnsupportedEncodingException {
        if (unincoding) {
            if (coding == UTF16) {
                if (j - i < 3) {
                    throw new UnsupportedEncodingException();
                }
                int k = b[i++] & 0xFF;

                int l = b[i++] & 0xFF;

                char c = (char) (k << 8 | l);

                if (c == BIG_ENDIAN) {
                    return convertUTF16(b, i, i + j - 2, new char[j / 2], true);
                } else if (c == LITTLE_ENDIAN) {
                    return convertUTF16(b, i, i + j - 2, new char[j / 2], false);
                }
                throw new UnsupportedEncodingException();
            } else if (coding == UTF16BE) {
                if (j - i < 2) {
                    throw new UnsupportedEncodingException();
                }
                return convertUTF16BE(b, i, i + j, new char[j / 2]);
            } else if (coding == UTF8) {
                return convertUTF8(b, i, i + j);
            }
            throw new UnsupportedEncodingException();
        }
        return convert8859_1(b, i, i + j, new char[j]);
    }

    private String convert8859_1(byte b[], int i, int j, char c[]) {
        int charIndex = 0;

        for (int byteIndex = i; byteIndex < j;) {

            byte oneb = b[byteIndex++];

            if (oneb >= 0) {
                c[charIndex++] = (char) oneb;
            } else {
                c[charIndex++] = (char) (256 + oneb);
            }
        }
        return new String(c);
    }

    private String convertUTF16(byte b[], int i, int j, char c[], boolean big) {
        int k, charIndex = 0;

        char onec;

        int byteIndex = i;

        int l = b[byteIndex++] & 0xFF;

        while (byteIndex < j) {
            k = b[byteIndex++] & 0xFF;

            if (big) {
                onec = (char) (l << 8 | k);
            } else {
                onec = (char) (k << 8 | l);
            }

            if (byteIndex < j) {
                c[charIndex++] = onec;
                l = b[byteIndex++] & 0xFF;
            }
        }

        k = b[byteIndex++] & 0xFF;

        if (big) {
            onec = (char) (l << 8 | k);
        } else {
            onec = (char) (k << 8 | l);
        }

        c[charIndex] = onec;

        return new String(c);
    }

    private String convertUTF16BE(byte b[], int i, int j, char c[]) {
        int k, charIndex = 0;

        char onec;

        int byteIndex = i;

        int l = b[byteIndex++] & 0xFF;

        while (byteIndex < j) {
            k = b[byteIndex++] & 0xFF;

            onec = (char) (l << 8 | k);

            if (byteIndex < j) {
                c[charIndex++] = onec;
                l = b[byteIndex++] & 0xFF;
            }
        }

        k = b[byteIndex++] & 0xFF;

        onec = (char) (l << 8 | k);

        c[charIndex] = onec;

        return new String(c);
    }

    private String convertUTF8(byte b[], int i, int j) {
        int charIndex = 0, k, l, m, n;

        char onec = 0;

        StringBuilder buffer = new StringBuilder();

        for (int byteIndex = i; byteIndex < j;) {

            byte oneb = b[byteIndex++];

            if (oneb >= 0) {
                onec = (char) oneb;
            } else {
                k = oneb & 0xFF;

                if ((k >>> 5 & 0x7) == 6 && byteIndex < j) {
                    l = b[byteIndex++];
                    onec = (char) ((k & 0x1F) << 6 | l & 0x3F);
                } else if ((k >>> 4 & 0xF) == 14 && byteIndex < j - 1) {
                    l = b[byteIndex++];
                    m = b[byteIndex++];
                    onec = (char) ((k & 0xF) << 12 | (l & 0x3F) << 6 | m & 0x3F);
                } else if (byteIndex < j - 2) {
                    l = b[byteIndex++];
                    m = b[byteIndex++];
                    n = b[byteIndex++];
                    onec = (char) ((k & 0x7) << 18 | (l & 0x3F) << 12 | (m & 0x3F) << 6 | n & 0x3F);
                } else {
                    onec = ' ';
                }
            }
            buffer.append(onec);
        }
        return buffer.toString();
    }
}
