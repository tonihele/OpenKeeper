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

public class Level {

    public enum LevelType {

        Level, MPD, Secret;
    }
    private final LevelType type;
    private final int level;
    private final String variation;

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
}
