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
import java.util.zip.*;
import toniarts.openkeeper.audio.plugins.decoder.tag.TagException;

/**
 * The
 * <code>Frame</code> class contains all parsing stuff of an IDv2 frame. Also
 * the encodedData of one ID3v2 frame is handled. Unsynchronization and
 * Encryption/Deencryption is done in the
 * <code>IDv2</code> class, but GZIP-deflating is done here in case of
 * compressed data.
 *
 * @author Michael Scheerer
 */
final class Frame {

    private final static int MAXIMUM_FRAMESIZE = 16000000;
    private final static int MAXIMUM_SHORT_FRAMESIZE = 1600000;
    private final static int SCAN_RANGE = 10000;
    private boolean tagAlterPreservation;
    private boolean fileAlterPreservation;
    private boolean readOnly;
    private boolean compression;
    private boolean encrypted;
    private boolean grouped;
    private boolean unsynchronizated;
    private boolean dataLengthIndication;
    private int dataLengthIndicator;
    private String shortKey;
    private byte[] encodedData;
    private byte groupId;
    private byte encryptionId;
    private int frameLength;
    private int skippedDataLength;
    private int version;
    private int unflatedFrameLength;
    private int state;
    private ID3v2 tag;

    Frame(InputStream stream, ID3v2 tag, int version) throws IOException, TagException {
        skippedDataLength = 0;
        state = 0;
        byte[] b = new byte[4];
        int i;

        this.version = version;
        this.tag = tag;
        if (version >= 3) {
            stream.read(b, 0, 4);

            if (b[0] == 0) { // last frame is reaches -> terminus signal
                shortKey = null;
                stream.skip(6);
                return;
            }
            if (!verifyKeyString((int) b[0], (int) b[1], (int) b[2], (int) b[3])) {
                if (!tag.containsValue(new String(b))) {
                    if (!sync(stream, b, version)) {
                        shortKey = "DUMMY";
                        return;
                    }
                }
            }
            StringBuilder buf = new StringBuilder();

            buf.append((char) (b[0] & 0xFF));
            buf.append((char) (b[1] & 0xFF));
            buf.append((char) (b[2] & 0xFF));
            buf.append((char) (b[3] & 0xFF));
            shortKey = buf.toString();

            stream.read(b, 0, 4);

            if (version == 3) {
                frameLength = b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;

                stream.read(b, 0, 2);

                i = b[0] << 8 & 0xFF00 | b[1] & 0xFF;

                tagAlterPreservation = (i >>> 15 & 0x1) == 1 ? true : false;
                fileAlterPreservation = (i >>> 14 & 0x1) == 1 ? true : false;
                readOnly = (i >>> 13 & 0x1) == 1 ? true : false;
                compression = (i >>> 7 & 0x1) == 1 ? true : false;
                encrypted = (i >>> 6 & 0x1) == 1 ? true : false;
                grouped = (i >>> 5 & 0x1) == 1 ? true : false;

                if (compression) {
                    frameLength -= stream.read(b, 0, 4);
                    unflatedFrameLength = b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;
                }
                if (encrypted) {
                    encryptionId = (byte) (stream.read() & 0xFF);
                    frameLength--;
                }
                if (grouped) {
                    groupId = (byte) (stream.read() & 0xFF);
                    frameLength--;
                }
            } else {
                frameLength = b[0] << 21 | b[1] << 14 & 0xFF0000 | b[2] << 7 & 0xFF00 | b[3] & 0xFF;

                stream.read(b, 0, 2);

                i = b[0] << 8 & 0xFF00 | b[1] & 0xFF;

                tagAlterPreservation = (i >>> 14 & 0x1) == 1 ? true : false;
                fileAlterPreservation = (i >>> 13 & 0x1) == 1 ? true : false;
                readOnly = (i >>> 12 & 0x1) == 1 ? true : false;
                grouped = (i >>> 6 & 0x1) == 1 ? true : false;
                compression = (i >>> 3 & 0x1) == 1 ? true : false;
                encrypted = (i >>> 2 & 0x1) == 1 ? true : false;
                unsynchronizated = (i >>> 1 & 0x1) == 1 ? true : false;
                dataLengthIndication = (i & 0x1) == 1 ? true : false;

                if (grouped) {
                    groupId = (byte) (stream.read() & 0xFF);
                    frameLength--;
                }
                if (compression) {
                    frameLength -= stream.read(b, 0, 4);
                    unflatedFrameLength = b[0] << 24 | b[1] << 16 & 0xFF0000 | b[2] << 8 & 0xFF00 | b[3] & 0xFF;
                }
                if (encrypted) {
                    encryptionId = (byte) (stream.read() & 0xFF);
                    frameLength--;
                }
                if (dataLengthIndication) {
                    frameLength -= stream.read(b, 0, 4);
                    dataLengthIndicator = b[0] << 21 | b[1] << 14 & 0xFF0000 | b[2] << 7 & 0xFF00 | b[3] & 0xFF;
                }
            }

        } else {
            stream.read(b, 0, 3);

            if (b[0] == 0) { // last frame is reaches -> terminus signal
                shortKey = null;
                stream.skip(3);
                return;
            }
            if (!verifyKeyString((int) b[0], (int) b[1], (int) b[2])) {
                if (!tag.containsValue(new String(b, 0, 2))) {
                    if (!sync(stream, b, version)) {
                        shortKey = "DUMMY";
                        return;
                    }
                }
            }
            StringBuilder buf = new StringBuilder();

            buf.append((char) (b[0] & 0xFF));
            buf.append((char) (b[1] & 0xFF));
            buf.append((char) (b[2] & 0xFF));
            shortKey = buf.toString();
            stream.read(b, 0, 3);

            frameLength = b[0] << 16 & 0xFF0000 | b[1] << 8 & 0xFF00 | b[2] & 0xFF;
        }
        if (shortKey.equals("PIC") || shortKey.equals("APIC")) {
            if (frameLength < 0 || getFrameLength() > MAXIMUM_FRAMESIZE) {
                shortKey = "DUMMY";
                return;
            }
        } else {
            if (frameLength < 0 || getFrameLength() > MAXIMUM_SHORT_FRAMESIZE) {
                shortKey = "DUMMY";
                return;
            }
        }
        if (unflatedFrameLength < 0) {
            shortKey = "DUMMY";
            return;
        }
        if (compression) {
            try {
                ZipInputStream gstream = new ZipInputStream(stream);

                encodedData = new byte[unflatedFrameLength];

                gstream.read(encodedData);
            } catch (java.util.zip.ZipException e) {
                GZIPInputStream gstream = new GZIPInputStream(stream);

                encodedData = new byte[unflatedFrameLength];
                gstream.read(encodedData);
            }
        } else {
            encodedData = new byte[frameLength];
            stream.read(encodedData);
        }
    }

