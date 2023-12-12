/*
 * Copyright (C) 2014-2019 OpenKeeper
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
package toniarts.openkeeper.game.sound;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;



/**
 *
 * @author ArchDemon
 */
public class GlobalCategory {
    
    private static final Logger LOGGER = System.getLogger(GlobalCategory.class.getName());

    public static final String AMBIENCE = "AMBIENCE";
    public static final String EFFECTS = "EFFECTS";
    public static final String FRONT_END = "FRONT_END";
    public static final String GUI_BUTTON_DEFAULT = "GUI_BUTTON_DEFAULT";
    public static final String GUI_BUTTON_ICON_LARGE = "GUI_BUTTON_ICON_LARGE";
    public static final String GUI_BUTTON_ICON = "GUI_BUTTON_ICON";
    public static final String GUI_BUTTON_INFO = "GUI_BUTTON_INFO";
    public static final String GUI_BUTTON_OPTIONS = "GUI_BUTTON_OPTIONS";
    public static final String GUI_BUTTON_REAPER_TALISMAN = "GUI_BUTTON_REAPER_TALISMAN";
    public static final String GUI_BUTTON_SIZE = "GUI_BUTTON_SIZE";
    public static final String GUI_BUTTON_TAB_CREATURE = "GUI_BUTTON_TAB_CREATURE";
    public static final String GUI_BUTTON_TAB_NEW_COMBAT = "GUI_BUTTON_TAB_NEW_COMBAT";
    public static final String GUI_BUTTON_TAB_ROOMS = "GUI_BUTTON_TAB_ROOMS";
    public static final String GUI_BUTTON_TAB_SPELLS = "GUI_BUTTON_TAB_SPELLS";
    public static final String GUI_BUTTON_TAB_WORKSHOP = "GUI_BUTTON_TAB_WORKSHOP";
    public static final String GUI_BUTTON_ZOOM = "GUI_BUTTON_ZOOM";
    public static final String GUI_MESSAGE_BAR = "GUI_MESSAGE_BAR";
    public static final String GUI_MINIMAP = "GUI_MINIMAP";
    public static final String GUI_SELL = "GUI_SELL";
    public static final String HAND = "HAND";
    public static final String MULTI_PLAYER = "MULTI_PLAYER";
    public static final String MUSIC = "MUSIC";
    public static final String ONE_SHOT_ATMOS = "ONE_SHOT_ATMOS";
    public static final String OPTIONS_MUSIC = "OPTIONS_MUSIC";
    public static final String OPTIONS_SPEECH = "OPTIONS_SPEECH";
    public static final String OPTIONS = "OPTIONS";
    public static final String WEAPON_FLESH = "WEAPON_FLESH";
    public static final String WEAPON_METAL = "WEAPON_METAL";
    public static final String WEAPON_SCYTHE = "WEAPON_SCYTHE";
    public static final String WEAPON_WOOD = "WEAPON_WOOD";

    /**
     * @see toniarts.openkeeper.game.sound.GlobalCategory.* constants
     * @return list of avaliable global categories
     */
    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();

        try {
            for (Field f : GlobalCategory.class.getFields()) {
                if (f.getType().equals(String.class)) {
                    categories.add((String) f.get(GlobalCategory.class));
                }
            }
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException ex) {
            LOGGER.log(Level.ERROR, ex.getMessage(), ex);
        }

        return categories;
    }

}
