/*
 * Copyright (C) 2014-2018 OpenKeeper
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
package toniarts.openkeeper.game.controller;

import java.util.Collection;
import java.util.List;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.data.GameTimer;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.tools.convert.map.KwdFile;

/**
 * General level related info
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ILevelInfo {

    /**
     * Get the level raw data file
     *
     * @return the KWD
     */
    KwdFile getLevelData();

    Keeper getPlayer(short playerId);

    Collection<Keeper> getPlayers();

    int getFlag(int id);

    /**
     * Get level score, not really a player score... kinda
     *
     * @return the level score
     */
    int getLevelScore();

    Float getTimeLimit();

    GameTimer getTimer(int id);

    void setFlag(int id, int value);

    void setLevelScore(int levelScore);

    void setTimeLimit(float timeLimit);

    /**
     * Get action point by id
     *
     * @param id the id
     * @return ActionPoint
     */
    ActionPoint getActionPoint(int id);

    /**
     * Get action points
     *
     * @return ActionPoints
     */
    List<ActionPoint> getActionPoints();

}