    String getShortKey() {
        return shortKey;
    }

    byte[] getContent() {
        return encodedData;
    }

    void setContent(byte[] b) {
        encodedData = b;
    }

    int getFrameLength() {
        return skippedDataLength + frameLength + (version == 2 ? 6 : 10);
    }

    int getHeaderLength() {
        return (version == 2 ? 6 : 10);
    }

    boolean isUnsynchronizated() {
        return unsynchronizated;
    }

    void close() {
        encodedData = null;
    }

    private boolean verifyKeyString(int c1, int c2, int c3) throws TagException {
        if (verifyChar(c1, false) && verifyChar(c2, false) && verifyCharWithNumber(c3, false)) {
            return true;
        }
        return false;
    }

    private boolean verifyKeyString(int c1, int c2, int c3, int c4) throws TagException {
        if (verifyChar(c1, false) && verifyChar(c2, false) && verifyChar(c3, false) && verifyCharWithNumber(c4, false)) {
            return true;
        }
        return false;
    }

    private boolean verifyChar(int c, boolean checkForMP3) throws TagException {
        if (checkForMP3) {
            verifyMP3SyncPattern(c);
        }
        if (c >= 65 && c <= 90) {
            return true;
        }
        return false;
    }

    private boolean verifyCharWithNumber(int c, boolean checkForMP3) throws TagException {
        if (checkForMP3) {
            verifyMP3SyncPattern(c);
        }
        if (c >= 48 && c <= 52 || c >= 65 && c <= 90) {
            return true;
        }
        return false;
    }

    private boolean verifyMP3SyncPattern(int c) throws TagException {
        skippedDataLength++;
        if (c == 0xFF) {
            state = 0;
            state++;
        } else if (c == 0xE0 && state == 1) {
            state++;
        } else if (c == 0x00 && state == 2) {
            state++;
        } else if (c == 0x00 && state == 3) {
            state = 0;
            throw new TagException("MP3 sync pattern found");
        } else {
            state = 0;
        }

        return false;
    }

    private boolean sync(InputStream stream, byte[] b, int version) throws IOException, TagException {
        boolean stop = false;
        int modulo = 0;

        state = 0;
        int i;

        while (!stop) {
            b[0] = (byte) (stream.read() & 0xFF);
            while (!verifyChar(b[0], true)) {

                if (skippedDataLength >= SCAN_RANGE) {
                    state = 0;
                    return false;
                }
                stream.read(b, 1, 3);
                for (i = 1; i < 4; i++) {
                    verifyMP3SyncPattern(b[i]);
                }
                if (version >= 3) {
                    verifyMP3SyncPattern(b[0] = (byte) (stream.read() & 0xFF));
                } else {
                    b[0] = b[3];
                }
            }
            if (version >= 3) {
                stream.read(b, 1, 3);
                if (verifyChar((int) b[1], true) && verifyChar((int) b[2], true) && verifyCharWithNumber((int) b[3], true)) {
                    if (tag.containsValue(new String(b))) {
                        stop = true;
                    }
                }
            } else {
                stream.read(b, 1, 2);
                if (verifyChar((int) b[1], true) && verifyCharWithNumber((int) b[2], true)) {
                    if (tag.containsValue(new String(b, 0, 2))) {
                        stop = true;
                    }
                }
            }
        }
        state = 0;
        return true;
    }
}
