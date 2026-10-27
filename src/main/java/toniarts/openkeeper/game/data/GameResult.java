/*
 * Copyright (C) 2014-2017 OpenKeeper
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

import java.util.HashMap;
import java.util.Map;

/**
 * The result and statistics of a game for a player, shown on the debriefing screen
 *
 * @author archdemon
 */
public final class GameResult {

    public enum ResultType {
        TOTAL_RATING,
        OVERALL_TOTAL_RATING,
        SPECIALS_FOUND,
        SPECIALS_MAX,

        LEVEL_WON,
        TIME_TAKEN,
        ENEMY_CREATURES_KILLED,
        HEROES_DESTROYED,
        ROOMS_CAPTURED,
        LAND_OWNED,
        GOLD_MINED,
        MANA_STORED,
        ITEMS_MANUFACTURED,
        CREATURES_COMMANDED,
        CREATURES_CONVERTED,
        CREATURE_TRAINING,
        ATTEMPTS_AT_LEVEL,
    };

    /**
     * The Evil Rating weights per statistic, from DK II
     */
    private static final int ENEMY_CREATURE_KILLED_RATING = 40;
    private static final int HERO_DESTROYED_RATING = 50;
    private static final int ROOM_CAPTURED_RATING = 5;
    private static final int LAND_OWNED_RATING = 5;
    private static final int GOLD_MINED_RATING = 1;
    private static final int MANA_STORED_RATING = 2;
    private static final int ITEM_MANUFACTURED_RATING = 5;
    private static final int CREATURE_COMMANDED_RATING = 20;
    private static final int CREATURE_CONVERTED_RATING = 40;
    private static final int CREATURE_TRAINING_RATING = 50;

    private Map<ResultType, Object> data = null;

    public GameResult() {

    }

    public <T> T getData(ResultType key) {
        if (data == null) {
            return null;
        }

        Object s = data.get(key);
        return (T) s;
    }

    public void setData(ResultType key, Object value) {
        if (data == null) {
            data = new HashMap<>();
        }
        if (data.containsKey(key)) {
            return;
        }

        data.put(key, value);
    }

    /**
     * See if the level was won. A result that doesn't say so is no win.
     *
     * @return {@code true} if the level was won
     */
    public boolean isLevelWon() {
        return Boolean.TRUE.equals(getData(ResultType.LEVEL_WON));
    }

    /**
     * Calculates the Evil Rating of the level from the statistics. It is a weighted sum of the
     * statistics, the won or lost status and the time taken don't count. The creature training
     * (the average experience level) counts with two decimals, cut off rather than rounded, as DK II
     * does. Statistics that are not set count as zero.
     *
     * @return the Evil Rating of the level
     */
    public int calculateEvilRating() {
        Number training = getData(ResultType.CREATURE_TRAINING);
        float averageLevel = training != null ? training.floatValue() : 0;
        int level = (int) averageLevel;
        int levelHundredths = (int) ((averageLevel - level) * 100);

        return ENEMY_CREATURE_KILLED_RATING * getInt(ResultType.ENEMY_CREATURES_KILLED)
                + HERO_DESTROYED_RATING * getInt(ResultType.HEROES_DESTROYED)
                + ROOM_CAPTURED_RATING * getInt(ResultType.ROOMS_CAPTURED)
                + LAND_OWNED_RATING * getInt(ResultType.LAND_OWNED)
                + GOLD_MINED_RATING * getInt(ResultType.GOLD_MINED)
                + MANA_STORED_RATING * getInt(ResultType.MANA_STORED)
                + ITEM_MANUFACTURED_RATING * getInt(ResultType.ITEMS_MANUFACTURED)
                + CREATURE_COMMANDED_RATING * getInt(ResultType.CREATURES_COMMANDED)
                + CREATURE_CONVERTED_RATING * getInt(ResultType.CREATURES_CONVERTED)
                + CREATURE_TRAINING_RATING * level + CREATURE_TRAINING_RATING * levelHundredths / 100;
    }

    private int getInt(ResultType key) {
        Number value = getData(key);
        return value != null ? value.intValue() : 0;
    }
}
