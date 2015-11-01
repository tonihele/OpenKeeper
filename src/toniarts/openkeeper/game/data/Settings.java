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

import com.jme3.input.KeyInput;
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
        LEVEL_NUMBER(Integer.class, 0, SettingCategory.CAMPAIGN),
        LEVEL_ATTEMPTS(Integer.class, 0, SettingCategory.CAMPAIGN),
        LEVEL_STATUS(LevelStatus.class, LevelStatus.NOT_COMPLETED, SettingCategory.CAMPAIGN),
        SECRET_LEVEL_STATUS(LevelStatus.class, SecretLevelStatus.NOT_DISCOVED, SettingCategory.CAMPAIGN),
        MPD_LEVEL_STATUS(LevelStatus.class, LevelStatus.NOT_COMPLETED, SettingCategory.CAMPAIGN),
        // Graphic
        ANISOTROPY(Integer.class, 0, SettingCategory.GRAPHICS),
        SSAO(Boolean.class, false, SettingCategory.GRAPHICS),
        SSAO_SAMPLE_RADIUS(Float.class, 5.94f, SettingCategory.GRAPHICS),
        SSAO_INTENSITY(Float.class, 3.92f, SettingCategory.GRAPHICS),
        SSAO_SCALE(Float.class, 0.33f, SettingCategory.GRAPHICS),
        SSAO_BIAS(Float.class, 0.1f, SettingCategory.GRAPHICS),
        // Controls
        CAMERA_ZOOM_IN(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_W, SettingCategory.CONTROLS, 124),
        CAMERA_ZOOM_OUT(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_S, SettingCategory.CONTROLS, 125),
        CAMERA_UP(Integer.class, null, KeyInput.KEY_W, SettingCategory.CONTROLS, 118),
        CAMERA_DOWN(Integer.class, null, KeyInput.KEY_S, SettingCategory.CONTROLS, 119),
        CAMERA_LEFT(Integer.class, null, KeyInput.KEY_A, SettingCategory.CONTROLS, 106),
        CAMERA_RIGHT(Integer.class, null, KeyInput.KEY_D, SettingCategory.CONTROLS, 107),
        //CAMERA_ROTATE(Integer.class, null, KeyInput.KEY_LCONTROL, SettingCategory.CONTROLS, 120),
        CAMERA_ROTATE_LEFT(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_A, SettingCategory.CONTROLS, 122),
        CAMERA_ROTATE_RIGHT(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_D, SettingCategory.CONTROLS, 123),
        
        TOGGLE_SNIPER_MODE(Integer.class, null, KeyInput.KEY_INSERT, SettingCategory.CONTROLS, 1748),
        POSSESSED_PICK_LOCK_OR_DISARM(Integer.class, null, KeyInput.KEY_NUMPAD0, SettingCategory.CONTROLS, 1729),
        USE_ATTACK(Integer.class, null, KeyInput.KEY_SPACE, SettingCategory.CONTROLS, 2044),
        POSSESSED_CREEP(Integer.class, null, KeyInput.KEY_LCONTROL, SettingCategory.CONTROLS, 1730),
        POSSESSED_RUN(Integer.class, null, KeyInput.KEY_LSHIFT, SettingCategory.CONTROLS, 1732),
        
        POSSESSED_SELECT_MELEE(Integer.class, null, KeyInput.KEY_1, SettingCategory.CONTROLS, 1733),        
        POSSESSED_SELECT_SPELL_1(Integer.class, null, KeyInput.KEY_2, SettingCategory.CONTROLS, 1734),
        POSSESSED_SELECT_SPELL_2(Integer.class, null, KeyInput.KEY_3, SettingCategory.CONTROLS, 1735),
        POSSESSED_SELECT_SPELL_3(Integer.class, null, KeyInput.KEY_4, SettingCategory.CONTROLS, 1736),
        POSSESSED_SELECT_ABILITY_1(Integer.class, null, KeyInput.KEY_5, SettingCategory.CONTROLS, 1737),
        
        POSSESSED_SELECT_ABILITY_2(Integer.class, null, KeyInput.KEY_6, SettingCategory.CONTROLS, 1738),       
        POSSESSED_SELECT_GROUP(Integer.class, null, KeyInput.KEY_7, SettingCategory.CONTROLS, 1739),
        POSSESSED_REMOVE_FROM_GROUP(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_G, SettingCategory.CONTROLS, 1740),
        //ZOOM_IN(Integer.class, null, KeyInput.KEY_HOME, SettingCategory.CONTROLS, 124),
        //ZOOM_OUT(Integer.class, null, KeyInput.KEY_END, SettingCategory.CONTROLS, 125),
        
        //UP(Integer.class, null, KeyInput.KEY_UP, SettingCategory.CONTROLS, 118),       
        //DOWN(Integer.class, null, KeyInput.KEY_DOWN, SettingCategory.CONTROLS, 119,
        //LEFT(Integer.class, null, KeyInput.KEY_LEFT, SettingCategory.CONTROLS, 106),
        //RIGHT(Integer.class, null, KeyInput.KEY_RIGHT, SettingCategory.CONTROLS, 107),
        //ROTATE(Integer.class, null, KeyInput.KEY_LCONTROL, SettingCategory.CONTROLS, 120),
        
        Speed_Scroll(Integer.class, null, KeyInput.KEY_LSHIFT, SettingCategory.CONTROLS, 121),
        //ROTATE_VIEW_LEFT(Integer.class, null, KeyInput.KEY_DELETE, SettingCategory.CONTROLS, 122);
        //ROTATE_VIEW_RIGHT(Integer.class, null, KeyInput.KEY_PGDN, SettingCategory.CONTROLS, 123);
        PAUSE_OR_OPTIONS(Integer.class, null, KeyInput.KEY_ESCAPE, SettingCategory.CONTROLS, 1727),
        Screen_Shot(Integer.class, null, KeyInput.KEY_SYSRQ, SettingCategory.CONTROLS, 1728),
        
        PICKUP_OBJECTS_ONLY(Integer.class, null, KeyInput.KEY_PGUP, SettingCategory.CONTROLS, 1744),
        SEND_MESSAGE_TO_All_PLAYERS(Integer.class, null, KeyInput.KEY_TAB, SettingCategory.CONTROLS, 2831),
        SEND_MESSAGE_TO_Allies(Integer.class, KeyInput.KEY_LMENU, KeyInput.KEY_A, SettingCategory.CONTROLS, 2830),
        SEND_MESSAGE_TO_PLAYER_1(Integer.class, KeyInput.KEY_LMENU, KeyInput.KEY_1, SettingCategory.CONTROLS, 2832),
        SEND_MESSAGE_TO_PLAYER_2(Integer.class, KeyInput.KEY_LMENU, KeyInput.KEY_2, SettingCategory.CONTROLS, 2833),
        
        SEND_MESSAGE_TO_PLAYER_3(Integer.class, KeyInput.KEY_LMENU, KeyInput.KEY_3, SettingCategory.CONTROLS, 2834),
        SEND_MESSAGE_TO_PLAYER_4(Integer.class, KeyInput.KEY_LMENU, KeyInput.KEY_4, SettingCategory.CONTROLS, 2835),
        DISPLAY_PLAYERS(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_P, SettingCategory.CONTROLS, 2848),
        INCREASE_AMBIENT_LIGHT(Integer.class, null, KeyInput.KEY_EQUALS, SettingCategory.CONTROLS, 1749),
        DECREASE_AMBIENT_LIGHT(Integer.class, null, KeyInput.KEY_MINUS, SettingCategory.CONTROLS, 1750),
        
        ISOMETRIC_CAMERA(Integer.class, null, KeyInput.KEY_F1, SettingCategory.CONTROLS, 1741),
        Top_Down_CAMERA(Integer.class, null, KeyInput.KEY_F2, SettingCategory.CONTROLS, 1742),
        OBLIQUE_CAMERA(Integer.class, null, KeyInput.KEY_F3, SettingCategory.CONTROLS, 1743),
        USER_CAMERA_1(Integer.class, null, KeyInput.KEY_F4, SettingCategory.CONTROLS, 1486),
        USER_CAMERA_2(Integer.class, null, KeyInput.KEY_F5, SettingCategory.CONTROLS, 1487),
        
        USER_CAMERA_3(Integer.class, null, KeyInput.KEY_F6, SettingCategory.CONTROLS, 1488),
        TOGGLE_ALLY_WINDOW(Integer.class, null, KeyInput.KEY_A, SettingCategory.CONTROLS, 1751),
        ZOOM_TO_NEXT_FIGHT(Integer.class, null, KeyInput.KEY_F, SettingCategory.CONTROLS, 126),
        TOGGLE_GUI(Integer.class, null, KeyInput.KEY_G, SettingCategory.CONTROLS, 1745),
        ZOOM_TO_DUNGEON_HEART(Integer.class, null, KeyInput.KEY_H, SettingCategory.CONTROLS, 1298),
        
        TOGGLE_PLAYER_INFORMATION(Integer.class, null, KeyInput.KEY_I, SettingCategory.CONTROLS, 1752),
        MAP(Integer.class, null, KeyInput.KEY_M, SettingCategory.CONTROLS, 533),
        ZOOM_TO_PORTAL(Integer.class, null, KeyInput.KEY_P, SettingCategory.CONTROLS, 1299),
        CAMERA_MOUSE_ROTATE(Integer.class, null, KeyInput.KEY_X, SettingCategory.CONTROLS, 1746),
        CAMERA_MOUSE_ZOOM(Integer.class, null, KeyInput.KEY_Z, SettingCategory.CONTROLS, 1747),
        
        INCREASE_GAMMA(Integer.class, KeyInput.KEY_LSHIFT, KeyInput.KEY_PERIOD, SettingCategory.CONTROLS, 1761),
        DECREASE_GAMMA(Integer.class, KeyInput.KEY_LSHIFT, KeyInput.KEY_COMMA, SettingCategory.CONTROLS, 1762),
        PITCH_CAMERA_UP(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_HOME, SettingCategory.CONTROLS, 1757),
        PITCH_CAMERA_DOWN(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_END, SettingCategory.CONTROLS, 1758),
        ROLL_CAMERA_LEFT(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_INSERT, SettingCategory.CONTROLS, 1756),
        
        ROLL_CAMERA_RIGHT(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_DELETE, SettingCategory.CONTROLS, 1755),
        YAW_CAMERA_LEFT(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_PGUP, SettingCategory.CONTROLS, 1759),
        YAW_CAMERA_RIGHT(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_PGDN, SettingCategory.CONTROLS, 1760),
        PICKUP_HIGH_LEVEL_CREATURE(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_PERIOD, SettingCategory.CONTROLS, 1763),
        PICKUP_LOW_LEVEL_CREATURE(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_COMMA, SettingCategory.CONTROLS, 1764),
        
        QUICK_LOAD(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_L, SettingCategory.CONTROLS, 1753),
        QUICK_SAVE(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_S, SettingCategory.CONTROLS, 1754),  
        RESTART_LEVEL(Integer.class, KeyInput.KEY_LCONTROL, KeyInput.KEY_R, SettingCategory.CONTROLS, 1269),
        // Screen recorder
        RECORDER_QUALITY(Float.class, 0.8f, SettingCategory.MISCELLANEOUS),
        RECORDER_FPS(Integer.class, 60, SettingCategory.MISCELLANEOUS);

        private Setting(Class clazz, Integer specialKey, Object defValue, SettingCategory category, Integer resourceKey) {
            this.clazz = clazz;
            this.specialKey = specialKey;
            this.defValue = defValue;
            this.category = category;
            this.resourceKey = resourceKey;
        }
        
        private Setting(Class clazz, Object defValue, SettingCategory category, Integer resourceKey) {
            this(clazz, null, defValue, category, resourceKey);          
        }
        
        private Setting(Class clazz, Object defValue, SettingCategory category) {
            this(clazz, null, defValue, category, null);          
        }

        @Override
        public Class getSettingClass() {
            return clazz;
        }

        @Override
        public String getKey() {
            String name = name().toLowerCase();
            StringBuilder sb = new StringBuilder(name.length());
            for (String word : name.split("_")) {
                sb.append(word.substring(0, 1).toUpperCase());
                sb.append(word.substring(1));
            }
            return sb.toString();
        }

        @Override
        public Object getDefaultValue() {
            return defValue;
        }
        
        public Integer getSpecialKey() {
            return this.specialKey;
        }

        @Override
        public SettingCategory getCategory() {
            return category;
        }

        @Override
        public Integer getTranslationKey() {
            return resourceKey;
        }

        //@Override
        public static List<Setting> getSettings(SettingCategory category) {
            List<Setting> settings = new ArrayList<>();
            for (Setting setting : Setting.values()) {
                if (category != null && category.equals(setting.getCategory())) {
                    settings.add(setting);
                }
            }
            return settings;
        }
        private final Class clazz;
        private final Object defValue;
        private final SettingCategory category;
        private final Integer resourceKey;
        private final Integer specialKey;  // Control, Alt, Shift
    }
    private static volatile Settings instance;
    private final AppSettings settings;
    private final static int MAX_FPS = 90;
    private final static String USER_HOME_FOLDER = System.getProperty("user.home").concat(File.separator).concat(".").concat(Main.TITLE).concat(File.separator);
    private final static String USER_SETTINGS_FILE = USER_HOME_FOLDER.concat("openkeeper.properties");
    private static final Logger logger = Logger.getLogger(Settings.class.getName());

    private Settings(final AppSettings settings) {

        // Init the settings
        this.settings = settings;

        //Default resolution
        if (!this.settings.containsKey("Width") || !this.settings.containsKey("Height")) {
            this.settings.setResolution(800, 600); // Default resolution
        }
        File settingsFile = new File(USER_SETTINGS_FILE);
        if (settingsFile.exists()) {
            try {
                this.settings.load(new FileInputStream(settingsFile));
            } catch (IOException ex) {
                logger.log(java.util.logging.Level.WARNING, "Settings file failed to load from " + settingsFile + "!", ex);
            }
        }
        this.settings.setFrameRate(Math.max(MAX_FPS, settings.getFrequency()));
    }

    public static Settings getInstance(final AppSettings settings) {
        if (instance == null) {
            synchronized (Settings.class) {
                if (instance == null) {
                    instance = new Settings(settings);
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
