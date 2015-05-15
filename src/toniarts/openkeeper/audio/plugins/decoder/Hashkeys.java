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
 * <code>Hashkeys</code> interface contains predefined keys, which are used by
 * the
 * <code>MediaInformation</code> class and the
 * <code>MediaControl</code> class.
 *
 * @author	Michael Scheerer
 */
public interface Hashkeys {

    /**
     * The predefined bit protection value key. The standard value is true. If
     * true the flag tries to force a bitProtection (CRC, ADLER, ..).
     */
    public final static String B_BIT_PROTECTION = "Boolean bitProtection";
    /**
     * The predefined fast seek value key. The standard value is false. If true
     * the flag tries to force a fast seeking algorithm - for example without
     * vbr support.
     */
    public final static String B_FAST_SEEKING = "Boolean fastSeeking";
}
