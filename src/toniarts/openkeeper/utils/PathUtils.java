/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import java.io.File;
import toniarts.openkeeper.tools.convert.AssetsConverter;

public class PathUtils {
    public static final String DKII_DATA_FOLDER = "Data";
    public static final String DKII_SOUND_FOLDER = "Sound";
    public static final String DKII_SFX_FOLDER = "sfx";
    public static final String DKII_MOVIES_FOLDER = "Movies";
    public static final String DKII_TEXT_FOLDER = "Text";
    public static final String DKII_DEFAULT_FOLDER = "Default";
    public static final String DKII_EDITOR_FOLDER = "editor";
    public static final String DKII_MAPS_FOLDER = "maps";
    private final static String DKII_FOLDER_KEY = "DungeonKeeperIIFolder";
    private final static String TEST_FILE = AssetsConverter.MAPS_FOLDER.concat("FrontEnd3DLevel.kwd");
    
    /**
     * Get the folder of the original Dungeon Keeper 2 installation
     * @return Dungeon Keeper 2 folder
     */
    public static String getDKIIFolder() {
        return SettingUtils.getSettings().getString(DKII_FOLDER_KEY);
    }

    /**
     * Set the folder of the dk2 installation in the settings
     * 
     * @param dkIIFolder 
     */
    public static void setDKIIFolder(String dkIIFolder) {
        SettingUtils.getSettings().putString(DKII_FOLDER_KEY, dkIIFolder);
    }
    
    /**
     * Checks the DK 2 folder validity
     *
     * @param folder the supposed DK II folder
     * @return true if the folder is valid
     */
    public static boolean checkDkFolder(String folder) {
        // Throw a simple test to the folder, try to find a test file
        if (folder != null && !folder.isEmpty() && new File(folder).exists()) {
            File testFile = new File(PathUtils.fixFilePath(folder).concat(TEST_FILE));
            return testFile.exists();
        }

        // Better luck next time
        return false;
    }
    
    /**
     * Adds a file separator to the folder path if it doesn't end with one
     * 
     * @param folderPath path to the folder
     * @return folder with file separator at the end
     */
    public static String fixFilePath(final String folderPath) {
        if (!folderPath.endsWith(File.separator)) {
            return folderPath.concat(File.separator);
        }
        return folderPath;
    }
}
