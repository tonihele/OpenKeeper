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

import java.lang.reflect.*;

/**
 * The
 * <code>Equalizer</code> class stores and controls settings of equalizer
 * channels. MPEG1/2/2.5 audio has a maximum of 32 channels
 *
 * @author	Michael Scheerer
 */
final class Equalizer {

    final static int SBLIMIT = 32;
    final static int MAX_SAMPLE_RATE = 48000;
    private float[] settings;

    Equalizer(Object eqArray, Spline spline, Frame information) {
        int length = 0;

        int sampleRate = ((Integer) information.get(AudioInformation.I_SAMPLE_RATE)).intValue();

        int numberOfSubbands = information.numberOfSubbands;

        int i = 0;

        int j = 0;

        int k = 0;

        try {
            length = Array.getLength(eqArray);

            settings = new float[SBLIMIT];

            float[] buffer = new float[SBLIMIT];

            if (length > 0) {
                float inc;

                for (; i < SBLIMIT; i++) {
                    inc = i / (float) (SBLIMIT - 1) * sampleRate / (float) MAX_SAMPLE_RATE;
                    buffer[i] = (float) spline.getValue((float[]) eqArray, inc);
                }

                i = 0;

                k = 0;

                if (numberOfSubbands == 8) {
                    for (; i < 32; i += 4) {
                        settings[j++] = (buffer[i] + buffer[i + 1] + buffer[i + 2] + buffer[i + 3]) / 4;
                    }
                } else if (numberOfSubbands == 12) {
                    boolean toggle = false;

                    for (; i < 32; i += k) {
                        if (toggle) {
                            settings[j++] = (buffer[i] + buffer[i + 1] + buffer[i + 2]) / 3;
                            k = 3;
                        } else {
                            settings[j++] = (buffer[i] + buffer[i + 1]) / 2;
                            k = 2;
                        }
                        toggle = !toggle;
                    }
                } else if (numberOfSubbands == 27) {
                    for (; i < 8; i++) {
                        settings[j++] = buffer[i];
                    }
                    settings[j++] = (buffer[i++] + buffer[i++] + buffer[i++]) / 3;
                    for (; i < 20; i++) {
                        settings[j++] = buffer[i];
                    }
                    settings[j++] = (buffer[i++] + buffer[i++] + buffer[i++]) / 3;
                    for (; i < 32; i++) {
                        settings[j++] = buffer[i];
                    }
                } else if (numberOfSubbands == 30) {
                    for (; i < 10; i++) {
                        settings[j++] = buffer[i];
                    }
                    settings[j++] = (buffer[i++] + buffer[i++]) / 2;
                    for (; i < 21; i++) {
                        settings[j++] = buffer[i];
                    }
                    settings[j++] = (buffer[i++] + buffer[i++]) / 2;
                    for (; i < 32; i++) {
                        settings[j++] = buffer[i];
                    }
                } else {
                    settings = buffer;
                    j = i = 32;
                }
                for (; j < SBLIMIT; j++) {
                    settings[j] = 1;
                }
            } else {
                settings = new float[SBLIMIT];
                for (; i < settings.length; i++) {
                    settings[i] = 1;
                }
            }
        } catch (Exception e) {
            settings = new float[SBLIMIT];
            for (i = 0; i < settings.length; i++) {
                settings[i] = 1;
            }
        }
    }

    float[] getBands() {
        return settings;
    }

    void close() {
        settings = null;
    }
}
