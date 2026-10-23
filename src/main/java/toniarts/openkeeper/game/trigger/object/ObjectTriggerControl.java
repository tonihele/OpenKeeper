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
package toniarts.openkeeper.game.trigger.object;

import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.game.trigger.TriggerGenericData;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ObjectTriggerControl extends AbstractThingTriggerControl<IObjectController> {

    private short lastOwnerId;

    public ObjectTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId, final short playerId,
            final PlayerService playerService) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId, playerId, playerService);
    }

    @Override
    protected void setThing(IObjectController thingInstance) {
        super.setThing(thingInstance);
        if (thingInstance != null) {
            lastOwnerId = thingInstance.getOwnerId();
        }
    }

    /**
     * The original does not walk the tree of an object on its own. The tree
     * only gets the claimed event, so nothing else in it ever runs.
     */
    @Override
    protected void updateControl(float tpf) {
        if (isArmed()) {
            pollEvents();
        }
    }

    @Override
    protected void pollEvents() {
        super.pollEvents();

        // Event: this object was claimed, i.e. a neutral object got an owner. Changing hands
        // between the keepers does not count
        if (instanceControl.isRemoved()) {
            return;
        }
        short ownerId = instanceControl.getOwnerId();
        if (lastOwnerId == Player.NEUTRAL_PLAYER_ID && ownerId != Player.NEUTRAL_PLAYER_ID) {
            fire(TriggerGeneric.TargetType.OBJECT_CLAIMED);
        }
        lastOwnerId = ownerId;
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case OBJECT_CLAIMED:
                return false; // Event, see pollEvents()
            default:
                return super.isActive(trigger);
        }
    }
}
