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
package toniarts.openkeeper.game.trigger.player;

import com.jme3.util.SafeArrayList;
import java.util.Map;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.tools.convert.map.Player;

/**
 * Handles player triggers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class PlayerTriggerLogicController implements IGameLogicUpdatable {

    private final SafeArrayList<PlayerTriggerControl> playerTriggerControls = new SafeArrayList<>(PlayerTriggerControl.class);

    public PlayerTriggerLogicController(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final PlayerService playerService) {
        Map<Short, Player> players = levelInfo.getLevelData().getPlayers();
        for (Keeper keeper : levelInfo.getPlayers()) {
            int triggerId = players.get(keeper.getId()).getTriggerId();
            if (triggerId != 0) {
                playerTriggerControls.add(new PlayerTriggerControl(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId, keeper.getId(), playerService));
            }
        }

        // TODO: get notified on new parties (a.k.a. parties re-spawned)
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void processTick(float tpf, double gameTime) {
        for (PlayerTriggerControl playerTriggerControl : playerTriggerControls.getArray()) {
            playerTriggerControl.update(tpf);
        }
    }
}
