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
package toniarts.openkeeper.game.task;

import toniarts.openkeeper.game.controller.ai.ICreatureController;
import toniarts.openkeeper.game.controller.room.AbstractRoomController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Thing;

/**
 * Simple task manager for creatures
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ITaskManager {

    /**
     * Assigns closest room task to a given creature of requested type
     *
     * @param creature the creature to assign to
     * @param objectType the type of room service
     * @return true if the task was assigned
     */
    boolean assignClosestRoomTask(ICreatureController creature, AbstractRoomController.ObjectType objectType);

    /**
     * Assigns gold to treasury task to the given creature
     *
     * @param creature the creature to assign to
     * @return true if the task was assigned
     */
    boolean assignGoldToTreasuryTask(ICreatureController creature);

    /**
     * Assign a task according to the creature's objectives
     *
     * @param creature the creature
     * @param objective the objective
     * @return true if the objective task could be accomplished
     */
    boolean assignObjectiveTask(ICreatureController creature, Thing.HeroParty.Objective objective);

    /**
     * Assigns a go to sleep task to the player if the lair is accessible
     *
     * @param creature the creature wanting to go to sleep
     * @return true if the task was assigned
     */
    boolean assignSleepTask(ICreatureController creature);

    /**
     * Assign a task to a creature
     *
     * @param creature the creature to assign a task to
     * @param byDistance whether we should assign the closest task (i.e. if a
     * player drops the creature somewhere)
     * @return true if a task was assigned
     */
    boolean assignTask(ICreatureController creature, boolean byDistance);

    /**
     * Assigns a creature to given task type
     *
     * @param creature the creature asking for the task
     * @param jobType the job type to ask for
     * @return true if the task was assigned
     */
    boolean assignTask(ICreatureController creature, Creature.JobType jobType);

    /**
     * Test if a given task type is available
     *
     * @param creature the creature asking for the task
     * @param jobType the job type to ask for
     * @return true if task type is available
     */
    boolean isTaskAvailable(ICreatureController creature, Creature.JobType jobType);

}
