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
package toniarts.openkeeper.audio.plugins.decoder.tag;

/**
 * This
 * <code>Exception</code> should be thrown if tag informations can't be
 * extracted
 *
 * @author Michael Scheerer
 */
public final class TagException extends Exception {

    /**
     * Constructs an instance of
     * <code>TagException</code>.
     */
    public TagException() {
    }

    /**
     * Constructs an instance of
     * <code>TagException</code>.
     *
     * @param s the exception message
     */
    public TagException(java.lang.String s) {
        super(s);
    }
}
