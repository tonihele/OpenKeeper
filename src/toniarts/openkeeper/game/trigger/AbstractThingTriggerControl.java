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
package toniarts.openkeeper.game.trigger;

import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.player.PlayerTriggerControl;

/**
 * A base trigger control for Things, they seem to be very similar
 *
 * @param <T> the thing control type
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class AbstractThingTriggerControl<T> extends PlayerTriggerControl {

    protected T instanceControl;

    public AbstractThingTriggerControl() { // empty serialization constructor
        super();
    }

    public AbstractThingTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId, final short playerId,
            final PlayerService playerService) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId, playerId, playerService);
    }

    /**
     * Set the actual thing instance to this trigger control
     *
     * @param thingInstance the thing instance
     */
    protected void setThing(T thingInstance) {
        instanceControl = thingInstance;
    }
}
