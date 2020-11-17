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
package toniarts.openkeeper.tools.convert;

import java.io.File;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * A simple class to load up a map file
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class MapLoader {

    private static String dkIIFolder;

    public static void main(String[] args) {

        // Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !new File(args[1]).exists()) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null || args.length == 0)
            {
                throw new RuntimeException("Please provide actual map file as a first parameter! Second parameter is the Dungeon Keeper II main folder (optional)!");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[1]);
        }

        // Load the map
        KwdFile kwd = new KwdFile(dkIIFolder, new File(args[0]));
    }
}
