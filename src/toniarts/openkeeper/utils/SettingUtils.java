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

public class SettingUtils {
    private static final Logger LOGGER = Logger.getLogger(SettingUtils.class.getName());
    private final static String SETTINGS_FILE = "openkeeper.properties";
    private final static AppSettings APP_SETTINGS = new AppSettings(false);
    
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
}
