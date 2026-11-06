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
package toniarts.openkeeper.tools.convert.map;

import java.util.EnumSet;

/**
 * Level data for the keeper defeat tests, the setters are not public
 */
public final class KeeperDefeatFixtures {

    private KeeperDefeatFixtures() {
        // Nope
    }

    /**
     * A player of the level
     *
     * @param playerId the player id
     * @param ai whether the level gives the player to the computer
     * @return the player
     */
    public static Player player(short playerId, boolean ai) {
        Player player = new Player();
        player.setPlayerId(playerId);
        player.setAi(ai);
        return player;
    }

    /**
     * The level settings
     *
     * @param flags the level flags
     * @return the level
     */
    public static GameLevel level(GameLevel.LevFlag... flags) {
        EnumSet<GameLevel.LevFlag> set = EnumSet.noneOf(GameLevel.LevFlag.class);
        for (GameLevel.LevFlag flag : flags) {
            set.add(flag);
        }

        GameLevel level = new GameLevel();
        level.setLvlFlags(set);
        return level;
    }

}
