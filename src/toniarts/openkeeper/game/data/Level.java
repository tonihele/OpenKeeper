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

import java.io.IOException;
import java.lang.System.Logger;
import javax.annotation.Nullable;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.KwdFile;

public final class Level extends GeneralLevel {

    private IKwdFile kwdFile;
    private static final Logger logger = System.getLogger(Level.class.getName());

    public enum LevelType {

        Level, MPD, Secret;
    }
    private final LevelType type;
    private final int level;
    private String variation = null;

    public Level(LevelType type, int level) {
        this.type = type;
        this.level = level;
    }

    public Level(LevelType type, int level, @Nullable String variation) {
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

    @Override
    public String getFileName() {
        return String.format("%s%s", getType(), this.toString());
    }

    @Override
    public IKwdFile getKwdFile() {
        if (kwdFile == null) {
            try {
                // Load the actual level info
                kwdFile = new KwdFile.KwdFileLoader(Main.getDkIIFolder()).load(getFileName(), false);
            } catch (IOException ex) {
                logger.log(Logger.Level.ERROR, "Failed to load the level file!", ex);
            }
        }
        return kwdFile;
    }

    @Override
    public String toString() {
        return String.format("%s%s", getLevel() > 0 ? getLevel() : "", getVariation());
    }
}
