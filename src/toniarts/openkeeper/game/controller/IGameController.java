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

import com.simsilica.es.EntityId;
import java.util.Collection;
import toniarts.openkeeper.game.data.GameResult;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.ITaskManager;

/**
 * Game controller. Controls the game
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface IGameController {

    /**
     * Breaks alliance between two players
     *
     * @param playerOneId player ID 1
     * @param playerTwoId player ID 2
     */
    void breakAlliance(short playerOneId, short playerTwoId);

    /**
     * Creates alliance between two players
     *
     * @param playerOneId player ID 1
     * @param playerTwoId player ID 2
     */
    void createAlliance(short playerOneId, short playerTwoId);

    GameResult getGameResult();

    IPlayerController getPlayerController(short playerId);

    Collection<IPlayerController> getPlayerControllers();

    ITaskManager getTaskManager();

    void pauseGame();

    void resumeGame();

    /**
     * End the game (for one player)
     *
     * @param playerId the player
     * @param win if true, then he won, if not, well, he lost
     */
    void endGame(short playerId, boolean win);

    public INavigationService getNavigationService();

    public IEntityPositionLookup getEntityLookupService();

    public IGameWorldController getGameWorldController();

    /**
     * Set player possession mode on/off
     *
     * @param target possession target, null if possession ends
     * @param playerId player ID that posesses
     */
    public void setPossession(EntityId target, short playerId);

}
