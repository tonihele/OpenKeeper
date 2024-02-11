/*
 * Copyright (C) 2014-2017 OpenKeeper
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

import java.util.Deque;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.task.Task;

/**
 * Interface for chainable complex tasks
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public interface ObjectiveTask extends Task {

    /**
     * Add a sub task that should be completed first
     *
     * @param task
     */
    default void addSubTask(ObjectiveTask task) {
        getTaskQueue().addLast(task);
    }

    /**
     * Get the next task. Advances the task queue
     *
     * @return the next task
     */
    default Task getNextTask() {
        Task nextTask = getTaskQueue().peekFirst();
        if (nextTask != null) {
            if (nextTask instanceof ObjectiveTask nextObjectiveTask) {
                nextTask = nextObjectiveTask.getNextTask();
                if (nextTask == null) {
                    getTaskQueue().removeFirst();
                    return getNextTask();
                }
                return nextTask;
            }
            getTaskQueue().removeFirst();
            return nextTask;
        }
        return null;
    }

    /**
     * Get the current task
     *
     * @return the current task
     */
    default Task getCurrentTask() {
        Task nextTask = getTaskQueue().peekFirst();
        if (nextTask != null) {
            if (nextTask instanceof ObjectiveTask nextObjectiveTask) {
                nextObjectiveTask.getCurrentTask();
            }
            return nextTask;
        }
        return this;
    }

    /**
     * Get the task queue storage for this objective task
     *
     * @return the task queue
     */
    Deque<ObjectiveTask> getTaskQueue();

    default boolean isWorkerPartyTask() {
        return false;
    }

    @Override
    public default void executeTask(ICreatureController creature, float executionDuration) {
        if (!isValid(creature)) {

            // Assign next task
            Task nextTask = getNextTask();
            if (nextTask != null) {
                if (nextTask instanceof ObjectiveTask && ((ObjectiveTask) nextTask).isWorkerPartyTask() && creature.getParty() != null) {

                    // Assign to all workers
                    for (ICreatureController c : creature.getParty().getActualMembers()) {
                        if (!c.equals(creature) && c.isWorker() && nextTask.canAssign(c)) {
                            nextTask.assign(c, true);
                        }
                    }
                }
                nextTask.assign(creature, true);
            }
        }
    }

}
