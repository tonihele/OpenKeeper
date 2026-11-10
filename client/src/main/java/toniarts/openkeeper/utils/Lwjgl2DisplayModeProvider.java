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
import java.util.List;

/**
 * Extracts display modes using LWJGL 2
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
final class Lwjgl2DisplayModeProvider extends DefaultDisplayModeProvider {

    private final Method getBitsPerPixel;
    private final Method getFrequency;
    private final Method getModeHeight;
    private final Method getModeWidth;
    private final Method getModes;

    public Lwjgl2DisplayModeProvider() {
        try {
            Class<?> displayClass = Class.forName("org.lwjgl.opengl.Display");
            getModes = displayClass.getDeclaredMethod("getAvailableDisplayModes");
            Class<?> displayModeClass = Class.forName("org.lwjgl.opengl.DisplayMode");
            getBitsPerPixel = displayModeClass.getDeclaredMethod("getBitsPerPixel");
            getFrequency = displayModeClass.getDeclaredMethod("getFrequency");
            getModeHeight = displayModeClass.getDeclaredMethod("getHeight");
            getModeWidth = displayModeClass.getDeclaredMethod("getWidth");
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException("Failed to instantiate LWJGL 2 display mode provider. Has API been changed?", ex);
        }
    }

    @Override
    public List<DisplayMode> getDisplayModes() {
        try {
            Object[] glModes = (Object[]) getModes.invoke(null);

            List<DisplayMode> displayModes = new ArrayList<>();
            for (Object glMode : glModes) {
                DisplayMode mode = getDisplayMode(glMode);
                addDisplayMode(displayModes, mode);
            }

            return displayModes;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException("Failed to get display modes from LWJGL 2", ex);
        }
    }

    private DisplayMode getDisplayMode(Object glMode)
            throws IllegalAccessException, InvocationTargetException {
        int width = (Integer) getModeWidth.invoke(glMode);
        int height = (Integer) getModeHeight.invoke(glMode);
        int bitDepth = (Integer) getBitsPerPixel.invoke(glMode);
        int rate = (Integer) getFrequency.invoke(glMode);

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
