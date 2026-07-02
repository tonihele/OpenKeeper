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
package toniarts.openkeeper.game.trigger.actionpoint;

import com.jme3.util.SafeArrayList;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.data.ActionPoint;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.IGameLogicUpdatable;

/**
 *
 * @author ArchDemon
 */
public final class ActionPointTriggerLogicController implements IGameLogicUpdatable {

    private final SafeArrayList<ActionPointTriggerControl> triggers = new SafeArrayList<>(ActionPointTriggerControl.class);

    public ActionPointTriggerLogicController(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final IEntityPositionLookup entityPositionLookup) {

        // Create the triggers
        for (ActionPoint actionPoint : levelInfo.getActionPoints()) {
            if (actionPoint.getTriggerId() != 0) {
                triggers.add(new ActionPointTriggerControl(gameController, levelInfo, gameTimer, mapController, creaturesController, actionPoint.getTriggerId(), actionPoint, entityPositionLookup));
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void processTick(float tpf) {
        for (ActionPointTriggerControl triggerControl : triggers.getArray()) {
            triggerControl.update(tpf);
        }
    }
}
