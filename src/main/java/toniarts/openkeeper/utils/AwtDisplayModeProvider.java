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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses Java's own way of getting available display modes. Often errorenious and
 * locks up on MacOS with LWJGL 3
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
final class AwtDisplayModeProvider extends DefaultDisplayModeProvider {

    public AwtDisplayModeProvider() {
    }

    @Override
    public List<DisplayMode> getDisplayModes() {
        GraphicsDevice device = getGraphicsDevice();

        java.awt.DisplayMode[] modes = device.getDisplayModes();

        List<DisplayMode> displayModes = new ArrayList<>(modes.length);

        // Loop them through
        for (java.awt.DisplayMode dm : modes) {
            DisplayMode mdm = getDisplayMode(dm);
            addDisplayMode(displayModes, mdm);
        }

        return displayModes;
    }

    private GraphicsDevice getGraphicsDevice() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    }

    private DisplayMode getDisplayMode(java.awt.DisplayMode dm) {
        DisplayMode displayMode = new DisplayMode(dm.getWidth(), dm.getHeight());
        if (dm.getRefreshRate() != java.awt.DisplayMode.REFRESH_RATE_UNKNOWN) {
            displayMode.addRefreshRate(dm.getRefreshRate());
        }
        if (dm.getBitDepth() != java.awt.DisplayMode.BIT_DEPTH_MULTI) {
            displayMode.addBitDepth(dm.getBitDepth());
        }

        return displayMode;
    }

    @Override
    public boolean isFullScreenSupported() {
        GraphicsDevice device = getGraphicsDevice();

        return device.isFullScreenSupported();
    }

}
