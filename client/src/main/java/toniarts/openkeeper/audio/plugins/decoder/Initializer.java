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

import java.io.*;

/**
 * The
 * <code>Initializer</code> class provides all necessary decoder initializing
 * related methods which are called from the constructor.
 *
 * @author Michael Scheerer
 */
abstract class Initializer extends Decoder {

    int outputChannels;
    Equalizer equalizer;
    Synthesizer filter1;
    Synthesizer filter2;
    Spline spline;

    /**
     * Constructs an instance of
     * <code>Initializer</code> with a
     * <code>MediaInformation</code> object managing all necessary objects
     * needed for the global decoder system.
     *
     * @param control the <code>MediaControl</code> object containing all
     * requests related to the audio decoding
     * @param info the <code>Frame</code> object containing the neccesary
     * informations about the source
     * @param in the input stream
     */
    Initializer(Frame info, InputStream in) {
        super(info, in);

        if (spline == null) {
            spline = new Spline();
        }

        outputChannels = info.channels;

        equalizer = new Equalizer(get(FA_EQUALIZE), spline, information);

        initializeSynthesizer();
    }

    /**
     * Sets the equalizer.
     *
     * @param f the equalizer
     */
    final void setEqualizer(Object f) {

        if (spline == null) {
            spline = new Spline();
        }

        equalizer = new Equalizer(f, spline, information);

        float af[] = equalizer.getBands();

        if (filter1 != null) {
            filter1.setEQ(af);
        }
        if (filter2 != null) {
            filter2.setEQ(af);
        }
        information.put(FA_EQUALIZE, f);
    }

    /**
     * Frees all system resources, which are bounded to this object.
     */
    @Override
    public void close() {
        super.close();
        if (filter1 != null) {
            filter1.close();
        }
        filter1 = null;
        if (filter2 != null) {
            filter2.close();
        }
        filter2 = null;
        if (equalizer != null) {
            equalizer.close();
        }
        equalizer = null;
        if (spline != null) {
            spline.close();
        }
        spline = null;
    }

    private void initializeSynthesizer() {
        float af[] = equalizer.getBands();

        filter1 = new Synthesizer(0, af);
        if (outputChannels == 2) {
            filter2 = new Synthesizer(1, af);
        }
    }
}
