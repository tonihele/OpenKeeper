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
package toniarts.openkeeper.game.data;

import com.jme3.system.AppSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import static toniarts.openkeeper.game.data.Level.LevelType.Level;
import static toniarts.openkeeper.game.data.Level.LevelType.MPD;
import static toniarts.openkeeper.game.data.Level.LevelType.Secret;

/**
 * Holds all kinds of game settings. These are per user, stored in user folder.
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Settings {

    public enum SettingCategory {

        GRAPHICS, CAMPAIGN, CONTROLS, SOUND, MISCELLANEOUS
    }

    public enum LevelStatus {

        COMPLETED, IN_PROGRESS, NOT_COMPLETED
    }

    public enum SecretLevelStatus {

        NOT_DISCOVED, DISCOVERED, IN_PROGRESS
    }

    /**
     * Settings
     */
    public enum Setting implements ISetting {

        // Campaign
        LEVEL_NUMBER("LevelNumber", Integer.class, 0, SettingCategory.CAMPAIGN, null),
        LEVEL_ATTEMPTS("LevelAttempts", Integer.class, 0, SettingCategory.CAMPAIGN, null),
        LEVEL_STATUS("LevelStatus", LevelStatus.class, LevelStatus.NOT_COMPLETED, SettingCategory.CAMPAIGN, null),
        SECRET_LEVEL_STATUS("SecretLevelStatus", LevelStatus.class, SecretLevelStatus.NOT_DISCOVED, SettingCategory.CAMPAIGN, null),
        MPD_LEVEL_STATUS("MPDLevelStatus", LevelStatus.class, LevelStatus.NOT_COMPLETED, SettingCategory.CAMPAIGN, null),
        // Graphic
        ANISOTROPY("Anisotrophy", Integer.class, 0, SettingCategory.GRAPHICS, null),
        SSAO("SSAO", Boolean.class, false, SettingCategory.GRAPHICS, null),
        // Screen recorder
        RECORDER_QUALITY("VideoRecorderQuality", Float.class, 0.8f, SettingCategory.MISCELLANEOUS, null),
        RECORDER_FPS("VideoRecorderFPS", Integer.class, 60, SettingCategory.MISCELLANEOUS, null);

        private Setting(String key, Class clazz, Object defValue, SettingCategory category, String resourceKey) {
            this.clazz = clazz;
            this.key = key;
            this.defValue = defValue;
            this.category = category;
            this.resourceKey = resourceKey;
        }

        @Override
        public Class getSettingClass() {
            return clazz;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getDefaultValue() {
            return defValue;
        }

        @Override
        public SettingCategory getCategory() {
            return category;
        }

        @Override
        public String getTranslationKey() {
            return resourceKey;
        }

        @Override
        public List<Setting> getSettings(SettingCategory category) {
            List<Setting> settings = new ArrayList<>();
            for (Setting setting : Setting.values()) {
                if (category != null && category.equals(setting.getCategory())) {
                    settings.add(setting);
                }
            }
            return settings;
        }
        private final Class clazz;
        private final String key;
        private final Object defValue;
        private final SettingCategory category;
        private final String resourceKey;
    }
    private static volatile Settings instance;
    private final AppSettings settings;
    private final static int MAX_FPS = 90;
    private final static String USER_HOME_FOLDER = System.getProperty("user.home").concat(File.separator).concat(".").concat(Main.TITLE).concat(File.separator);
    private final static String USER_SETTINGS_FILE = USER_HOME_FOLDER.concat("openkeeper.properties");
    private static final Logger logger = Logger.getLogger(Settings.class.getName());

    private Settings() {

        // Init the settings
        settings = new AppSettings(true);

        //Default resolution
        if (!settings.containsKey("Width") || !settings.containsKey("Height")) {
            settings.setResolution(800, 600); // Default resolution
        }
        File settingsFile = new File(USER_SETTINGS_FILE);
        if (settingsFile.exists()) {
            try {
                settings.load(new FileInputStream(settingsFile));
            } catch (IOException ex) {
                logger.log(java.util.logging.Level.WARNING, "Settings file failed to load from " + settingsFile + "!", ex);
            }
        }
        settings.setFrameRate(Math.max(MAX_FPS, settings.getFrequency()));
    }

    public static Settings getInstance() {
        if (instance == null) {
            synchronized (Settings.class) {
                if (instance == null) {
                    instance = new Settings();
                }
            }
        }
        return instance;
    }

    /**
     * Get the JME interface, should be used with care
     *
     * @return the JME app settings
     */
    public AppSettings getAppSettings() {
        return settings;
    }

    /**
     * Save the settings
     */
    public void save() throws IOException {
        settings.save(new FileOutputStream(new File(USER_SETTINGS_FILE)));
    }

    /**
     * Get the setting value
     *
     * @param setting the setting
     * @return the setting value
     */
    public Object getSetting(ISetting setting) {
        return getSetting(setting.getKey(), setting.getDefaultValue());
    }

    private Object getSetting(String key, Object defaultValue) {
        Object value = settings.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Get the setting value as integer
     *
     * @param setting the setting
     * @return the setting value
     */
    public int getSettingInteger(ISetting setting) {
        return (int) getSetting(setting);
    }

    /**
     * Get the setting value as boolean
     *
     * @param setting the setting
     * @return the setting value
     */
    public boolean getSettingBoolean(ISetting setting) {
        return (boolean) getSetting(setting);
    }

    /**
     * Get the setting value as float
     *
     * @param setting the setting
     * @return the setting value
     */
    public float getSettingFloat(ISetting setting) {
        return (float) getSetting(setting);
    }

    /**
     * Save a setting value
     *
     * @param setting setting to save
     * @param value the value to be saved
     */
    public void setSetting(ISetting setting, Object value) {
        setSetting(setting.getKey(), value);
    }

    private void setSetting(String key, Object value) {
        settings.put(key, (value.getClass().isEnum() ? value.toString() : value));
    }

    /**
     * Check whether the given key exists
     *
     * @param setting the setting
     * @return true if given setting exists
     */
    public boolean containsSetting(ISetting setting) {
        return settings.containsKey(setting.getKey());
    }

    /**
     * Get level attempts
     *
     * @param level the level
     * @return number of attempts to a level
     */
    public int getLevelAttempts(Level level) {
        return (int) getSetting(Setting.LEVEL_ATTEMPTS.toString() + level, Setting.LEVEL_ATTEMPTS.getDefaultValue());
    }

    /**
     * Get level status (MPD or normal)
     *
     * @param level the level
     * @return the level status
     */
    public LevelStatus getLevelStatus(Level level) {
        switch (level.getType()) {
            case Level:
                return LevelStatus.valueOf((String) getSetting(Setting.LEVEL_STATUS.toString() + level, Setting.LEVEL_STATUS.getDefaultValue()));
            case MPD:
                return LevelStatus.valueOf((String) getSetting(Setting.MPD_LEVEL_STATUS.toString() + level, Setting.MPD_LEVEL_STATUS.getDefaultValue()));
        }
        return null;
    }

    /**
     * Get secret level status
     *
     * @param level the secret level
     * @return the secret level status
     */
    public SecretLevelStatus getSecredLevelStatus(Level level) {
        switch (level.getType()) {
            case Secret:
                return SecretLevelStatus.valueOf((String) getSetting(Setting.SECRET_LEVEL_STATUS.toString() + level, Setting.SECRET_LEVEL_STATUS.getDefaultValue()));
        }
        return null;
    }

    /**
     * Get level attempts
     *
     * @param level the level
     * @return number of attempts to a level
     */
    public void increaseLevelAttempts(Level level) {
        setSetting(Setting.LEVEL_ATTEMPTS.toString() + level, getLevelAttempts(level) + 1);
    }

    /**
     * Get level status (MPD or normal)
     *
     * @param level the level
     * @return the level status
     */
    public void setLevelStatus(Level level, LevelStatus status) {
        switch (level.getType()) {
            case Level:
                setSetting(Setting.LEVEL_STATUS.toString() + level, status);
            case MPD:
                setSetting(Setting.MPD_LEVEL_STATUS.toString() + level, status);
        }
    }

    /**
     * Get secret level status
     *
     * @param level the secret level
     * @return the secret level status
     */
    public void setSecredLevelStatus(Level level, SecretLevelStatus status) {
        switch (level.getType()) {
            case Secret:
                setSetting(Setting.SECRET_LEVEL_STATUS.toString() + level, status);
        }
    }
}
