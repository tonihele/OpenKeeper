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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



public class SettingUtils {

    private static final Logger LOGGER = System.getLogger(SettingUtils.class.getName());
    
    private final static Path SETTINGS_FILE = Paths.get("openkeeper.properties");
    private final AppSettings settings;
    private final static SettingUtils instance;

    static {
        instance = new SettingUtils(new AppSettings(false));
    }

    private SettingUtils(AppSettings settings) {
        this.settings = settings;
        loadSettings();
    }

    public static SettingUtils getInstance() {
        return instance;
    }

    public AppSettings getSettings() {
        return settings;
    }

    private void loadSettings() {

        // Init the application settings which contain just the conversion & folder data
        if (Files.exists(SETTINGS_FILE)) {
            try (InputStream in = Files.newInputStream(SETTINGS_FILE);
                    BufferedInputStream bin = new BufferedInputStream(in)) {
                settings.load(bin);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Settings file failed to load from " + SETTINGS_FILE + "!", ex);
            }
        }
    }

    public void saveSettings() {
        try (OutputStream out = Files.newOutputStream(SETTINGS_FILE);
                BufferedOutputStream bout = new BufferedOutputStream(out)) {
            settings.save(bout);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Settings file failed to save!", ex);
        }
    }
}
