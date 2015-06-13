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
package toniarts.openkeeper.tools.convert.textures.loadingscreens;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import toniarts.openkeeper.tools.convert.textures.Dk2TextureDecoder;

/**
 * Decoder for the .444 textures<br>
 * Texture extraction code by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class LoadingScreenTextureDecoder extends Dk2TextureDecoder {

    @Override
    protected void decompress_block(ByteBuffer out, int stride, boolean alphaFlag) {
        IntBuffer inp;
        int i;
        int bs_pos = (int) bs_index;
        long red = bs_read(bs_pos, 8);
        bs_index = prepare_decompress((int) red, bs_pos + 8);
        for (i = 0; i < 8; i++) {
            decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
        }
        for (i = 0; i < 8; i++) {
            decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64, decompress4_chunk.length - (i * 64)));
        }
        bs_pos = (int) bs_index;

        long green = bs_read(bs_pos, 8);
        bs_index = prepare_decompress((int) green, bs_pos + 8);
        for (i = 0; i < 8; i++) {
            decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
        }
        for (i = 0; i < 8; i++) {
            decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64 + 9, decompress4_chunk.length - (i * 64 + 9)));
        }
        bs_pos = (int) bs_index;

        long blue = bs_read(bs_pos, 8);
        bs_index = prepare_decompress((int) blue, bs_pos + 8);
        for (i = 0; i < 8; i++) {
            decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
        }
        for (i = 0; i < 8; i++) {
            decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9),
                    IntBuffer.wrap(decompress4_chunk, i * 64 + 18, decompress4_chunk.length - (i * 64 + 18)));
        }
        bs_pos = (int) bs_index;

        if (alphaFlag) {
            long alpha = bs_read(bs_pos, 8);
            bs_index = prepare_decompress((int) alpha, bs_pos + 8);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9),
                        IntBuffer.wrap(decompress4_chunk, i * 64 + 27, decompress4_chunk.length - (i * 64 + 27)));
            }
            bs_pos = (int) bs_index;
        }

        /* another check for a flag at 668dc7, set in the master routine */
        /* dword_7af600 = dest */

        inp = IntBuffer.wrap(decompress4_chunk);
        if (alphaFlag) {
//            dkabort(); /* 669427 */
        } else {
            for (int j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    int value;
                    /* some weird jumps that don't seem necessary */
                    int r = inp.get(inp.position() + i + 0);
                    int g = inp.get(inp.position() + i + 18);
                    int b = inp.get(inp.position() + i + 9);

                    value = clamp(r >> 16, 0, 255);
                    value |= clamp(g >> 16, 0, 255) << 16;
                    value |= clamp(b >> 16, 0, 255) << 8;
                    value |= 0xff000000;
                    if (out.position() + i * 4 <= out.limit() - 4) { // Some go overboard...
                        out.putInt(out.position() + i * 4, value);
                    }
                }
                out.position(Math.min(out.limit(), out.position() + stride));
                inp.position(inp.position() + 64);
            }
        }
    }
}
