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
package toniarts.openkeeper.video.tgq;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import toniarts.openkeeper.tools.convert.Utils;

/**
 * Holds a TGQ frame (one texture that is)
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class TgqFrame implements Comparable<TgqFrame> {

    private final int width;
    private final int height;
    private final int frameIndex;

    public TgqFrame(byte[] data, int frameIndex) {
        this.frameIndex = frameIndex;

        // Read width & height from the header
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        width = buf.getShort();
        height = buf.getShort();

        // Decode
        decodeFrame(buf);
    }

    private void decodeFrame(ByteBuffer buf) {
        short quantizer = Utils.toUnsignedByte(buf.get());
        buf.position(buf.position() + 3); // Skip 3 bytes
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "Video frame: " + frameIndex + ", " + width + "x" + height;
    }

    @Override
    public int compareTo(TgqFrame o) {
        return Integer.compare(frameIndex, o.frameIndex);
    }
}
