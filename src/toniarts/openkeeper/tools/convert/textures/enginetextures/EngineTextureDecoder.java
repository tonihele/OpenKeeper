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
package toniarts.openkeeper.tools.convert.textures.enginetextures;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import toniarts.openkeeper.tools.convert.textures.Dk2TextureDecoder;

/**
 * Decoder for the engine textures<br>
 * Texture extraction code by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class EngineTextureDecoder extends Dk2TextureDecoder {

    private static final short[] jump_table_7af4e0 = {0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2,
        0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x2, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12,
        0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x12, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22, 0x22,
        0x22, 0x22, 0x22, 0x22, 0x22, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
        0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
        0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33, 0x33,
        0x33, 0x33, 0x33, 0x33, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44,
        0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x44, 0x55, 0x55,
        0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x66, 0x66, 0x66, 0x66, 0x77,
        0x77, 0x88, 0x0};
    private float float_7af000 = 1.048576e6f;
    private float float_7af004 = 8.388608e6f;
    private float float_7af008 = 1.169f;
    private float float_7af00c = -8.1300002e-1f;
    private float float_7af010 = -3.91e-1f;
    private float float_7af014 = 1.602f;
    private float float_7af018 = 2.0250001f;
    private double double_7af048 = 6.75539944108852e15;

    @Override
    protected void decompress_block(ByteBuffer out, int stride, boolean alphaFlag) {
        IntBuffer inp;
        double d;
        long xr, xg, xb;
        int ir, ig, ib;

        int a;
        float r, g, b;
        int i, j;

        decompress(alphaFlag);

        inp = IntBuffer.wrap(decompress4_chunk);
        for (j = 0; j < 8; j++) {
            for (i = 0; i < 8; i++) {
                int value;
                r = inp.get(inp.position() + i + 0);
                g = inp.get(inp.position() + i + 18);
                b = inp.get(inp.position() + i + 9);
                a = inp.get(inp.position() + i + 27);
                d = float_7af014 * (g - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
                xr = (long) (d + (d > 0 ? 0.5f : -0.5f)) & 0xFFFFFFFFL;
                ir = (int) xr;
                d = float_7af018 * (b - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
                xg = (long) (d + (d > 0 ? 0.5f : -0.5f)) & 0xFFFFFFFFL;
                ig = (int) xg;
                d = float_7af010 * (b - float_7af004) + float_7af00c * (g - float_7af004) + float_7af008 * (r - float_7af000) + double_7af048;
                xb = (long) (d + (d > 0 ? 0.5f : -0.5f)) & 0xFFFFFFFFL;
                ib = (int) xb;

                value = clamp(ir >> 16, 0, 255);
                value |= clamp(ig >> 16, 0, 255) << 16;
                value |= clamp(ib >> 16, 0, 255) << 8;
                if (alphaFlag) {
                    value |= clamp(a >> 16, 0, 255) << 24;
                } else {
                    value |= 0xff000000;
                }
                out.putInt(out.position() + i * 4, value);
            }
            out.position(Math.min(out.limit(), out.position() + stride));
            inp.position(inp.position() + 64);
        }
    }

    private void decompress(boolean alphaFlag) {
        int jt_index, jt_value;
        int bs_pos = (int) bs_index;
        int value;
        int blanket_fill;

        /* red */
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_red += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i] = (int) bs_red << 16;
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_red, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64, decompress4_chunk.length - i * 64));
            }
        }

        bs_pos = (int) bs_index;

        /* green */
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_green += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i + 9] = (int) (bs_green << 16);
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_green, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64 + 9, decompress4_chunk.length - (i * 64 + 9)));
            }
        }

        bs_pos = (int) bs_index;

        /* blue */
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_blue += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i + 18] = (int) (bs_blue << 16);
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_blue, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64 + 18, decompress4_chunk.length - (i * 64 + 18)));
            }
        }

        bs_pos = (int) bs_index;

        /* alpha */
        if (!alphaFlag) {
            return;
        }
        value = 0;
        jt_index = (int) bs_read(bs_pos, 8);

        jt_value = jump_table_7af4e0[jt_index];
        bs_pos += jt_value & 0xf;
        jt_value >>= 4;
        if (jt_value != 0) {
            /* value is signed */
            value = (int) bs_read(bs_pos, jt_value);
            if ((value & (1 << (jt_value - 1))) == 0) {
                value -= (1 << jt_value) - 1;
            }

            bs_pos += jt_value;
        }

        bs_alpha += value;
        blanket_fill = (int) bs_read(bs_pos, 2);
        if (blanket_fill == 2) {
            int i, j;
            bs_pos += 2;
            for (j = 0; j < 8; j++) {
                for (i = 0; i < 8; i++) {
                    decompress4_chunk[j * 64 + i + 27] = (int) (bs_alpha << 16);
                }
            }
            bs_index = bs_pos;
        } else {
            int i;
            bs_index = prepare_decompress((int) bs_alpha, bs_pos);
            for (i = 0; i < 8; i++) {
                decompress_func1(IntBuffer.wrap(decompress2_chunk, i * 8, decompress2_chunk.length - i * 8), IntBuffer.wrap(decompress3_chunk, i, decompress3_chunk.length - i));
            }
            for (i = 0; i < 8; i++) {
                decompress_func2(IntBuffer.wrap(decompress3_chunk, i * 9, decompress3_chunk.length - i * 9), IntBuffer.wrap(decompress4_chunk, i * 64 + 27, decompress4_chunk.length - (i * 64 + 27)));
            }
        }
    }
}
