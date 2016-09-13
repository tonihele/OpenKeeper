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
package toniarts.openkeeper.game.data;

import java.util.ResourceBundle;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 *
 * @author ufdada
 */
public abstract class GeneralLevel {

    private static final Logger logger = Logger.getLogger(GeneralLevel.class.getName());
    final static String TEXT_DIR = "Interface/Texts/";
    private ResourceBundle resourceBundle;

    /**
     * Get the selected level title (value 0 and value 1 combined)
     *
     * @return level title
     */
    public String getTitle() {
        ResourceBundle dict = getResourceBundle();
        StringBuilder sb = new StringBuilder();
        String name = dict.getString("0");
        if (!name.isEmpty()) {
            // is empty on secret levels
            sb.append("\"");
            sb.append(name);
            sb.append("\" - ");
        }
        sb.append(dict.getString("1"));
        return sb.toString();
    }

    public String getMainObjective() {
        return getResourceBundle().getString("2");
    }

    public String getSubObjective1() {
        return getResourceBundle().getString("3");
    }

    public String getSubObjective2() {
        return getResourceBundle().getString("4");
    }

    public String getSubObjective3() {
        return getResourceBundle().getString("5");
    }

    /**
     * Gets the selected level briefing resource bundle
     *
     * @return the resource bundle
     */
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            this.resourceBundle = readResourceBundle();
        }
        return this.resourceBundle;
    }

    /**
     * Reads the resource bundle from disc and stores it
     *
     * @return the resource bundle
     */
    private ResourceBundle readResourceBundle() {
        GameLevel gameLevel = getKwdFile().getGameLevel();
        if (gameLevel.getName().equalsIgnoreCase("mpd7")) {
            // it was hardcoded in dk2 too
            return Main.getResourceBundle(TEXT_DIR.concat("LEVELMPD7_BRIEFING"));
        } else if (gameLevel.getTextTableId() != null && gameLevel.getTextTableId() != GameLevel.TextTable.NONE && gameLevel.getTextTableId().getLevelBriefingDictFile() != null) {
            return Main.getResourceBundle(TEXT_DIR.concat(gameLevel.getTextTableId().getLevelBriefingDictFile()));
        } else {
            final String briefing = gameLevel.getName().concat("_BRIEFING");
            try {
                return Main.getResourceBundle(TEXT_DIR.concat(briefing));
            } catch (Exception e) {
                // stack is already thrown by getResourceBundle
            }
        }
        return null;
    }

    public abstract KwdFile getKwdFile();

    public abstract String getFullName();
}
