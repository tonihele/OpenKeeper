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
import toniarts.openkeeper.tools.convert.map.TriggerGeneric;

/**
 *
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ObjectTriggerControl extends AbstractThingTriggerControl<IObjectController> {

    public ObjectTriggerControl(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final int triggerId, final short playerId,
            final PlayerService playerService) {
        super(gameController, levelInfo, gameTimer, mapController, creaturesController, triggerId, playerId, playerService);
    }

    @Override
    protected boolean isActive(TriggerGenericData trigger) {
        boolean result = false;

        float target = 0;

        TriggerGeneric.TargetType targetType = trigger.getType();
        switch (targetType) {
            case OBJECT_CLAIMED:
                return false;
            default:
                return super.isActive(trigger);
        }

//        TriggerGeneric.ComparisonType comparisonType = trigger.getComparison();
//        if (comparisonType != null && comparisonType != TriggerGeneric.ComparisonType.NONE) {
//            result = compare(target, comparisonType, (int) trigger.getUserData("value"));
//        }
//
//        return result;
    }
}
