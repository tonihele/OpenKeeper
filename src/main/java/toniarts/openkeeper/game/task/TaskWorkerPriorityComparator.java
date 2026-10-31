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
package toniarts.openkeeper.game.task;

import java.util.Comparator;

/**
 * The order in which the unemployed workers take the tasks
 */
final class TaskWorkerPriorityComparator implements Comparator<TaskManager.TaskWorkerPriority> {

    @Override
    public int compare(TaskManager.TaskWorkerPriority o1, TaskManager.TaskWorkerPriority o2) {

        // A dropped worker first takes the closest work around the drop, whatever it is
        int result = Integer.compare(o1.dropRing(), o2.dropRing());
        if (result != 0) {
            return result;
        }

        // A digging or claiming worker carries on with the closest work of the same kind, however far
        result = Boolean.compare(o2.sameWork(), o1.sameWork());
        if (result != 0) {
            return result;
        }
        if (o1.sameWork()) {
            result = Integer.compare(o1.points(), o2.points());
            if (result != 0) {
                return result;
            }
        }

        // Walls are only reinforced once there is nothing else to do, the floor is claimed first
        result = Boolean.compare(isClaimWall(o1.task()), isClaimWall(o2.task()));
        if (result != 0) {
            return result;
        }

        // Fill in jobs that have no workers first
        result = Integer.compare(o1.task().getAssigneeCount(), o2.task().getAssigneeCount());
        if (result != 0) {
            return result;
        }

        // Closest creature gets the job
        result = Integer.compare(o1.points(), o2.points());
        if (result != 0) {
            return result;
        }

        // If the same, compare by date added
        return o1.task().getTaskCreated().compareTo(o2.task().getTaskCreated());
    }

    private static boolean isClaimWall(Task task) {
        return task.getTaskType() == TaskType.CLAIM_WALL;
    }

}
