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
package toniarts.openkeeper.audio.plugins.decoder;

/**
 * This
 * <code>Exception</code> should be thrown if a media format can't be handled
 *
 * @author Michael Scheerer
 */
public final class UnsupportedMediaException extends Exception {

    /**
     * Constructs an instance of
     * <code>UnsupportedMediaException</code> with a default message.
     */
    public UnsupportedMediaException() {
    }

    /**
     * Constructs an instance of
     * <code>UnsupportedMediaException</code> with a default message.
     *
     * @param s the exception message
     */
    public UnsupportedMediaException(String s) {
        super(s);
    }
}
