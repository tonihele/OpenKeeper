/* Copyright (C) 2003-2014 Michael Scheerer. All Rights Reserved. */

/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package toniarts.openkeeper.audio.plugins.decoder;

/**
 * The
 * <code>Synthesizer</code> class contains the polyphase synthesis filterbank.
 * 32 frequency-samples are transformed via IDCT and inverse filtering to one
 * output sample.
 * <p>
 * Note that IDCT and DCT algorithms are completely equal with exception of a
 * division through a norming factor. The IDCT is programmed as a DCT algorithm.
 * A description of such a DCT algorythm can be found in:
 *
 * <ul>
 * <li>A New Algorithm to Compute the Discrete Cosine Transform, Byeong Gi Lee,
 * IEEE Trans. Acoustics, Speech, and Signal Processing, Vol. ASSP-32, Number 6,
 * p. 1243-1245, 1984.</li>
 * <li>Computernumeric 2, Christian Ueberhausen, p. 55, 1984.</li>
 * </ul>
 *
 * The IDCT is performed as a decimation of time with bit reversed ordered input
 * values.
 *
 * @author Michael Scheerer
 */
final class Synthesizer {

    final static int SBLIMIT = 32;
    // ISO/IEC 11172-3 Annex B Table B.3
    private final static float WINDOW[][] = {
        {
            0.0F, -4.42505E-4F, 0.003250122F, -0.007003784F, 0.031082153F, -0.07862854F, 0.10031128F, -0.57203674F, 1.144989F, 0.57203674F, 0.10031128F, 0.07862854F, 0.031082153F, 0.007003784F, 0.003250122F, 4.42505E-4F
        }, {
            -1.5259E-5F, -4.73022E-4F, 0.003326416F, -0.007919312F, 0.030517578F, -0.08418274F, 0.090927124F, -0.6002197F, 1.1442871F, 0.54382324F, 0.1088562F, 0.07305908F, 0.03147888F, 0.006118774F, 0.003173828F, 3.96729E-4F
        }, {
            -1.5259E-5F, -5.34058E-4F, 0.003387451F, -0.008865356F, 0.029785156F, -0.08970642F, 0.08068848F, -0.6282959F, 1.1422119F, 0.51560974F, 0.11657715F, 0.06752014F, 0.03173828F, 0.0052948F, 0.003082275F, 3.66211E-4F
        }, {
            -1.5259E-5F, -5.79834E-4F, 0.003433228F, -0.009841919F, 0.028884888F, -0.09516907F, 0.06959534F, -0.6562195F, 1.1387634F, 0.48747253F, 0.12347412F, 0.06199646F, 0.031845093F, 0.004486084F, 0.002990723F, 3.20435E-4F
        }, {
            -1.5259E-5F, -6.2561E-4F, 0.003463745F, -0.010848999F, 0.027801514F, -0.10054016F, 0.057617188F, -0.6839142F, 1.1339264F, 0.45947266F, 0.12957764F, 0.056533813F, 0.031814575F, 0.003723145F, 0.00289917F, 2.89917E-4F
        }, {
            -1.5259E-5F, -6.86646E-4F, 0.003479004F, -0.011886597F, 0.026535034F, -0.1058197F, 0.044784546F, -0.71131897F, 1.1277466F, 0.43165588F, 0.1348877F, 0.051132202F, 0.031661987F, 0.003005981F, 0.002792358F, 2.59399E-4F
        }, {
            -1.5259E-5F, -7.47681E-4F, 0.003479004F, -0.012939453F, 0.02508545F, -0.110946655F, 0.031082153F, -0.7383728F, 1.120224F, 0.40408325F, 0.13945007F, 0.045837402F, 0.03138733F, 0.002334595F, 0.002685547F, 2.44141E-4F
        }, {
            -3.0518E-5F, -8.08716E-4F, 0.003463745F, -0.014022827F, 0.023422241F, -0.11592102F, 0.01651001F, -0.7650299F, 1.1113739F, 0.37680054F, 0.14326477F, 0.040634155F, 0.03100586F, 0.001693726F, 0.002578735F, 2.13623E-4F
        }, {
            -3.0518E-5F, -8.8501E-4F, 0.003417969F, -0.01512146F, 0.021575928F, -0.12069702F, 0.001068115F, -0.791214F, 1.1012115F, 0.34986877F, 0.1463623F, 0.03555298F, 0.030532837F, 0.001098633F, 0.002456665F, 1.98364E-4F
        }, {
            -3.0518E-5F, -9.61304E-4F, 0.003372192F, -0.016235352F, 0.01953125F, -0.1252594F, -0.015228271F, -0.816864F, 1.0897827F, 0.32331848F, 0.1487732F, 0.03060913F, 0.029937744F, 5.49316E-4F, 0.002349854F, 1.67847E-4F
        }, {
            -3.0518E-5F, -0.001037598F, 0.00328064F, -0.017349243F, 0.01725769F, -0.12956238F, -0.03237915F, -0.84194946F, 1.0771179F, 0.2972107F, 0.15049744F, 0.025817871F, 0.029281616F, 3.0518E-5F, 0.002243042F, 1.52588E-4F
        }, {
            -4.5776E-5F, -0.001113892F, 0.003173828F, -0.018463135F, 0.014801025F, -0.1335907F, -0.050354004F, -0.8663635F, 1.0632172F, 0.2715912F, 0.15159607F, 0.0211792F, 0.028533936F, -4.42505E-4F, 0.002120972F, 1.37329E-4F
        }, {
            -4.5776E-5F, -0.001205444F, 0.003051758F, -0.019577026F, 0.012115479F, -0.13729858F, -0.06916809F, -0.89009094F, 1.0481567F, 0.24650574F, 0.15206909F, 0.016708374F, 0.02772522F, -8.69751E-4F, 0.00201416F, 1.2207E-4F
        }, {
            -6.1035E-5F, -0.001296997F, 0.002883911F, -0.020690918F, 0.009231567F, -0.14067078F, -0.088775635F, -0.9130554F, 1.0319366F, 0.22198486F, 0.15196228F, 0.012420654F, 0.02684021F, -0.001266479F, 0.001907349F, 1.06812E-4F
        }, {
            -6.1035E-5F, -0.00138855F, 0.002700806F, -0.02178955F, 0.006134033F, -0.14367676F, -0.10916138F, -0.9351959F, 1.0146179F, 0.19805908F, 0.15130615F, 0.00831604F, 0.025909424F, -0.001617432F, 0.001785278F, 1.06812E-4F
        }, {
            -7.6294E-5F, -0.001480103F, 0.002487183F, -0.022857666F, 0.002822876F, -0.1462555F, -0.13031006F, -0.95648193F, 0.99624634F, 0.17478943F, 0.15011597F, 0.004394531F, 0.024932861F, -0.001937866F, 0.001693726F, 9.1553E-5F
        }, {
            -7.6294E-5F, -0.001586914F, 0.002227783F, -0.023910522F, -6.86646E-4F, -0.14842224F, -0.15220642F, -0.9768524F, 0.9768524F, 0.15220642F, 0.14842224F, 6.86646E-4F, 0.023910522F, -0.002227783F, 0.001586914F, 7.6294E-5F
        }, {
            -9.1553E-5F, -0.001693726F, 0.001937866F, -0.024932861F, -0.004394531F, -0.15011597F, -0.17478943F, -0.99624634F, 0.95648193F, 0.13031006F, 0.1462555F, -0.002822876F, 0.022857666F, -0.002487183F, 0.001480103F, 7.6294E-5F
        }, {
            -1.06812E-4F, -0.001785278F, 0.001617432F, -0.025909424F, -0.00831604F, -0.15130615F, -0.19805908F, -1.0146179F, 0.9351959F, 0.10916138F, 0.14367676F, -0.006134033F, 0.02178955F, -0.002700806F, 0.00138855F, 6.1035E-5F
        }, {
            -1.06812E-4F, -0.001907349F, 0.001266479F, -0.02684021F, -0.012420654F, -0.15196228F, -0.22198486F, -1.0319366F, 0.9130554F, 0.088775635F, 0.14067078F, -0.009231567F, 0.020690918F, -0.002883911F, 0.001296997F, 6.1035E-5F
        }, {
            -1.2207E-4F, -0.00201416F, 8.69751E-4F, -0.02772522F, -0.016708374F, -0.15206909F, -0.24650574F, -1.0481567F, 0.89009094F, 0.06916809F, 0.13729858F, -0.012115479F, 0.019577026F, -0.003051758F, 0.001205444F, 4.5776E-5F
        }, {
            -1.37329E-4F, -0.002120972F, 4.42505E-4F, -0.028533936F, -0.0211792F, -0.15159607F, -0.2715912F, -1.0632172F, 0.8663635F, 0.050354004F, 0.1335907F, -0.014801025F, 0.018463135F, -0.003173828F, 0.001113892F, 4.5776E-5F
        }, {
            -1.52588E-4F, -0.002243042F, -3.0518E-5F, -0.029281616F, -0.025817871F, -0.15049744F, -0.2972107F, -1.0771179F, 0.84194946F, 0.03237915F, 0.12956238F, -0.01725769F, 0.017349243F, -0.00328064F, 0.001037598F, 3.0518E-5F
        }, {
            -1.67847E-4F, -0.002349854F, -5.49316E-4F, -0.029937744F, -0.03060913F, -0.1487732F, -0.32331848F, -1.0897827F, 0.816864F, 0.015228271F, 0.1252594F, -0.01953125F, 0.016235352F, -0.003372192F, 9.61304E-4F, 3.0518E-5F
        }, {
            -1.98364E-4F, -0.002456665F, -0.001098633F, -0.030532837F, -0.03555298F, -0.1463623F, -0.34986877F, -1.1012115F, 0.791214F, -0.001068115F, 0.12069702F, -0.021575928F, 0.01512146F, -0.003417969F, 8.8501E-4F, 3.0518E-5F
        }, {
            -2.13623E-4F, -0.002578735F, -0.001693726F, -0.03100586F, -0.040634155F, -0.14326477F, -0.37680054F, -1.1113739F, 0.7650299F, -0.01651001F, 0.11592102F, -0.023422241F, 0.014022827F, -0.003463745F, 8.08716E-4F, 3.0518E-5F
        }, {
            -2.44141E-4F, -0.002685547F, -0.002334595F, -0.03138733F, -0.045837402F, -0.13945007F, -0.40408325F, -1.120224F, 0.7383728F, -0.031082153F, 0.110946655F, -0.02508545F, 0.012939453F, -0.003479004F, 7.47681E-4F, 1.5259E-5F
        }, {
            -2.59399E-4F, -0.002792358F, -0.003005981F, -0.031661987F, -0.051132202F, -0.1348877F, -0.43165588F, -1.1277466F, 0.71131897F, -0.044784546F, 0.1058197F, -0.026535034F, 0.011886597F, -0.003479004F, 6.86646E-4F, 1.5259E-5F
        }, {
            -2.89917E-4F, -0.00289917F, -0.003723145F, -0.031814575F, -0.056533813F, -0.12957764F, -0.45947266F, -1.1339264F, 0.6839142F, -0.057617188F, 0.10054016F, -0.027801514F, 0.010848999F, -0.003463745F, 6.2561E-4F, 1.5259E-5F
        }, {
            -3.20435E-4F, -0.002990723F, -0.004486084F, -0.031845093F, -0.06199646F, -0.12347412F, -0.48747253F, -1.1387634F, 0.6562195F, -0.06959534F, 0.09516907F, -0.028884888F, 0.009841919F, -0.003433228F, 5.79834E-4F, 1.5259E-5F
        }, {
            -3.66211E-4F, -0.003082275F, -0.0052948F, -0.03173828F, -0.06752014F, -0.11657715F, -0.51560974F, -1.1422119F, 0.6282959F, -0.08068848F, 0.08970642F, -0.029785156F, 0.008865356F, -0.003387451F, 5.34058E-4F, 1.5259E-5F
        }, {
            -3.96729E-4F, -0.003173828F, -0.006118774F, -0.03147888F, -0.07305908F, -0.1088562F, -0.54382324F, -1.1442871F, 0.6002197F, -0.090927124F, 0.08418274F, -0.030517578F, 0.007919312F, -0.003326416F, 4.73022E-4F, 1.5259E-5F
        }
    };
    private final static double NULL_MATRIX[] = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };
    double[] samples;
    float[] eq;
    private double sum, sum1, sum2, sum3, sum4, sum5, sum6, sum7, sum8, sum9, sum10, sum11, sum12, sum13, sum14, sum15, sum16, sum17, sum18, sum19, sum20, sum21, sum22, sum23, sum24, sum25, sum26, sum27, sum28, sum29, sum30, sum31, sum32;
    private double sum33, sum34, sum35, sum36, sum37, sum38, sum39, sum40, sum41, sum42, sum43, sum44, sum45, sum46, sum47, sum48, sum49, sum50, sum51, sum52, sum53, sum54, sum55, sum56, sum57, sum58, sum59, sum60, sum61, sum62, sum63;
    private double f;
    private double[][] u;
    private double[] v;
    private int writePos, writePos1, writePos2, writePos3, writePos4, writePos5, writePos6, writePos7, writePos8, writePos9, writePos10, writePos11, writePos12, writePos13, writePos14, writePos15;
    private final int channelNumber;
    private float winTemp[];
    private int i, j;

    Synthesizer(int i, float af[]) {
        u = new double[2][512];
        samples = new double[SBLIMIT];
        channelNumber = i;
        setEQ(af);
    }

    void close() {
        samples = null;
        u[0] = null;
        u[1] = null;
        u = null;
        v = null;
    }

    void setEQ(float af[]) {
        eq = af;
    }

    private void idct(double vec1[], double vec2[]) {
        for (i = 31; i > 15; i--) {
            if (samples[i] != 0 && i > 15) {
                idct32_32();
                idct32(vec1, vec2);
                return;
            }
        }
        for (; i > 7; i--) {
            if (samples[i] != 0 && i > 7) {
                idct32_16();
                idct32(vec1, vec2);
                return;
            }
        }
        idct32_8();
        idct32(vec1, vec2);
    }

    private void idct32_32() {
        sum = samples[0] + samples[31];
        sum31 = (samples[0] - samples[31]) * 0.50060299823519627; // (1 / (2 * Cos(1*PI/64))
        sum1 = samples[1] + samples[30];
        sum30 = (samples[1] - samples[30]) * 0.50547095989754365; // (1 / (2 * Cos(3*PI/64))
        sum2 = samples[2] + samples[29];
        sum29 = (samples[2] - samples[29]) * 0.51544730992262455; // (1 / (2 * Cos(5*PI/64))
        sum3 = samples[3] + samples[28];
        sum28 = (samples[3] - samples[28]) * 0.53104259108978413; // (1 / (2 * Cos(7*PI/64))
        sum4 = samples[4] + samples[27];
        sum27 = (samples[4] - samples[27]) * 0.55310389603444454; // (1 / (2 * Cos(9*PI/64))
        sum5 = samples[5] + samples[26];
        sum26 = (samples[5] - samples[26]) * 0.58293496820613389; // (1 / (2 * Cos(11*PI/64))
        sum6 = samples[6] + samples[25];
        sum25 = (samples[6] - samples[25]) * 0.62250412303566482; // (1 / (2 * Cos(13*PI/64))
        sum7 = samples[7] + samples[24];
        sum24 = (samples[7] - samples[24]) * 0.67480834145500568; // (1 / (2 * Cos(15*PI/64))
        sum8 = samples[8] + samples[23];
        sum23 = (samples[8] - samples[23]) * 0.74453627100229836; // (1 / (2 * Cos(17*PI/64))
        sum9 = samples[9] + samples[22];
        sum22 = (samples[9] - samples[22]) * 0.83934964541552681; // (1 / (2 * Cos(19*PI/64))
        sum10 = samples[10] + samples[21];
        sum21 = (samples[10] - samples[21]) * 0.97256823786196078; // (1 / (2 * Cos(21*PI/64))
        sum11 = samples[11] + samples[20];
        sum20 = (samples[11] - samples[20]) * 1.1694399334328847; // (1 / (2 * Cos(23*PI/64))
        sum12 = samples[12] + samples[19];
        sum19 = (samples[12] - samples[19]) * 1.4841646163141662; // (1 / (2 * Cos(25*PI/64))
        sum13 = samples[13] + samples[18];
        sum18 = (samples[13] - samples[18]) * 2.0577810099534108; // (1 / (2 * Cos(27*PI/64))
        sum14 = samples[14] + samples[17];
        sum17 = (samples[14] - samples[17]) * 3.407608418468719; // (1 / (2 * Cos(29*PI/64))
        sum15 = samples[15] + samples[16];
        sum16 = (samples[15] - samples[16]) * 10.190008123548033; // (1 / (2 * Cos(31*PI/64))

        sum32 = sum + sum15;
        sum47 = (sum - sum15) * 0.50241928618815568; // (1 / (2 * Cos(1*PI/32))
        sum33 = sum1 + sum14;
        sum46 = (sum1 - sum14) * 0.52249861493968885; // (1 / (2 * Cos(3*PI/32))
        sum34 = sum2 + sum13;
        sum45 = (sum2 - sum13) * 0.56694403481635769; // (1 / (2 * Cos(5*PI/32))
        sum35 = sum3 + sum12;
        sum44 = (sum3 - sum12) * 0.64682178335999008; // (1 / (2 * Cos(7*PI/32))
        sum36 = sum4 + sum11;
        sum43 = (sum4 - sum11) * 0.7881546234512502; // (1 / (2 * Cos(9*PI/32))
        sum37 = sum5 + sum10;
        sum42 = (sum5 - sum10) * 1.0606776859903471; // (1 / (2 * Cos(11*PI/32))
        sum38 = sum6 + sum9;
        sum41 = (sum6 - sum9) * 1.7224470982383342; // (1 / (2 * Cos(13*PI/32))
        sum39 = sum7 + sum8;
        sum40 = (sum7 - sum8) * 5.1011486186891553; // (1 / (2 * Cos(15*PI/32))

        sum48 = sum16 + sum31;
        sum63 = (sum31 - sum16) * 0.50241928618815568; // (1 / (2 * Cos(1*PI/32))
        sum49 = sum17 + sum30;
        sum62 = (sum30 - sum17) * 0.52249861493968885; // (1 / (2 * Cos(3*PI/32))
        sum50 = sum18 + sum29;
        sum61 = (sum29 - sum18) * 0.56694403481635769; // (1 / (2 * Cos(5*PI/32))
        sum51 = sum19 + sum28;
        sum60 = (sum28 - sum19) * 0.64682178335999008; // (1 / (2 * Cos(7*PI/32))
        sum52 = sum20 + sum27;
        sum59 = (sum27 - sum20) * 0.7881546234512502; // (1 / (2 * Cos(9*PI/32))
        sum53 = sum21 + sum26;
        sum58 = (sum26 - sum21) * 1.0606776859903471; // (1 / (2 * Cos(11*PI/32))
        sum54 = sum22 + sum25;
        sum57 = (sum25 - sum22) * 1.7224470982383342; // (1 / (2 * Cos(13*PI/32))
        sum55 = sum23 + sum24;
        sum56 = (sum24 - sum23) * 5.1011486186891553; // (1 / (2 * Cos(15*PI/32))
    }

    private void idct32_16() {
        sum = samples[0];
        sum31 = sum * 0.50060299823519627; // (1 / (2 * Cos(1*PI/64))
        sum1 = samples[1];
        sum30 = sum1 * 0.50547095989754365; // (1 / (2 * Cos(3*PI/64))
        sum2 = samples[2];
        sum29 = sum2 * 0.51544730992262455; // (1 / (2 * Cos(5*PI/64))
        sum3 = samples[3];
        sum28 = sum3 * 0.53104259108978413; // (1 / (2 * Cos(7*PI/64))
        sum4 = samples[4];
        sum27 = sum4 * 0.55310389603444454; // (1 / (2 * Cos(9*PI/64))
        sum5 = samples[5];
        sum26 = sum5 * 0.58293496820613389; // (1 / (2 * Cos(11*PI/64))
        sum6 = samples[6];
        sum25 = sum6 * 0.62250412303566482; // (1 / (2 * Cos(13*PI/64))
        sum7 = samples[7];
        sum24 = sum7 * 0.67480834145500568; // (1 / (2 * Cos(15*PI/64))
        sum8 = samples[8];
        sum23 = sum8 * 0.74453627100229836; // (1 / (2 * Cos(17*PI/64))
        sum9 = samples[9];
        sum22 = sum9 * 0.83934964541552681; // (1 / (2 * Cos(19*PI/64))
        sum10 = samples[10];
        sum21 = sum10 * 0.97256823786196078; // (1 / (2 * Cos(21*PI/64))
        sum11 = samples[11];
        sum20 = sum11 * 1.1694399334328847; // (1 / (2 * Cos(23*PI/64))
        sum12 = samples[12];
        sum19 = sum12 * 1.4841646163141662; // (1 / (2 * Cos(25*PI/64))
        sum13 = samples[13];
        sum18 = sum13 * 2.0577810099534108; // (1 / (2 * Cos(27*PI/64))
        sum14 = samples[14];
        sum17 = sum14 * 3.407608418468719; // (1 / (2 * Cos(29*PI/64))
        sum15 = samples[15];
        sum16 = sum15 * 10.190008123548033; // (1 / (2 * Cos(31*PI/64))

        sum32 = sum + sum15;
        sum47 = (sum - sum15) * 0.50241928618815568; // (1 / (2 * Cos(1*PI/32))
        sum33 = sum1 + sum14;
        sum46 = (sum1 - sum14) * 0.52249861493968885; // (1 / (2 * Cos(3*PI/32))
        sum34 = sum2 + sum13;
        sum45 = (sum2 - sum13) * 0.56694403481635769; // (1 / (2 * Cos(5*PI/32))
        sum35 = sum3 + sum12;
        sum44 = (sum3 - sum12) * 0.64682178335999008; // (1 / (2 * Cos(7*PI/32))
        sum36 = sum4 + sum11;
        sum43 = (sum4 - sum11) * 0.7881546234512502; // (1 / (2 * Cos(9*PI/32))
        sum37 = sum5 + sum10;
        sum42 = (sum5 - sum10) * 1.0606776859903471; // (1 / (2 * Cos(11*PI/32))
        sum38 = sum6 + sum9;
        sum41 = (sum6 - sum9) * 1.7224470982383342; // (1 / (2 * Cos(13*PI/32))
        sum39 = sum7 + sum8;
        sum40 = (sum7 - sum8) * 5.1011486186891553; // (1 / (2 * Cos(15*PI/32))

        sum48 = sum16 + sum31;
        sum63 = (sum31 - sum16) * 0.50241928618815568; // (1 / (2 * Cos(1*PI/32))
        sum49 = sum17 + sum30;
        sum62 = (sum30 - sum17) * 0.52249861493968885; // (1 / (2 * Cos(3*PI/32))
        sum50 = sum18 + sum29;
        sum61 = (sum29 - sum18) * 0.56694403481635769; // (1 / (2 * Cos(5*PI/32))
        sum51 = sum19 + sum28;
        sum60 = (sum28 - sum19) * 0.64682178335999008; // (1 / (2 * Cos(7*PI/32))
        sum52 = sum20 + sum27;
        sum59 = (sum27 - sum20) * 0.7881546234512502; // (1 / (2 * Cos(9*PI/32))
        sum53 = sum21 + sum26;
        sum58 = (sum26 - sum21) * 1.0606776859903471; // (1 / (2 * Cos(11*PI/32))
        sum54 = sum22 + sum25;
        sum57 = (sum25 - sum22) * 1.7224470982383342; // (1 / (2 * Cos(13*PI/32))
        sum55 = sum23 + sum24;
        sum56 = (sum24 - sum23) * 5.1011486186891553; // (1 / (2 * Cos(15*PI/32))
    }

    private void idct32_8() {
        sum32 = samples[0];
        sum48 = sum32 * 0.50060299823519627; // (1 / (2 * Cos(1*PI/64))
        sum33 = samples[1];
        sum49 = sum33 * 0.50547095989754365; // (1 / (2 * Cos(3*PI/64))
        sum34 = samples[2];
        sum50 = sum34 * 0.51544730992262455; // (1 / (2 * Cos(5*PI/64))
        sum35 = samples[3];
        sum51 = sum35 * 0.53104259108978413; // (1 / (2 * Cos(7*PI/64))
        sum36 = samples[4];
        sum52 = sum36 * 0.55310389603444454; // (1 / (2 * Cos(9*PI/64))
        sum37 = samples[5];
        sum53 = sum37 * 0.58293496820613389; // (1 / (2 * Cos(11*PI/64))
        sum38 = samples[6];
        sum54 = sum38 * 0.62250412303566482; // (1 / (2 * Cos(13*PI/64))
        sum39 = samples[7];
        sum55 = sum39 * 0.67480834145500568; // (1 / (2 * Cos(15*PI/64))

        sum47 = sum32 * 0.50241928618815568; // (1 / (2 * Cos(1*PI/32))
        sum46 = sum33 * 0.52249861493968885; // (1 / (2 * Cos(3*PI/32))
        sum45 = sum34 * 0.56694403481635769; // (1 / (2 * Cos(5*PI/32))
        sum44 = sum35 * 0.64682178335999008; // (1 / (2 * Cos(7*PI/32))
        sum43 = sum36 * 0.7881546234512502; // (1 / (2 * Cos(9*PI/32))
        sum42 = sum37 * 1.0606776859903471; // (1 / (2 * Cos(11*PI/32))
        sum41 = sum38 * 1.7224470982383342; // (1 / (2 * Cos(13*PI/32))
        sum40 = sum39 * 5.1011486186891553; // (1 / (2 * Cos(15*PI/32))

        sum63 = sum48 * 0.50241928618815568; // (1 / (2 * Cos(1*PI/32))
        sum62 = sum49 * 0.52249861493968885; // (1 / (2 * Cos(3*PI/32))
        sum61 = sum50 * 0.56694403481635769; // (1 / (2 * Cos(5*PI/32))
        sum60 = sum51 * 0.64682178335999008; // (1 / (2 * Cos(7*PI/32))
        sum59 = sum52 * 0.7881546234512502; // (1 / (2 * Cos(9*PI/32))
        sum58 = sum53 * 1.0606776859903471; // (1 / (2 * Cos(11*PI/32))
        sum57 = sum54 * 1.7224470982383342; // (1 / (2 * Cos(13*PI/32))
        sum56 = sum55 * 5.1011486186891553; // (1 / (2 * Cos(15*PI/32))
    }

    private void idct32(double vec1[], double vec2[]) {
        sum = sum32 + sum39;
        sum7 = (sum32 - sum39) * 0.50979557910415918; // (1 / (2 * Cos(1*PI/16))
        sum1 = sum33 + sum38;
        sum6 = (sum33 - sum38) * 0.60134488693504529; // (1 / (2 * Cos(3*PI/16))
        sum2 = sum34 + sum37;
        sum5 = (sum34 - sum37) * 0.89997622313641557; // (1 / (2 * Cos(5*PI/16))
        sum3 = sum35 + sum36;
        sum4 = (sum35 - sum36) * 2.5629154477415055; // (1 / (2 * Cos(7*PI/16))

        sum8 = sum40 + sum47;
        sum15 = (sum47 - sum40) * 0.50979557910415918; // (1 / (2 * Cos(1*PI/16))
        sum9 = sum41 + sum46;
        sum14 = (sum46 - sum41) * 0.60134488693504529; // (1 / (2 * Cos(3*PI/16))
        sum10 = sum42 + sum45;
        sum13 = (sum45 - sum42) * 0.89997622313641557; // (1 / (2 * Cos(5*PI/16))
        sum11 = sum43 + sum44;
        sum12 = (sum44 - sum43) * 2.5629154477415055; // (1 / (2 * Cos(7*PI/16))

        sum16 = sum48 + sum55;
        sum23 = (sum48 - sum55) * 0.50979557910415918; // (1 / (2 * Cos(1*PI/16))
        sum17 = sum49 + sum54;
        sum22 = (sum49 - sum54) * 0.60134488693504529; // (1 / (2 * Cos(3*PI/16))
        sum18 = sum50 + sum53;
        sum21 = (sum50 - sum53) * 0.89997622313641557; // (1 / (2 * Cos(5*PI/16))
        sum19 = sum51 + sum52;
        sum20 = (sum51 - sum52) * 2.5629154477415055; // (1 / (2 * Cos(7*PI/16))

        sum24 = sum56 + sum63;
        sum31 = (sum63 - sum56) * 0.50979557910415918; // (1 / (2 * Cos(1*PI/16))
        sum25 = sum57 + sum62;
        sum30 = (sum62 - sum57) * 0.60134488693504529; // (1 / (2 * Cos(3*PI/16))
        sum26 = sum58 + sum61;
        sum29 = (sum61 - sum58) * 0.89997622313641557; // (1 / (2 * Cos(5*PI/16))
        sum27 = sum59 + sum60;
        sum28 = (sum60 - sum59) * 2.5629154477415055; // (1 / (2 * Cos(7*PI/16))

        sum32 = sum + sum3;
        sum35 = (sum - sum3) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum33 = sum1 + sum2;
        sum34 = (sum1 - sum2) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum36 = sum4 + sum7;
        sum39 = (sum7 - sum4) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum37 = sum5 + sum6;
        sum38 = (sum6 - sum5) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum40 = sum8 + sum11;
        sum43 = (sum8 - sum11) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum41 = sum9 + sum10;
        sum42 = (sum9 - sum10) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum44 = sum12 + sum15;
        sum47 = (sum15 - sum12) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum45 = sum13 + sum14;
        sum46 = (sum14 - sum13) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum48 = sum16 + sum19;
        sum51 = (sum16 - sum19) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum49 = sum17 + sum18;
        sum50 = (sum17 - sum18) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum52 = sum20 + sum23;
        sum55 = (sum23 - sum20) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum53 = sum21 + sum22;
        sum54 = (sum22 - sum21) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum56 = sum24 + sum27;
        sum59 = (sum24 - sum27) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum57 = sum25 + sum26;
        sum58 = (sum25 - sum26) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum60 = sum28 + sum31;
        sum63 = (sum31 - sum28) * 0.54119610014619701; // (1 / (2 * Cos(1*PI/8))
        sum61 = sum29 + sum30;
        sum62 = (sum30 - sum29) * 1.3065629648763764; // (1 / (2 * Cos(3*PI/8))

        sum = sum32 + sum33;
        sum1 = (sum32 - sum33) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum2 = sum34 + sum35;
        sum3 = (sum35 - sum34) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum4 = sum36 + sum37;
        sum5 = (sum36 - sum37) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum6 = sum38 + sum39;
        sum7 = (sum39 - sum38) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum8 = sum40 + sum41;
        sum9 = (sum40 - sum41) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum10 = sum42 + sum43;
        sum11 = (sum43 - sum42) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum12 = sum44 + sum45;
        sum13 = (sum44 - sum45) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum14 = sum46 + sum47;
        sum15 = (sum47 - sum46) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum16 = sum48 + sum49;
        sum17 = (sum48 - sum49) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum18 = sum50 + sum51;
        sum19 = (sum51 - sum50) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum20 = sum52 + sum53;
        sum21 = (sum52 - sum53) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum22 = sum54 + sum55;
        sum23 = (sum55 - sum54) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum24 = sum56 + sum57;
        sum25 = (sum56 - sum57) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum26 = sum58 + sum59;
        sum27 = (sum59 - sum58) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum28 = sum60 + sum61;
        sum29 = (sum60 - sum61) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum30 = sum62 + sum63;
        sum31 = (sum63 - sum62) * 0.70710678118654746; // (1 / (2 * Cos(1*PI/4))

        sum2 += sum3;
        sum6 += sum7;
        sum4 += sum6;
        sum6 += sum5;
        sum5 += sum7;
        sum10 += sum11;
        sum14 += sum15;
        sum12 += sum14;
        sum14 += sum13;
        sum13 += sum15;
        sum18 += sum19;
        sum22 += sum23;
        sum20 += sum22;
        sum22 += sum21;
        sum21 += sum23;
        sum26 += sum27;
        sum30 += sum31;
        sum28 += sum30;
        sum30 += sum29;
        sum29 += sum31;
        sum8 += sum12;
        sum12 += sum10;
        sum10 += sum14;
        sum14 += sum9;
        sum9 += sum13;
        sum13 += sum11;
        sum11 += sum15;
        sum24 += sum28;
        sum28 += sum26;
        sum26 += sum30;
        sum30 += sum25;
        sum25 += sum29;
        sum29 += sum27;
        sum27 += sum31;
        // vector loading
        vec1[writePos] = -sum1;
        vec1[writePos | 31] = vec1[writePos | 1] = -sum30 - sum17;
        vec1[writePos | 30] = vec1[writePos | 2] = -sum14;
        vec1[writePos | 29] = vec1[writePos | 3] = -sum22 - sum30;
        vec1[writePos | 28] = vec1[writePos | 4] = -sum6;
        vec1[writePos | 27] = vec1[writePos | 5] = -sum26 - sum22;
        vec1[writePos | 26] = vec1[writePos | 6] = -sum10;
        vec1[writePos | 25] = vec1[writePos | 7] = -sum18 - sum26;
        vec1[writePos | 24] = vec1[writePos | 8] = -sum2;
        vec1[writePos | 23] = vec1[writePos | 9] = -sum28 - sum18;
        vec1[writePos | 22] = vec1[writePos | 10] = -sum12;
        vec1[writePos | 21] = vec1[writePos | 11] = -sum20 - sum28;
        vec1[writePos | 20] = vec1[writePos | 12] = -sum4;
        vec1[writePos | 19] = vec1[writePos | 13] = -sum24 - sum20;
        vec1[writePos | 18] = vec1[writePos | 14] = -sum8;
        vec1[writePos | 17] = vec1[writePos | 15] = -sum16 - sum24;
        vec1[writePos | 16] = -sum;

        vec2[writePos] = sum1;
        vec2[writePos | 31] = -(vec2[writePos | 1] = sum17 + sum25);
        vec2[writePos | 30] = -(vec2[writePos | 2] = sum9);
        vec2[writePos | 29] = -(vec2[writePos | 3] = sum25 + sum21);
        vec2[writePos | 28] = -(vec2[writePos | 4] = sum5);
        vec2[writePos | 27] = -(vec2[writePos | 5] = sum21 + sum29);
        vec2[writePos | 26] = -(vec2[writePos | 6] = sum13);
        vec2[writePos | 25] = -(vec2[writePos | 7] = sum29 + sum19);
        vec2[writePos | 24] = -(vec2[writePos | 8] = sum3);
        vec2[writePos | 23] = -(vec2[writePos | 9] = sum19 + sum27);
        vec2[writePos | 22] = -(vec2[writePos | 10] = sum11);
        vec2[writePos | 21] = -(vec2[writePos | 11] = sum27 + sum23);
        vec2[writePos | 20] = -(vec2[writePos | 12] = sum7);
        vec2[writePos | 19] = -(vec2[writePos | 13] = sum23 + sum31);
        vec2[writePos | 18] = -(vec2[writePos | 14] = sum15);
        vec2[writePos | 17] = -(vec2[writePos | 15] = sum31);
        vec2[writePos | 16] = 0;
    }

    void synthesize(Output output) {
        idct(u[j], u[j + 1 & 0x1]);

        j = j + 1 & 0x1;
        v = u[j];

        writePos1 = writePos - 32 & 0x1FF;
        writePos2 = writePos - 64 & 0x1FF;
        writePos3 = writePos - 96 & 0x1FF;
        writePos4 = writePos - 128 & 0x1FF;
        writePos5 = writePos - 160 & 0x1FF;
        writePos6 = writePos - 192 & 0x1FF;
        writePos7 = writePos - 224 & 0x1FF;
        writePos8 = writePos - 256 & 0x1FF;
        writePos9 = writePos - 288 & 0x1FF;
        writePos10 = writePos - 320 & 0x1FF;
        writePos11 = writePos - 352 & 0x1FF;
        writePos12 = writePos - 384 & 0x1FF;
        writePos13 = writePos - 416 & 0x1FF;
        writePos14 = writePos - 448 & 0x1FF;
        writePos15 = writePos - 480 & 0x1FF;

        for (i = 0; i < SBLIMIT; i++) {

            winTemp = WINDOW[i];

            f = v[i | writePos] * winTemp[0] + v[i | writePos1] * winTemp[1] + v[i | writePos2] * winTemp[2] + v[i | writePos3] * winTemp[3] + v[i | writePos4] * winTemp[4] + v[i | writePos5] * winTemp[5] + v[i | writePos6] * winTemp[6] + v[i | writePos7] * winTemp[7] + v[i | writePos8] * winTemp[8] + v[i | writePos9] * winTemp[9] + v[i | writePos10] * winTemp[10] + v[i | writePos11] * winTemp[11] + v[i | writePos12] * winTemp[12] + v[i | writePos13] * winTemp[13] + v[i | writePos14] * winTemp[14] + v[i | writePos15] * winTemp[15];

            output.setBuffer(f, samples[i], i, channelNumber);
        }

        writePos = writePos + 32 & 0x1FF;

        System.arraycopy(NULL_MATRIX, 0, samples, 0, SBLIMIT);
    }

    void reset() {
        u = new double[2][512];
        samples = new double[SBLIMIT];
        j = 0;
        writePos = 0;
    }
}
