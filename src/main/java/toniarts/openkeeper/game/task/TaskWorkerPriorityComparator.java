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
import toniarts.openkeeper.game.task.worker.ClaimTileTask;

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

        // Trap and door crates are brought to their blueprints before digging, claiming and the like, as in the
        // original's imp work order (DKII.EXE 0x463DF0). Both are one job there (0x46C1A0, and 0x468910 for a loose crate)
        result = Boolean.compare(isCrateForBlueprint(o2.task()), isCrateForBlueprint(o1.task()));
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

        // The floor that digging has left, with no digging tagged around it, is claimed before the other usual work. It
        // is the first job on the original's work list (DKII.EXE 0x469600, list at 0x698658)
        result = Boolean.compare(isDugFloorToClaim(o2.task()), isDugFloorToClaim(o1.task()));
        if (result != 0) {
            return result;
        }

        // Walls are only worked on once there is nothing else to do, the floor is claimed first. Repairing goes with
        // reinforcing, in the original (DKII.EXE 0x46AEA0) both are one job that only comes after the usual work
        result = Boolean.compare(isWallWork(o1.task()), isWallWork(o2.task()));
        if (result != 0) {
            return result;
        }

        // Closest creature gets the job, whether or not someone already works on it. The original's imp scans rings of
        // tiles outwards for work (DKII.EXE 0x468D10) and takes over a job held by another imp only when it is closer
        // (0x46AB50, 0x46AEA0)
        result = Integer.compare(o1.points(), o2.points());
        if (result != 0) {
            return result;
        }

        // If the same, compare by date added
        return o1.task().getTaskCreated().compareTo(o2.task().getTaskCreated());
    }

    private static boolean isCrateForBlueprint(Task task) {
        return switch (task.getTaskType()) {
            case FETCH_TRAP_CRATE, DELIVER_TRAP_CRATE, FETCH_DOOR_CRATE, DELIVER_DOOR_CRATE ->
                true;
            default ->
                false;
        };
    }

    private static boolean isDugFloorToClaim(Task task) {
        return task instanceof ClaimTileTask claimTileTask && claimTileTask.isDugFloorToClaim();
    }

    private static boolean isWallWork(Task task) {
        return task.getTaskType() == TaskType.CLAIM_WALL || task.getTaskType() == TaskType.REPAIR_WALL;
    }

}
