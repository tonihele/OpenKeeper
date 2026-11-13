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
package toniarts.openkeeper.game.task.objective;

import com.jme3.math.Vector2f;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.creature.PartyObjectiveOrder;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Holds the leader at the position where the party received its WAIT order.
 * Followers remain in their normal follow state around the stationary leader.
 * Combat and fleeing still interrupt this task through the creature state
 * machine; after the interruption the leader resumes the same party order.
 */
public final class Wait extends AbstractObjectiveTask {

    private final Point waitPosition;

    public Wait(final INavigationService navigationService, final IMapController mapController,
            Point waitPosition, short playerId) {
        super(navigationService, mapController, waitPosition, playerId);
        this.waitPosition = waitPosition;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (creature == null) {
            return true;
        }
        if (creature.getObjective() != Thing.HeroParty.Objective.WAIT) {
            return false;
        }

        IPartyController party = creature.getParty();
        if (party == null) {
            return true;
        }
        if (!party.isPartyLeader(creature)) {
            return false;
        }
        PartyObjectiveOrder order = party.getObjectiveOrder();
        return order != null && waitPosition.equals(order.waitPosition());
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        return WorldUtils.pointToVector2f(waitPosition);
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        // A WAIT objective remains active until a trigger or map action changes it.
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_LOCATION;
    }
}
