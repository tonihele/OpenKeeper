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
package toniarts.openkeeper.tools.convert.hiscores;

/**
 * Stores the HiScores.dat file entry structure<br>
 *
 * @author ArchDemon
 */
public final class HiScoresEntry {

    private int score;
    private String name;
    private String level;

    public int getScore() {
        return score;
    }

    protected void setScore(int score) {
        this.score = score;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    protected void setLevel(String level) {
        this.level = level;
    }
}
