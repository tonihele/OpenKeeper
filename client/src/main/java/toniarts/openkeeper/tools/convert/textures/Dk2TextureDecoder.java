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
package toniarts.openkeeper.tools.convert.textures;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 * Base class for some Dungeon Keeper II textures decoding.<br>
 * Texture extraction code by George Gensure
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class Dk2TextureDecoder {

    //Decompression stuff I don't understand
    /* external buffers and data to be supplied with sizes */
    private static final int[] magic_input_table_6c10c0 = {
        0x2000, 0x1712, 0x187E, 0x1B37, 0x2000, 0x28BA, 0x3B21, 0x73FC, 0x1712,
        0x10A2, 0x11A8, 0x139F, 0x1712, 0x1D5D, 0x2AA1, 0x539F, 0x187E, 0x11A8,
        0x12BF, 0x14D4, 0x187E, 0x1F2C, 0x2D41, 0x58C5, 0x1B37, 0x139F, 0x14D4,
        0x1725, 0x1B37, 0x22A3, 0x3249, 0x62A3, 0x2000, 0x1712, 0x187E, 0x1B37,
        0x2000, 0x28BA, 0x3B21, 0x73FC, 0x28BA, 0x1D5D, 0x1F2C, 0x22A3, 0x28BA,
        0x33D6, 0x4B42, 0x939F, 0x3B21, 0x2AA1, 0x2D41, 0x3249, 0x3B21, 0x4B42,
        0x6D41, 0x0D650, 0x73FC, 0x539F, 0x58C5, 0x62A3, 0x73FC, 0x939F, 0x0D650,
        0x1A463
    };
    private static final int[] dc_control_table_7af0e0 = {
        0x00000000, 0x0000003f, 0x00000037, 0x0000003e,
        0x0000003d, 0x00000036, 0x0000002f, 0x00000027,
        0x0000002e, 0x00000035, 0x0000003c, 0x0000003b,
        0x00000034, 0x0000002d, 0x00000026, 0x0000001f,
        0x00000017, 0x0000001e, 0x00000025, 0x0000002c,
        0x00000033, 0x0000003a, 0x00000039, 0x00000032,
        0x0000002b, 0x00000024, 0x0000001d, 0x00000016,
        0x0000000f, 0x00000007, 0x0000000e, 0x00000015,
        0x0000001c, 0x00000023, 0x0000002a, 0x00000031,
        0x00000038, 0x00000030, 0x00000029, 0x00000022,
        0x0000001b, 0x00000014, 0x0000000d, 0x00000006,
        0x00000005, 0x0000000c, 0x00000013, 0x0000001a,
        0x00000021, 0x00000028, 0x00000020, 0x00000019,
        0x00000012, 0x0000000b, 0x00000004, 0x00000003,
        0x0000000a, 0x00000011, 0x00000018, 0x00000010,
        /* 60 */
        0x00000009, 0x00000002, 0x00000001, 0x00000008,
        0x00040102, 0x00040301, 0x00030201, 0x00030201,
        0x00024100, 0x00024100, 0x00024100, 0x00024100,
        /* 72 */
        0x00020101, 0x00020101, 0x00020101, 0x00020101,
        0x00064200, 0x00064200, 0x00064200, 0x00064200,
        0x00070302, 0x00070302, 0x00070a01, 0x00070a01,
        0x00070104, 0x00070104, 0x00070901, 0x00070901,
        0x00060801, 0x00060801, 0x00060801, 0x00060801,
        0x00060701, 0x00060701, 0x00060701, 0x00060701,
        0x00060202, 0x00060202, 0x00060202, 0x00060202,
        0x00060601, 0x00060601, 0x00060601, 0x00060601,
        0x00080e01, 0x00080106, 0x00080d01, 0x00080c01,
        0x00080402, 0x00080203, 0x00080105, 0x00080b01,
        0x00050103, 0x00050103, 0x00050103, 0x00050103,
        0x00050103, 0x00050103, 0x00050103, 0x00050103,
        0x00050501, 0x00050501, 0x00050501, 0x00050501,
        0x00050501, 0x00050501, 0x00050501, 0x00050501,
        /* 128 */
        0x00050401, 0x00050401, 0x00050401, 0x00050401,
        0x00050401, 0x00050401, 0x00050401, 0x00050401,
        0x000a1101, 0x000a0602, 0x000a0107, 0x000a0303,
        0x000a0204, 0x000a1001, 0x000a0f01, 0x000a0502,
        /* 144 */
        0x000c010b, 0x000c0902, 0x000c0503, 0x000c010a,
        0x000c0304, 0x000c0802, 0x000c1601, 0x000c1501,
        0x000c0109, 0x000c1401, 0x000c1301, 0x000c0205,
        0x000c0403, 0x000c0108, 0x000c0702, 0x000c1201,
        /* 160 */
        0x000d0b02, 0x000d0a02, 0x000d0603, 0x000d0404,
        0x000d0305, 0x000d0207, 0x000d0206, 0x000d010f,
        0x000d010e, 0x000d010d, 0x000d010c, 0x000d1b01,
        0x000d1a01, 0x000d1901, 0x000d1801, 0x000d1701,
        /* 176 */
        0x000e011f, 0x000e011e, 0x000e011d, 0x000e011c,
        0x000e011b, 0x000e011a, 0x000e0119, 0x000e0118,
        0x000e0117, 0x000e0116, 0x000e0115, 0x000e0114,
        0x000e0113, 0x000e0112, 0x000e0111, 0x000e0110,
        /* 192 */
        0x000f0128, 0x000f0127, 0x000f0126, 0x000f0125,
        0x000f0124, 0x000f0123, 0x000f0122, 0x000f0121,
        0x000f0120, 0x000f020e, 0x000f020d, 0x000f020c,
        0x000f020b, 0x000f020a, 0x000f0209, 0x000f0208,
        0x00100212, 0x00100211, 0x00100210, 0x0010020f,
        0x00100703, 0x00101102, 0x00101002, 0x00100f02,
        0x00100e02, 0x00100d02, 0x00100c02, 0x00102001,
        0x00101f01, 0x00101e01, 0x00101d01, 0x00101c01
    }; //Unsigned, hmm, 007AF0E4
    private static final int[] magic_output_table = new int[64]; /* magic values computed from magic input */


    // Initialize the magic_output_table
    static {
        int i;
        int d, a;

        for (i = 0; i < 64; i++) {
            d = (magic_input_table_6c10c0[i] & 0xfffe0000) >> 3;
            a = (magic_input_table_6c10c0[i] & 0x0001ffff) << 3;

            magic_output_table[i] = d + a;
        }
    }
    protected long bs[];//Unsigned
    protected long bs_index;//Unsigned
    protected long bs_red = 0;//Unsigned
    protected long bs_green = 0;//Unsigned
    protected long bs_blue = 0;//Unsigned
    protected long bs_alpha = 0;//Unsigned
    protected int[] decompress2_chunk = new int[256]; /* buffers */

    protected int[] decompress3_chunk = new int[288];
    protected int[] decompress4_chunk = new int[512];
    private static final int norm_7af038 = 0x5A82799A;
    private static final float float_7af03c = 5.4119611e-1f;
    private static final float float_7af040 = 1.306563f;
    private static final float float_7af044 = 3.8268343e-1f;

    protected void initialize_dd(long[] buf) {
        bs = buf;
        bs_index = 0;
        bs_red = 0;
        bs_blue = 0;
        bs_green = 0;
        bs_alpha = 0;
    }

    protected void decompress_func1(IntBuffer in, IntBuffer out) {
        long rx;
        int sa;
        int b, a, c, d, i, p, s;
        long sc, sd, si;
        int ra, rb;
        double rxf, rxg, rxs;
        double xf, xg;

        if ((in.get(in.position() + 1) | in.get(in.position() + 2) | in.get(in.position() + 3) | in.get(in.position() + 4) | in.get(in.position() + 6) | in.get(in.position() + 7)) == 0) {
            a = in.get();
            out.put(out.position() + 0, a);
            out.put(out.position() + 9, a);
            out.put(out.position() + 18, a);
            out.put(out.position() + 27, a);
            out.put(out.position() + 36, a);
            out.put(out.position() + 45, a);
            out.put(out.position() + 54, a);
            out.put(out.position() + 63, a);
            return;
        }

        b = in.get(in.position() + 5) - in.get(in.position() + 3);
        c = in.get(in.position() + 1) - in.get(in.position() + 7);
        i = in.get(in.position() + 3) + in.get(in.position() + 5);
        a = in.get(in.position() + 7) + in.get(in.position() + 1);
        xf = b;
        xg = c;
        p = i + a;
        a -= i;

        rxs = xg + xf;
        rxf = xf * float_7af03c + float_7af044 * rxs;
        rxg = xg * float_7af040 - float_7af044 * rxs;
        ra = (int) (rxf + (rxf > 0 ? 0.5f : -0.5f));
        rb = (int) (rxg + (rxg > 0 ? 0.5f : -0.5f));

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        d = (int) (rx >> 32);

        b = in.get(in.position() + 6);
        d += d;
        a = in.get(in.position() + 2);

        c = ra;
        i = rb;
        c += d;
        d += i;
        i += p;
        sc = c & 0xFFFFFFFFL;
        sd = d & 0xFFFFFFFFL;
        si = i & 0xFFFFFFFFL;
        c = in.get(in.position() + 0);
        d = in.get(in.position() + 4);
        s = b + a;
        a -= b;
        b = d + c;
        c -= d;

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        d = (int) (rx >> 32);

        d += d;
        out.put(out.position() + 18, (int) ((c - d) + sc));
        out.put(out.position() + 45, (int) ((c - d) - sc));
        out.put(out.position() + 27, (b - (s + d)) + ra);
        out.put(out.position() + 36, (b - (s + d)) - ra);
        out.put(out.position() + 0, (int) ((s + d) + b + si));
        out.put(out.position() + 9, (int) (sd + d + c));
        out.put(out.position() + 54, (int) (d + c - sd));
        out.put(out.position() + 63, (int) ((s + d) + b - si));
    }

    protected void decompress_func2(IntBuffer in, IntBuffer out) {
        long rx;
        int sa;
        int b, a, c, d, i, p, s;
        long sc, sd, si;
        int ra, rb;
        double rxf, rxg, rxs;
        double xf, xg;

        b = in.get(in.position() + 5) - in.get(in.position() + 3);
        c = in.get(in.position() + 1) - in.get(in.position() + 7);
        i = in.get(in.position() + 3) + in.get(in.position() + 5);
        a = in.get(in.position() + 7) + in.get(in.position() + 1);
        xf = b;
        xg = c;
        p = i + a;
        a -= i;

        rxs = xg + xf;
        rxf = xf * float_7af03c + float_7af044 * rxs;
        rxg = xg * float_7af040 - float_7af044 * rxs;
        ra = (int) (rxf + (rxf > 0 ? 0.5f : -0.5f));
        rb = (int) (rxg + (rxg > 0 ? 0.5f : -0.5f));

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        d = (int) (rx >> 32);

        b = in.get(in.position() + 6);
        d += d;
        a = in.get(in.position() + 2);

        c = ra;
        i = rb;
        c += d;
        d += i;
        i += p;
        sc = c & 0xFFFFFFFFL;
        sd = d & 0xFFFFFFFFL;
        si = i & 0xFFFFFFFFL;
        c = in.get(in.position() + 0);
        d = in.get(in.position() + 4);
        s = b + a;
        a -= b;
        b = d + c;
        c -= d;

        sa = a;
        rx = sa;
        rx *= norm_7af038;
        d = (int) (rx >> 32);

        d += d;
        p = (int) sc;
        s += d;
        a = d + c;
        c -= d;
        d = s + b;
        b -= s;
        s = c + p;
        c -= p;
        p = ra;
        out.put(out.position() + 2, s);
        s = (int) sd;
        out.put(out.position() + 5, c);
        c = b + p;
        b -= p;
        p = (int) si;
        out.put(out.position() + 3, c);
        out.put(out.position() + 4, b);
        b = s + a;
        a -= s;
        c = d + p;
        d -= p;
        out.put(out.position() + 0, c);
        out.put(out.position() + 1, b);
        out.put(out.position() + 6, a);
        out.put(out.position() + 7, d);
    }

    protected long bs_read(int pos, int bits) {
        long w1, w2;
        int word_index;
        int shamt;

        word_index = pos >> 5;
        shamt = pos & 0x1f;
        w1 = (bs[word_index] << shamt) & 0xFFFFFFFFL;
        w2 = shamt != 0 && word_index + 1 < bs.length ? ((bs[word_index + 1]) >> (32 - shamt)) & 0xFFFFFFFFL : 0;
        w1 |= w2;
        w1 >>= (32 - bits);

        return w1;
    }

    protected long prepare_decompress(int value, int pos) {
        int xindex = 0, index = 0, control_word = 0;
        short magic_index = 0x3f;
        boolean areWeDone = false;

        decompress2_chunk[0] = value * magic_output_table[0];
        Arrays.fill(decompress2_chunk, 1, decompress2_chunk.length, 0);

        while (true) {
            if (!areWeDone) {
                xindex = index = (int) bs_read(pos, 17);
            }
            if (index >= 0x8000 || areWeDone) {
                int out_index;
                if (!areWeDone) {
                    index >>= 13;
                    control_word = dc_control_table_7af0e0[60 + index];
                }
                are_we_done:
                {
                    areWeDone = false;
                    if ((control_word & 0xff00) == 0x4100) {
                        return pos + (control_word >> 16);
                    }
                    if ((control_word & 0xff00) > 0x4100) {
                        int unk14;
                        /* read next control */
                        pos += control_word >> 16;
                        unk14 = (int) bs_read(pos, 14);
                        pos += 14;
                        magic_index -= (unk14 & 0xff00) >> 8;
                        unk14 &= 0xff;
                        if (unk14 != 0) {
                            if (unk14 != 0x80) {
                                if (unk14 > 0x80) {
                                    unk14 -= 0x100;
                                }
                                magic_index--;
                            } else {
                                unk14 = (int) bs_read(pos, 8);
                                pos += 8;
                                unk14 -= 0x100;
                            }
                        } else {
                            unk14 = (int) bs_read(pos, 8);
                            pos += 8;
                        }
                        control_word = unk14;
                    } else {
                        int bit_to_test;
                        int rem = control_word >> 16;
                        int xoramt = 0;

                        magic_index -= (control_word & 0xff00) >> 8;
                        bit_to_test = 16 - rem;
                        if ((xindex & (1 << bit_to_test)) != 0) {
                            xoramt = ~0;
                        }
                        control_word &= 0xff;
                        control_word ^= xoramt;
                        pos++;
                        control_word -= xoramt;
                        pos += rem;
                    }
                    out_index = dc_control_table_7af0e0[magic_index + 1];
                    decompress2_chunk[out_index] = ((short) control_word) * magic_output_table[out_index];
                    continue;
                }
            } else if (index >= 0x800) {
                index >>= 9;
                control_word = dc_control_table_7af0e0[72 + index];
                areWeDone = true;
                continue;
            } else if (index >= 0x400) {
                index >>= 7;
                control_word = dc_control_table_7af0e0[128 + index];
                areWeDone = true;
                continue;
            } else if (index >= 0x200) {
                index >>= 5;
                control_word = dc_control_table_7af0e0[128 + index];
                areWeDone = true;
                continue;
            } else if (index >= 0x100) {
                index >>= 4;
                control_word = dc_control_table_7af0e0[144 + index];
                areWeDone = true;
                continue;
            } else if (index >= 0x80) {
                index >>= 3;
                control_word = dc_control_table_7af0e0[160 + index];
                areWeDone = true;
                continue;
            } else if (index >= 0x40) {
                index >>= 2;
                control_word = dc_control_table_7af0e0[176 + index];
                areWeDone = true;
                continue;
            } else if (index >= 0x20) {
                index >>= 1;
                control_word = dc_control_table_7af0e0[192 + index];
                areWeDone = true;
                continue;
            }
        }
    }

    protected int clamp(int n, int min, int max) {
        if (n < min) {
            return min;
        }
        if (n > max) {
            return max;
        }
        return n;
    }

    protected abstract void decompress_block(ByteBuffer out, int stride, boolean alphaFlag);

    public byte[] dd_texture(long[] buf, int stride, int width, int height, boolean alphaFlag) {
        int x, y;
        ByteBuffer out = ByteBuffer.allocate(width * height * 4);
        out.order(ByteOrder.LITTLE_ENDIAN);

        initialize_dd(buf);

        for (y = 0; y < height; y += 8) {
            for (x = 0; x < width; x += 8) {
                out.position(y * stride + x * 4);
                decompress_block(out, stride, alphaFlag);
            }
        }
        return out.array();
    }
}
