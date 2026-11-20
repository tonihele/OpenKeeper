/*
 * Copyright (C) 2026 OpenKeeper
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
package toniarts.openkeeper.tools.convert.wad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WadDecompressorTest {

    @Test
    void expandsLongOverlappingBackReferences() throws Exception {
        ByteArrayOutputStream commands = new ByteArrayOutputStream();
        commands.write(0xE0);
        commands.writeBytes("abcd".getBytes(StandardCharsets.US_ASCII));
        commands.write(0xBF); // 67-byte match
        commands.write(0);    // high offset
        commands.write(3);    // distance 4
        commands.write(0xFF); // final 3 literals
        commands.writeBytes("xyz".getBytes(StandardCharsets.US_ASCII));

        byte[] expected = new byte[74];
        byte[] seed = "abcd".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(seed, 0, expected, 0, seed.length);
        for (int i = seed.length; i < 71; i++) {
            expected[i] = expected[i - seed.length];
        }
        System.arraycopy("xyz".getBytes(StandardCharsets.US_ASCII), 0, expected, 71, 3);

        assertArrayEquals(expected, WadDecompressor.decompress(compressed(74, commands.toByteArray()), "overlap"));
    }

    @Test
    void copiesLongNonOverlappingBackReferences() throws Exception {
        ByteArrayOutputStream commands = new ByteArrayOutputStream();
        commands.write(0xE4); // 20 literals
        byte[] seed = new byte[20];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = (byte) (i + 1);
        }
        commands.writeBytes(seed);
        commands.write(0x90); // 20-byte match
        commands.write(0);    // high offset
        commands.write(19);   // distance 20
        commands.write(0xFF);
        commands.writeBytes("xyz".getBytes(StandardCharsets.US_ASCII));

        byte[] expected = new byte[43];
        System.arraycopy(seed, 0, expected, 0, seed.length);
        System.arraycopy(seed, 0, expected, seed.length, seed.length);
        System.arraycopy("xyz".getBytes(StandardCharsets.US_ASCII), 0, expected, 40, 3);

        assertArrayEquals(expected, WadDecompressor.decompress(compressed(43, commands.toByteArray()), "non-overlap"));
    }

    @Test
    void rejectsBackReferencesBeforeAnyOutputExists() {
        assertThrows(IOException.class,
                () -> WadDecompressor.decompress(compressed(3, new byte[]{0, 0}), "invalid-reference"));
    }

    private byte[] compressed(int decodedSize, byte[] commands) {
        byte[] payload = new byte[5 + commands.length];
        payload[0] = 0;
        payload[1] = 0;
        payload[2] = (byte) (decodedSize >>> 16);
        payload[3] = (byte) (decodedSize >>> 8);
        payload[4] = (byte) decodedSize;
        System.arraycopy(commands, 0, payload, 5, commands.length);
        return payload;
    }
}