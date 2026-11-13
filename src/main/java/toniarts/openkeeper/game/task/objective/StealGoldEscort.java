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
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

/**
 * Keeps a non-stealing party leader moving with the selected raider.
 * Its target follows the raider from the Treasury to the party's entrance.
 */
public final class StealGoldEscort extends AbstractObjectiveTask {

    private final ICreatureController raider;
    private final StealGold stealTask;
    private final short targetPlayerId;

    public StealGoldEscort(final INavigationService navigationService, final IMapController mapController,
            ICreatureController leader, ICreatureController raider, StealGold stealTask, short targetPlayerId) {
        super(navigationService, mapController, leader.getCreatureCoordinates(), leader.getOwnerId());
        this.raider = raider;
        this.stealTask = stealTask;
        this.targetPlayerId = targetPlayerId;
    }

    @Override
    public boolean isValid(ICreatureController creature) {
        if (creature == null) {
            return true;
        }
        IPartyController party = creature.getParty();
        Task raiderTask = stealTask.getCurrentTask();
        return party != null && party.isPartyLeader(creature)
                && Thing.HeroParty.Objective.STEAL_GOLD.equals(creature.getObjective())
                && targetPlayerId == creature.getObjectiveTargetPlayerId()
                && !raider.isRemoved() && !raider.isDead() && !raider.isIncapacitated()
                && raiderTask != null && raiderTask.isValid(raider)
                && raider.getAssignedTask() == raiderTask;
    }

    @Override
    public Vector2f getTarget(ICreatureController creature) {
        Task raiderTask = stealTask.getCurrentTask();
        return raiderTask != null ? raiderTask.getTarget(raider) : null;
    }

    @Override
    public void executeTask(ICreatureController creature, float executionDuration) {
        // The raider's task performs the interaction; the leader only keeps
        // the group moving along the same route.
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.GO_TO_LOCATION;
    }
}
