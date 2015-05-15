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

import java.util.*;

/**
 * The
 * <code>Information</code> interface provides all needed methods to obtain
 * information values. Each media source is assigned to an object of this
 * interface which contains a set of information values, such as volume, gain or
 * pan, that describes the media source passing through the various codecs. The
 * kind of information values are codec specific. <br> A value may be another
 * <code>Information</code> object containing tag informations. With the
 * <code>toString</code> method it is possible to get an overview of all loaded
 * hashkeys together with their values. The hashkey is always a
 * <code>String</code> and describes the type and name of the value object. The
 * prefix is the type of the object and the postfix the object name.
 *
 * <p>
 * An example is: "String audioTrackFormat".<br>
 * "String" is the contraction of "java.lang.String".
 *
 * <p> Another example: "java.awt.Dimension dimension".<br>
 * Only "java.lang.." is abbreviated to the pure
 * <code>Class</code> or
 * <code>Interface</code> name.
 *
 * If the type is an int array the prefix is "int[]". This is the general way to
 * label arrays.
 *
 * <p>
 * An example for accessing tag related informations: <p>
 *
 * <code>
 * Information tag;<br>
 * tag = (Information) player.getInformation().get("org.ljmf.Information
 * tag");<br>
 * System.out.println(tag.get("String artist")+" - "+tag.get("String title"));
 * </code>
 *
 * <p>
 * In this example the output device would then print out: "Madonna - Dont Tell
 * Me".
 *
 * <p>
 * These tag related informations may also be of type
 * <code>Information</code>, so that
 * <code>Information</code> objects can build a tree structure.
 * <p>
 * The difference between a value object contained in a
 * <code>Control</code> object and the same named value object contained in an
 * <code>Information</code> object is like the difference between realization
 * and requirement. <br>
 * The
 * <code>Information</code> and
 * <code>Control</code> classes represent a design based on a derivation of a
 * MVC
 * <nobr>(<b>M</b>odel, <b>V</b>iew and <b>C</b>ontroller)</nobr> pattern. For
 * example a decoder could get the request to downsample an audio signal in the
 * form of a scheduled value. It may then try to process the request and
 * feedbacks the result of the processing as a real value. The scheduled value
 * may be fixed to 9500 HZ, but the decoder could only downsample to the nearest
 * possible value of 8000 HZ. Therefore the real value may then determined by
 * 8000 HZ.
 *
 * @author	Michael Scheerer
 */
public interface Information {

    /**
     * Returns the information value to which the specified key is mapped in a
     * hashtable or
     * <code>null</code> if the key is not mapped to any value in this hashtable
     *
     * @param key a key in a hashtable
     * @return a information value
     */
    public Object get(Object key);

    /**
     * Returns a
     * <code>Hashtable</code> representation of this
     * <code>Information</code> object.
     *
     * @return a <code>Hashtable</code> representation of      * this <code>Information</code> object
     */
    public Hashtable getHashtable();

    /**
     * Obtains a
     * <code>String</code> describing all information keys and values. With the
     * hashkey of a specific information it is possible to obtain the
     * information value.
     *
     * @return a String representation all informations
     */
    @Override
    public String toString();
}
