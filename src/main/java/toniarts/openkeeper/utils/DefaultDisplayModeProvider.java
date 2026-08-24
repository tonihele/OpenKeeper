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

import java.util.Collections;
import java.util.List;

/**
 * Simple groups some common methods for display mode providers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
abstract class DefaultDisplayModeProvider implements DisplayModeProvider {

    protected void addDisplayMode(List<DisplayMode> displayModes, DisplayMode mdm) {
        int index = Collections.binarySearch(displayModes, mdm);
        if (index > -1) {
            DisplayMode existingDm = displayModes.get(index);
            for (Integer refreshRate : mdm.getRefreshRates()) {
                existingDm.addRefreshRate(refreshRate);
            }
            for (Integer bitDepth : mdm.getBitDepths()) {
                existingDm.addBitDepth(bitDepth);
            }
        } else {
            displayModes.add(~index, mdm);
        }
    }

}
