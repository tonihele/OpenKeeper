/*
 * Copyright (C) 2014-2015 OpenKeeper
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

/**
 * Some utility methods
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Utils {

    private static Boolean windows;

    private Utils() {
        // Nope
    }

    private static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * Is this OS MS Windows
     *
     * @return is Windows
     */
    public static boolean isWindows() {
        if (windows == null) {
            windows = getOsName().toLowerCase().startsWith("windows");
        }
        return windows;
    }
}
