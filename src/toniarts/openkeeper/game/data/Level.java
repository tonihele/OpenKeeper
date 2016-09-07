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

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.GameLevel;
import toniarts.openkeeper.tools.convert.map.KwdFile;

public class Level {

    public enum LevelType {

        Level, MPD, Secret;
    }
    private final LevelType type;
    private final int level;
    private final String variation;
    private KwdFile kwdFile = null;
    private static final Logger logger = Logger.getLogger(Level.class.getName());

    public Level(LevelType type, int level, String variation) {
        this.type = type;
        this.level = level;
        this.variation = variation;
    }

    public int getLevel() {
        return level;
    }

    public LevelType getType() {
        return type;
    }

    public String getVariation() {
        return variation != null ? variation : "";
    }

    public String getFullName() {
        return getType().toString() + (getLevel() > 0 ? getLevel() : "") + getVariation();
    }

    public KwdFile getKwdFile() {
        if (kwdFile == null) {
            try {

                // Load the actual level info
                kwdFile = new KwdFile(Main.getDkIIFolder(),
                        new File(ConversionUtils.getRealFileName(Main.getDkIIFolder(), AssetsConverter.MAPS_FOLDER + String.format("%s%s%s", getType(), getLevel(), getVariation()) + ".kwd")), false);
            } catch (IOException ex) {
                logger.log(java.util.logging.Level.SEVERE, "Failed to load the level file!", ex);
            }
        }
        return kwdFile;
    }

    /**
     * Get the selected level title
     *
     * @return level title
     */
    public String getTitle() {
        ResourceBundle dict = getResourceBundle();
        StringBuilder sb = new StringBuilder();
        String name = dict.getString("0");
        if (!name.equals("")) {
            sb.append("\"");
            sb.append(name);
            sb.append("\" - ");
        }
        sb.append(dict.getString("1"));
        return sb.toString();
    }

    /**
     * Gets the selected level briefing resource bundle
     *
     * @return the resource bundle
     */
    public ResourceBundle getResourceBundle() {
        GameLevel gameLevel = getKwdFile().getGameLevel();
        if (gameLevel.getTextTableId().getLevelBriefingDictFile() != null) {
            return Main.getResourceBundle("Interface/Texts/".concat(gameLevel.getTextTableId().getLevelBriefingDictFile()));
        }
        return null;
    }
}
