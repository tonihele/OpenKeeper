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

import java.awt.*;

/**
 * The
 * <code>Events</code> interface provides all possible events that are used to
 * induce an event based reaction model.
 *
 * @author	Michael Scheerer
 */
public interface Events {

    /**
     * Start event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. <br> In this case the event source is a
     * <code>Long</code> object containing the available playtime in
     * microseconds.
     */
    public final static int START_EVENT = AWTEvent.RESERVED_ID_MAX + 1;
    /**
     * Stop event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. <br> In this case the event source is a
     * <code>Long</code> object containing the consumed playtime in
     * microseconds.
     */
    public final static int STOP_EVENT = AWTEvent.RESERVED_ID_MAX + 2;
    /**
     * Resume event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. <br> In this case the event source is a
     * <code>Long</code> object containing the available playtime in
     * microseconds.
     */
    public final static int RESUME_EVENT = AWTEvent.RESERVED_ID_MAX + 3;
    /**
     * Suspend event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. <br> In this case the event source is a
     * <code>Long</code> object containing the consumed playtime in
     * microseconds.
     */
    public final static int SUSPEND_EVENT = AWTEvent.RESERVED_ID_MAX + 4;
    /**
     * End of media event ID. This event is used as an
     * <code>java.awt.AWTEvent</code>. In this case the event source is a
     * <code>Long</code> object containing the consumed playtime in
     * microseconds.
     */
    public final static int EOM_EVENT = AWTEvent.RESERVED_ID_MAX + 5;
    /**
     * Resize event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. In this case the event source is a
     * <code>java.awt.image.ImageProducer[]</code> object containing the sources
     * of an image stack needed to buffer the output of a video decoder plug-in.
     */
    public final static int RESIZE_EVENT = AWTEvent.RESERVED_ID_MAX + 6;
    /**
     * Repaint event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. In this case the event source is a
     * <code>Integer</code> containing the imageId of the to repainted image.
     */
    public final static int REPAINT_EVENT = AWTEvent.RESERVED_ID_MAX + 7;
    /**
     * Playtime event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. In this case the event source is a
     * <code>Long</code> containing the actual playtime in microseconds.
     */
    public final static int PLAYTIME_EVENT = AWTEvent.RESERVED_ID_MAX + 8;
    /**
     * Loading event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. In this case the event source is a
     * <code>Integer</code> containing the actual load level in percent.
     */
    public final static int LOADING_EVENT = AWTEvent.RESERVED_ID_MAX + 9;
    /**
     * Analyze event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. In this case the event source is a
     * <code>float[]</code> containing the actual spectrum, time or other kind
     * of analyze data.
     */
    public final static int ANALYZE_EVENT = AWTEvent.RESERVED_ID_MAX + 10;
    /**
     * Analyze switch mode event ID. <br> This event is used as an
     * <code>java.awt.AWTEvent</code>. In this case the event source is a
     * <code>String</code> containing a keyword to determine the analyze mode.
     */
    public final static int ANALYZE_SWITCH_MODE_EVENT = AWTEvent.RESERVED_ID_MAX + 11;
    /**
     * Skip event ID. <br> This event is used to feedback a dropped audio or
     * video frame.
     */
    public final static int SKIP_EVENT = AWTEvent.RESERVED_ID_MAX + 12;
    /**
     * Error event ID. <br> This event is used to feedback a malfunction.
     */
    public final static int ERROR_EVENT = AWTEvent.RESERVED_ID_MAX + 13;
    /**
     * Validation event ID. <br> This event is used to feedback an validation
     * case.
     */
    public final static int VALIDATION_EVENT = AWTEvent.RESERVED_ID_MAX + 14;
}
