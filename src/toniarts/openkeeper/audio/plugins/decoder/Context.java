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

import java.util.*;

/**
 * A class, which consists entirely of static fields and methods, encapsulates a
 * variety of information about the runtime environment. This includes:
 * <ul>
 * <li>Java vendor</li>
 * <li>Java version</li>
 * <li>Browser</li>
 * <li>Browser version</li>
 * <li>Operating system vendor</li>
 * <li>Operating system version</li>
 * <li>Kind of audio device</li>
 * </ul>
 * Furthermore this class sets all global library system parameters, if the
 * predefined values are not useful. This includes:
 * <ul>
 * <li>URL code base, default value =
 * <code>null</code></li>
 * <li>Maximum count of media sources streaming concurrent via an internet
 * protocol, default value = 1</li>
 * <li>Global input stream buffer, default value = 8192 bytes</li>
 * <li>Depth of the media format verification in bytes, default value = 500
 * bytes</li>
 * <li>Applet flag, default value = false</li>
 * </ul>
 * If this library runs for an applet, the URL address of a code base must be
 * specified, if jar-files are used.
 *
 * <p>
 * In every case the codec plug-ins must be loaded with the
 * <code>activatePlugIn</code> method. The argument is the name of the plug-in,
 * which must be obtained from the plug-in vendor/developer.
 *
 * <p>
 * The original Java Sound plug-ins must be plugged according to the approach of
 * the Java Sound API, but with one exception: It is recommended to store the
 * META-INF directory with all content outside a jar-file, if such a file
 * exists.
 *
 * @author Michael Scheerer
 */
public final class Context {

    private static String version = System.getProperty("java.version");
    private static String vendor = System.getProperty("java.vendor");
    private static String osName = System.getProperty("os.name");
    private static String osVersion = System.getProperty("os.version");
    private static String classVersion = System.getProperty("java.class.version");
    private static boolean applet = true;
    private static boolean isJava1;
    private static boolean isJsharp;
    private static boolean javasoundApi = true;
    private static int streamCountLimit = 1;
    private static int inputBufferSize = 51200;
    private static int minimumInputBufferSize = 1024;
    private static int scanDepth = 600;
    private static boolean logging;
    private static Vector plugIns = new Vector();

    static {

        if (version == null) {
            isJsharp = true;
        } else {
            int index = -1;

            if (!applet) {
                index = System.getProperty("java.class.path").toUpperCase().indexOf(".NET");
            }

            if (index > -1) {
                isJsharp = true;
                javasoundApi = false;
            } else if (version.startsWith("1.0") || version.startsWith("1.1")) {
                isJava1 = true;
                javasoundApi = false;
            }
            if (version.startsWith("1.2")) {
                javasoundApi = false;
            }
        }
    }

    /**
     * Sets an LJMF media file format plug-in. The names of the wrapper classes
     * of such a plug-in are equal to the argument string of this method. These
     * string is plug-in specific. Some examples are: "Mpeg", "Ogg".
     *
     * @param pluginName the string to determinate a specific LJMF media file
     * format plug-in
     */
    public static void activatePlugIn(String pluginName) {
        if (!plugIns.contains(pluginName)) {
            plugIns.addElement(pluginName);
        }
    }

    /**
     * Gets the
     * <code>Vector</code> containing all active LJMF media format plug-in names
     * as a
     * <code>String</code>.
     *
     * @return the number of existing LJMF media format plug-ins
     */
    public static Vector getActivePlugIns() {
        return plugIns;
    }

    /**
     * Sets the maximum possible number of parallel streamed media sources. The
     * default value is 1.
     *
     * @param s the maximum possible number of parallel streamed media sources
     */
    public static void setStreamCountLimit(int s) {
        if (s < streamCountLimit) {
            s = streamCountLimit;
        }
        streamCountLimit = s;
    }

    /**
     * Returns the maximum possible number of parallel streamed media sources.
     *
     * @return the maximum possible number of parallel streamed media sources
     */
    public static int getStreamCountLimit() {
        return streamCountLimit;
    }

    /**
     * Sets the size of the input stream buffer. If this buffer is too small an
     * "Invalid Reset To Mark" error or an "Unsupported File Format" exception
     * message can occur. The default value is 51200 bytes.
     *
     * @param s the size of the input stream buffer
     */
    public static void setInputBufferSize(int s) {
        if (s < minimumInputBufferSize) {
            s = minimumInputBufferSize;
        }
        inputBufferSize = s;
    }

    /**
     * Gets the size of the input stream buffer in bytes.
     *
     * @return the size of the input stream buffer
     */
    public static int getInputBufferSize() {
        return inputBufferSize;
    }

    /**
     * Sets the depth of the media format verification in bytes. If this value
     * is too small an "Unsupported File Format" exception message can occur. In
     * the special case of playing files with disengaged media tagging the value
     * must high enough to cover the full tag. The predefined default value is
     * related to playback with engaged tagging. The default value is 600 bytes.
     *
     * @param s the depth of the media format verification
     */
    public static void setVerificationDepth(int s) {
        scanDepth = s;
    }

    /**
     * Gets the depth of the media format verification in bytes.
     *
     * @return the depth of the media format verification
     */
    public static int getVerificationDepth() {
        return scanDepth;
    }

    /**
     * Returns if the current JDK or SDK is J#
     *
     * @return   <code>true</code> if the current JDK or SDK is J#
     */
    public static boolean isJSharp() {
        return isJsharp;
    }

    /**
     * Returns if the current JDK or SDK is Java1
     *
     * @return   <code>true</code> if the current JDK or SDK is Java1
     */
    public static boolean isJava1() {
        return isJava1;
    }

    /**
     * Returns if the current JDK or SDK supports JavaSound API
     *
     * @return   <code>true</code> if the current JDK or SDK support JavaSound API
     */
    public static boolean isJavaSound() {
        return javasoundApi;
    }

    /**
     * Returns the vendor.
     *
     * @return the vendor
     */
    public static String getVendor() {
        return vendor;
    }

    /**
     * Returns the version.
     *
     * @return the version
     */
    public static String getVersion() {
        return version;
    }

    /**
     * Returns the operating system version.
     *
     * @return the operating system version
     */
    public static String getOsVersion() {
        return osVersion;
    }

    /**
     * Returns the class version.
     *
     * @return the class version
     */
    public static String getClassVersion() {
        return classVersion;
    }

    /**
     * Returns the operation system name.
     *
     * @return the operation system
     */
    public static String getOs() {
        return osName;
    }

    /**
     * Returns the framework is running as an applet.
     *
     * @return   <code>true</code> the framework is running as an applet
     */
    public static boolean isApplet() {
        return applet;
    }

    /**
     * Sets the applet flag. The default value is true.
     *
     * @param b  <code>true</code> if the framework is running as an applet
     */
    public static void setAppletFlag(boolean b) {
        applet = b;
    }

    /**
     * Returns if the framework is running with logging.
     *
     * @return   <code>true</code> if the framework is running with logging
     */
    public static boolean getLogging() {
        return logging;
    }

    /**
     * Sets the logging flag. The default value is false.
     *
     * @param b  <code>true</code> if the framework is running with logging
     */
    public static void setLogging(boolean b) {
        logging = b;
    }
}
