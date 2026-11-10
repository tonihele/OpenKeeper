/*
 * Copyright (C) 2014-2024 OpenKeeper
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
package toniarts.openkeeper.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extracts display modes using LWJGL 3
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
final class Lwjgl3DisplayModeProvider extends DefaultDisplayModeProvider {

    private final Method get;
    private final Method getBlueBits;
    private final Method getFrequency;
    private final Method getGreenBits;
    private final Method getModeHeight;
    private final Method getModeWidth;
    private final Method getModes;
    private final Method getPrimaryMonitor;
    private final Method getRedBits;
    private final Method hasRemaining;

    public Lwjgl3DisplayModeProvider() {
        try {
            Class<?> glfwClass = Class.forName("org.lwjgl.glfw.GLFW");
            getModes = glfwClass.getDeclaredMethod("glfwGetVideoModes", long.class);
            getPrimaryMonitor = glfwClass.getDeclaredMethod("glfwGetPrimaryMonitor");

            Class<?> vidModeClass = Class.forName("org.lwjgl.glfw.GLFWVidMode");
            getBlueBits = vidModeClass.getDeclaredMethod("blueBits");
            getFrequency = vidModeClass.getDeclaredMethod("refreshRate");
            getGreenBits = vidModeClass.getDeclaredMethod("greenBits");
            getModeHeight = vidModeClass.getDeclaredMethod("height");
            getModeWidth = vidModeClass.getDeclaredMethod("width");
            getRedBits = vidModeClass.getDeclaredMethod("redBits");

            Class<?>[] vmInnerClasses = vidModeClass.getDeclaredClasses();
            assert vmInnerClasses.length == 1 : vmInnerClasses.length;
            Class<?> vmBufferClass = vmInnerClasses[0];
            get = vmBufferClass.getMethod("get");
            hasRemaining = vmBufferClass.getMethod("hasRemaining");
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException("Failed to instantiate LWJGL 3 display mode provider. Has API been changed?", ex);
        }
    }

    @Override
    public List<DisplayMode> getDisplayModes() {
        try {
            Object monitorId = getPrimaryMonitor.invoke(null);

            if (monitorId == null || 0L == (Long) monitorId) {
                return Collections.emptyList();
            }

            Object buf = getModes.invoke(null, monitorId);

            List<DisplayMode> displayModes = new ArrayList<>();
            while ((Boolean) hasRemaining.invoke(buf)) {
                Object vidMode = get.invoke(buf);

                DisplayMode mode = getDisplayMode(vidMode);
                addDisplayMode(displayModes, mode);
            }

            return displayModes;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Failed to get display modes from LWJGL 3", ex);
        }
    }

    private DisplayMode getDisplayMode(Object glfwVidMode)
            throws IllegalAccessException, InvocationTargetException {
        int width = (Integer) getModeWidth.invoke(glfwVidMode);
        int height = (Integer) getModeHeight.invoke(glfwVidMode);
        int redBits = (Integer) getRedBits.invoke(glfwVidMode);
        int greenBits = (Integer) getGreenBits.invoke(glfwVidMode);
        int blueBits = (Integer) getBlueBits.invoke(glfwVidMode);
        int rate = (Integer) getFrequency.invoke(glfwVidMode);
        int bitDepth = redBits + greenBits + blueBits;

        DisplayMode dm = new DisplayMode(width, height);
        dm.addRefreshRate(rate);
        dm.addBitDepth(bitDepth);
        
        return dm;
    }

    @Override
    public boolean isFullScreenSupported() {
        return true;
    }

}
