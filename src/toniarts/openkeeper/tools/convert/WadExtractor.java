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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import toniarts.openkeeper.tools.convert.wad.WadFile;
import toniarts.openkeeper.utils.PathUtils;

/**
 * Simple class to extract all the files from given WAD to given location
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class WadExtractor {

    private static String dkIIFolder;

    public static void main(String[] args) {

        // Take Dungeon Keeper 2 root folder as parameter
        if (args.length != 2 || !Files.exists(Paths.get(args[1]))) {
            dkIIFolder = PathUtils.getDKIIFolder();
            if (dkIIFolder == null || args.length == 0)
            {
                throw new RuntimeException("Please provide extraction target folder as a first parameter! Second parameter is the Dungeon Keeper II root folder (optional)!");
            }
        } else {
            dkIIFolder = PathUtils.fixFilePath(args[1]);
        }

        final Path dataFolder = Paths.get(dkIIFolder, PathUtils.DKII_DATA_FOLDER, "Meshes.WAD");

        // And the destination
        String destination = PathUtils.fixFilePath(args[0]);

        // Extract the meshes
        WadFile wad = new WadFile(dataFolder);
        wad.extractFileData(destination.concat("meshes"));
    }
}
