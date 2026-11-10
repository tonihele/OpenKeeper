/*
 * Copyright (C) 2014-2016 OpenKeeper
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

import com.simsilica.es.EntityData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.object.IObjectController;
import toniarts.openkeeper.game.state.session.PlayerService;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerControl;
import toniarts.openkeeper.game.trigger.AbstractThingTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * A state for handling object triggers
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class ObjectTriggerLogicController extends AbstractThingTriggerLogicController<IObjectController> {

    public ObjectTriggerLogicController(final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final PlayerService playerService, final EntityData entityData,
            final IObjectsController objectsController) {
        super(initTriggers(levelInfo.getLevelData().getThings(Thing.Object.class), gameController, levelInfo, gameTimer, mapController,
                creaturesController, playerService),
                entityData.getEntities(ObjectComponent.class, Trigger.class),
                objectsController);
    }

    private static Map<Integer, AbstractThingTriggerControl<IObjectController>> initTriggers(List<Thing.Object> things, final IGameController gameController, final ILevelInfo levelInfo, final IGameTimer gameTimer, final IMapController mapController,
            final ICreaturesController creaturesController, final PlayerService playerService) {
        Map<Integer, AbstractThingTriggerControl<IObjectController>> objectTriggers = new HashMap<>();
        for (Thing.Object object : things) {
            if (object.getTriggerId() != 0) {
                objectTriggers.put(object.getTriggerId(), new ObjectTriggerControl(gameController, levelInfo, gameTimer, mapController,
                        creaturesController, object.getTriggerId(), object.getPlayerId(), playerService));
            }
        }
        return objectTriggers;
    }

}
