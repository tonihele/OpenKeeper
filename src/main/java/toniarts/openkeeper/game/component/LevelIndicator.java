/*
 * Copyright (C) 2014-2026 OpenKeeper
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
package toniarts.openkeeper.game.component;

import com.simsilica.es.EntityComponent;
import toniarts.openkeeper.utils.Utils;

/**
 * The level the My Pet Dungeon level indicator is set to. The Hero Toolbox
 * creatures are kept at this level.
 *
 * @author OpenKeeper team
 */
public final class LevelIndicator implements EntityComponent {

    public int level;

    public LevelIndicator() {
        // For serialization
    }

    public LevelIndicator(int level) {
        this.level = level;
    }

    /**
     * Turns the indicator, round from the highest creature level back to the
     * first and the other way
     *
     * @param levels how many levels to turn, negative turns down
     * @return the turned indicator
     */
    public LevelIndicator turn(int levels) {
        return new LevelIndicator(Math.floorMod(level - 1 + levels, Utils.MAX_CREATURE_LEVEL) + 1);
    }

}
