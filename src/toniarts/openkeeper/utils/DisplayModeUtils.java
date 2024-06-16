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

import java.util.List;

/**
 * Java's own graphics settings are more often wrong than right. Also they block
 * MacOS from working (AWT & LWJGL 3 issue). This class tries to probe either
 * LWJGL 2 or 3 depending which is available without creating a dependency to
 * either. Finally falling back to AWT as a last resort.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class DisplayModeUtils implements DisplayModeProvider {

    /**
     * The delegate for the actual provider
     */
    private final DisplayModeProvider displayModeProvider;

    private static class SingletonHelper {

        private static final DisplayModeUtils INSTANCE = new DisplayModeUtils();
    }

    public static DisplayModeUtils getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private DisplayModeUtils() {
        displayModeProvider = getDisplayModeProvider();
    }

    private DisplayModeProvider getDisplayModeProvider() {

        // LWJGL 3
        try {
            Class.forName("com.jme3.system.lwjgl.LwjglWindow");
            return new Lwjgl3DisplayModeProvider();
        } catch (ClassNotFoundException exception) {
        }

        // LWJGL 2
        try {
            Class.forName("org.lwjgl.opengl.Display");
            return new Lwjgl2DisplayModeProvider();
        } catch (ClassNotFoundException exception) {
        }

        // Fallback
        return new AwtDisplayModeProvider();
    }

    @Override
    public List<DisplayMode> getDisplayModes() {
        return displayModeProvider.getDisplayModes();
    }

    @Override
    public boolean isFullScreenSupported() {
        return displayModeProvider.isFullScreenSupported();
    }

}
