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
 * <code>FrameContent</code> class contains the information related to a ID3(v2)
 * tag frame and contains all possible data converting handlings (UNICODE-, UTF8
 * or ISO8859_1-bytes to characters). The encoded data source containing all
 * informations of a frame may be partitioned into several binary and text
 * decoded data segments. The text decoded data segments are 00000000 (or
 * 0000000000000000 for Unicode) bitpattern terminated.
 *
 * @author Michael Scheerer
 */
final class FrameContent {

    private byte[] encodedData;
    private final int size;
    private int readPos;
    private boolean unincoding;
    private int coding;
    private Converter con = new Converter();

    FrameContent(byte[] b, boolean checkCharEncodingType) {
        encodedData = b;
        size = b.length;

        if (checkCharEncodingType) {
            unincoding = encodedData[0] >= 1;
            coding = encodedData[0];
            readPos++;
        }
    }

    String read() {
        return read(unincoding);
    }

    String read(boolean unincoding) {
        String ret;

        int i = readPos;

        if (unincoding && coding != Converter.UTF8) {
            for (; i < size - 1 && (encodedData[i] != 0 || encodedData[i + 1] != 0); i++) {
            }
            if (i == size - 1 && encodedData[i] != 0) {
                i++;
            }
        } else {
            for (; i < size && encodedData[i] != 0; i++) {
            }
        }
        try {
            ret = con.convert(encodedData, readPos, i - readPos, unincoding, coding);
        } catch (UnsupportedEncodingException e) {
            ret = "";
        }
        readPos = i + (unincoding && coding != Converter.UTF8 ? 2 : 1);
        return ret;
    }

    byte[] read(int l) {
        if (l == -1) {
            l = size - readPos;
        }
        if (readPos + l > size) {
            l = size - readPos;
        }

        byte[] ret = new byte[l];

        System.arraycopy(encodedData, readPos, ret, 0, l);

        readPos += l;
        return ret;
    }

    String read(int l, boolean b) throws UnsupportedEncodingException {
        return con.convert(read(l), b, coding);
    }

    void close() {
        encodedData = null;
        con = null;
    }
}
