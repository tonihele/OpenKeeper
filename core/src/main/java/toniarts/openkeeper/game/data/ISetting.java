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

import java.util.List;

/**
 * General setting
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ISetting<T> {

    /**
     * For UIs and whatnot, the setting object type
     *
     * @return the setting object type
     */
    public Class<T> getSettingClass();

    /**
     * Get the setting identifier key
     *
     * @return the key
     */
    public String getKey();

    /**
     * Get the default value for this setting
     *
     * @return default value
     */
    public T getDefaultValue();

    /**
     * Get the setting category
     *
     * @return category
     */
    public Settings.SettingCategory getCategory();

    /**
     * Get the translation key
     *
     * @return key in resource bundle
     */
    public Integer getTranslationKey();

    /**
     * Get settings by category
     *
     * @param category the category
     * @return all the settings by category
     */
    //public static List<ISetting<T>> getSettings(Settings.SettingCategory category);
}
