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

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The order in which unemployed workers take tasks
 * ({@link TaskWorkerPriorityComparator})
 */
class WorkerTaskOrderTest {

    private static final Instant CREATED = Instant.parse("2026-10-21T12:00:00Z");

    @Test
    void floorIsClaimedBeforeWalls() {

        // An imp just claimed a tile, another imp already works on the floor next to it. The wall next
        // to it is free, but a free floor tile further away is still claimed first
        TaskManager.TaskWorkerPriority takenFloor = pair(TaskType.CLAIM_TILE, 1, 101);
        TaskManager.TaskWorkerPriority wall = pair(TaskType.CLAIM_WALL, 0, 177);
        TaskManager.TaskWorkerPriority farFloor = pair(TaskType.CLAIM_TILE, 0, 106);

        assertEquals(List.of(farFloor, takenFloor, wall), sorted(wall, takenFloor, farFloor));
    }

    @Test
    void wallsComeAfterEveryOtherTask() {
        TaskManager.TaskWorkerPriority wall = pair(TaskType.CLAIM_WALL, 0, 1);
        TaskManager.TaskWorkerPriority repair = pair(TaskType.REPAIR_WALL, 3, 500);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 2, 400);
        TaskManager.TaskWorkerPriority gold = pair(TaskType.CARRY_GOLD_TO_TREASURY, 1, 300);

        assertEquals(wall, sorted(wall, repair, dig, gold).getLast());
    }

    @Test
    void wallsAreReinforcedWhenThereIsNothingElse() {
        TaskManager.TaskWorkerPriority farWall = pair(TaskType.CLAIM_WALL, 0, 190);
        TaskManager.TaskWorkerPriority nearWall = pair(TaskType.CLAIM_WALL, 0, 177);

        assertEquals(List.of(nearWall, farWall), sorted(farWall, nearWall));
    }

    @Test
    void otherTasksKeepTheirOrder() {

        // Free tasks first, then the points (distance and priority)
        TaskManager.TaskWorkerPriority takenNearDig = pair(TaskType.DIG_TILE, 1, 11);
        TaskManager.TaskWorkerPriority freeFarFloor = pair(TaskType.CLAIM_TILE, 0, 130);
        TaskManager.TaskWorkerPriority freeNearFloor = pair(TaskType.CLAIM_TILE, 0, 102);

        assertEquals(List.of(freeNearFloor, freeFarFloor, takenNearDig), sorted(takenNearDig, freeFarFloor, freeNearFloor));
    }

    private static List<TaskManager.TaskWorkerPriority> sorted(TaskManager.TaskWorkerPriority... pairs) {
        List<TaskManager.TaskWorkerPriority> result = new ArrayList<>(List.of(pairs));
        result.sort(new TaskWorkerPriorityComparator());
        return result;
    }

    private static TaskManager.TaskWorkerPriority pair(TaskType taskType, int assignees, int points) {
        Task task = (Task) Proxy.newProxyInstance(Task.class.getClassLoader(), new Class<?>[]{Task.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getTaskType" ->
                taskType;
            case "getAssigneeCount" ->
                assignees;
            case "getTaskCreated" ->
                CREATED;
            case "toString" ->
                taskType + " (" + assignees + " workers, " + points + " points)";
            case "hashCode" ->
                System.identityHashCode(proxy);
            case "equals" ->
                proxy == args[0];
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        return new TaskManager.TaskWorkerPriority(task, null, points, null);
    }

}
