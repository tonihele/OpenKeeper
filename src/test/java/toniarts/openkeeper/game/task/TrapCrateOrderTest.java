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
 * Where bringing trap crates to their blueprints comes in the order in which
 * unemployed workers take tasks ({@link TaskWorkerPriorityComparator})
 */
class TrapCrateOrderTest {

    private static final Instant CREATED = Instant.parse("2026-10-25T12:00:00Z");

    @Test
    void crateIsBroughtBeforeDiggingAndClaiming() {

        // The crate is far away and worth less points than the digging and claiming next to the imp
        TaskManager.TaskWorkerPriority fetchCrate = pair(TaskType.FETCH_TRAP_CRATE, 0, 140);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 11);
        TaskManager.TaskWorkerPriority claim = pair(TaskType.CLAIM_TILE, 0, 101);

        assertEquals(List.of(fetchCrate, dig, claim), sorted(claim, dig, fetchCrate));
    }

    @Test
    void deliveringCountsTheSame() {
        TaskManager.TaskWorkerPriority deliverCrate = pair(TaskType.DELIVER_TRAP_CRATE, 0, 150);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 11);

        assertEquals(List.of(deliverCrate, dig), sorted(dig, deliverCrate));
    }

    @Test
    void cratesAmongThemselvesKeepTheUsualOrder() {
        TaskManager.TaskWorkerPriority farCrate = pair(TaskType.FETCH_TRAP_CRATE, 0, 160);
        TaskManager.TaskWorkerPriority nearCrate = pair(TaskType.FETCH_TRAP_CRATE, 0, 120);

        assertEquals(List.of(nearCrate, farCrate), sorted(farCrate, nearCrate));
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
