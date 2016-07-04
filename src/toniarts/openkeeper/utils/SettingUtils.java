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

import com.jme3.system.AppSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.AssetsConverter;

public class SettingUtils {
    private static final Logger LOGGER = Logger.getLogger(SettingUtils.class.getName());
    private final static String DKII_FOLDER_KEY = "DungeonKeeperIIFolder";
    private final static String SETTINGS_FILE = "openkeeper.properties";
    private final static AppSettings APP_SETTINGS = new AppSettings(false);
    private final static String TEST_FILE = AssetsConverter.MAPS_FOLDER.concat("FrontEnd3DLevel.kwd");
    
    public static String getDKIIFolder() {
        return getSettings().getString(DKII_FOLDER_KEY);
    }

    public static void setDKIIFolder(String dkIIFolder) {
        getSettings().putString(DKII_FOLDER_KEY, dkIIFolder);
    }
    
    public static AppSettings getSettings() {
        if (APP_SETTINGS.isEmpty()) {
            loadSettings();
        }
        return APP_SETTINGS;
    }
    
    public static void loadSettings() {
        // Init the application settings which contain just the conversion & folder data
        File settingsFile = new File(SETTINGS_FILE);
        if (settingsFile.exists()) {
            try {
                APP_SETTINGS.load(new FileInputStream(settingsFile));
            } catch (IOException ex) {
                LOGGER.log(java.util.logging.Level.WARNING, "Settings file failed to load from " + settingsFile + "!", ex);
            }
        }
    }
    
    public static void saveSettings() {
        try {
            getSettings().save(new FileOutputStream(new File(SETTINGS_FILE)));
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Settings file failed to save!", ex);
        }
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
            if (!folder.endsWith(File.separator)) {
                folder = folder.concat(File.separator);
            }
            File testFile = new File(folder.concat(TEST_FILE));
            return testFile.exists();
        }

        // Better luck next time
        return false;
    }
    
    public static String fixFilePath(final String filePath) {
        if (!filePath.endsWith(File.separator)) {
            return filePath.concat(File.separator);
        }
        return filePath;
    }
}
