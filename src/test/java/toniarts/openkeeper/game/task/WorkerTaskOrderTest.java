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
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The order in which unemployed workers take tasks
 * ({@link TaskManager#sortByTaskStatusAndDistance})
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

        assertEquals(List.of(gold, dig, wall, repair), sorted(wall, repair, dig, gold));
    }

    @Test
    void damagedWallsAreRepairedWithTheOtherWallWork() {

        // As in the original, repairing is not done first: it comes after the usual work, together with
        // reinforcing, the free and closest first
        TaskManager.TaskWorkerPriority repair = pair(TaskType.REPAIR_WALL, 0, 140);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 12);
        TaskManager.TaskWorkerPriority claim = pair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority gold = pair(TaskType.CARRY_GOLD_TO_TREASURY, 0, 105);
        TaskManager.TaskWorkerPriority wall = pair(TaskType.CLAIM_WALL, 0, 177);

        assertEquals(List.of(dig, claim, gold, repair, wall), sorted(wall, gold, claim, dig, repair));
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

    @Test
    void droppedImpTakesTheClosestWorkAroundTheDrop() {

        // Dropped next to a wall, with floor to claim two tiles away and digging elsewhere: the wall comes first,
        // whatever kind of work it is
        TaskManager.TaskWorkerPriority wallNextToDrop = pair(TaskType.CLAIM_WALL, 0, 177, 1);
        TaskManager.TaskWorkerPriority claimTwoTilesAway = pair(TaskType.CLAIM_TILE, 0, 102, 2);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 11);

        assertEquals(List.of(wallNextToDrop, claimTwoTilesAway, dig), sorted(dig, claimTwoTilesAway, wallNextToDrop));
    }

    @Test
    void dropAreaIsTwoTilesInEveryDirection() {
        Point drop = new Point(10, 10);

        assertEquals(0, TaskManager.getDropRing(task(TaskType.CLAIM_TILE, new Point(10, 10)), drop));
        assertEquals(1, TaskManager.getDropRing(task(TaskType.DIG_TILE, new Point(11, 9)), drop));
        assertEquals(2, TaskManager.getDropRing(task(TaskType.CLAIM_WALL, new Point(12, 12)), drop));
        assertEquals(2, TaskManager.getDropRing(task(TaskType.REPAIR_WALL, new Point(8, 11)), drop));
        assertEquals(Integer.MAX_VALUE, TaskManager.getDropRing(task(TaskType.CLAIM_TILE, new Point(13, 10)), drop));
        assertEquals(Integer.MAX_VALUE, TaskManager.getDropRing(task(TaskType.CARRY_GOLD_TO_TREASURY, null), drop));
    }

    @Test
    void claimingImpCarriesOnWithTheClosestClaim() {

        // The imp just claimed a tile: it carries on claiming, although digging is closer and worth more
        TaskManager.TaskWorkerPriority nextClaim = sameWorkPair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 12);

        assertEquals(List.of(nextClaim, dig), sorted(dig, nextClaim));
    }

    @Test
    void carryingOnTakesTheClosestHoweverFarAndWhoeverWorksThere() {

        // As in the original, the closest of the same kind of work, not a free one further away
        TaskManager.TaskWorkerPriority takenNearClaim = sameWorkPair(TaskType.CLAIM_TILE, 1, 102);
        TaskManager.TaskWorkerPriority freeFarClaim = sameWorkPair(TaskType.CLAIM_TILE, 0, 140);

        assertEquals(List.of(takenNearClaim, freeFarClaim), sorted(freeFarClaim, takenNearClaim));
    }

    @Test
    void diggingImpCarriesOnDigging() {

        // The imp just dug a tile, another marked tile is 20 tiles away: it digs on, although there is
        // floor to claim right next to it and gold to carry
        TaskManager.TaskWorkerPriority nextDig = sameWorkPair(TaskType.DIG_TILE, 0, 30);
        TaskManager.TaskWorkerPriority claim = pair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority gold = pair(TaskType.CARRY_GOLD_TO_TREASURY, 0, 5);

        assertEquals(nextDig, sorted(claim, gold, nextDig).getFirst());
    }

    @Test
    void dropAreaStillComesFirst() {
        TaskManager.TaskWorkerPriority nextClaim = sameWorkPair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority claimNextToDrop = pair(TaskType.CLAIM_TILE, 0, 103, 1);

        assertEquals(List.of(claimNextToDrop, nextClaim), sorted(nextClaim, claimNextToDrop));
    }

    @Test
    void claimingRoomsIsClaimingToo() {
        assertTrue(TaskManager.isClaiming(task(TaskType.CLAIM_TILE, null)));
        assertTrue(TaskManager.isClaiming(task(TaskType.CLAIM_ROOM, null)));
        assertFalse(TaskManager.isClaiming(task(TaskType.CLAIM_WALL, null)));
        assertTrue(TaskManager.isDigging(task(TaskType.DIG_TILE, null)));
        assertFalse(TaskManager.isDigging(task(TaskType.CLAIM_TILE, null)));
    }

    @Test
    void onlyFreeDiggingTheImpCanReachCountsAsMoreDigging() {

        // A digging imp with gold only digs on if there is a free dig task it can reach, otherwise it brings
        // its gold home first
        assertTrue(TaskManager.hasMoreDigging(List.of(digTask(false, true)), null));
        assertFalse(TaskManager.hasMoreDigging(List.of(digTask(true, true), digTask(false, false)), null));
        assertFalse(TaskManager.hasMoreDigging(List.of(task(TaskType.CLAIM_TILE, null)), null));
    }

    private static Task digTask(boolean full, boolean reachable) {
        return (Task) Proxy.newProxyInstance(Task.class.getClassLoader(), new Class<?>[]{Task.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getTaskType" ->
                TaskType.DIG_TILE;
            case "isFull" ->
                full;
            case "canAssign" ->
                reachable;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

    private static List<TaskManager.TaskWorkerPriority> sorted(TaskManager.TaskWorkerPriority... pairs) {
        List<TaskManager.TaskWorkerPriority> result = new ArrayList<>(List.of(pairs));
        result.sort(TaskManager::sortByTaskStatusAndDistance);
        return result;
    }

    private static TaskManager.TaskWorkerPriority pair(TaskType taskType, int assignees, int points) {
        return pair(taskType, assignees, points, Integer.MAX_VALUE);
    }

    private static TaskManager.TaskWorkerPriority pair(TaskType taskType, int assignees, int points, int dropRing) {
        return new TaskManager.TaskWorkerPriority(task(taskType, assignees, points, null), null, points, dropRing, false, null);
    }

    /**
     * A task of the same kind as the worker's work, digging on or claiming on
     */
    private static TaskManager.TaskWorkerPriority sameWorkPair(TaskType taskType, int assignees, int points) {
        return new TaskManager.TaskWorkerPriority(task(taskType, assignees, points, null), null, points, Integer.MAX_VALUE, true, null);
    }

    private static Task task(TaskType taskType, Point location) {
        return task(taskType, 0, 0, location);
    }

    private static Task task(TaskType taskType, int assignees, int points, Point location) {
        return (Task) Proxy.newProxyInstance(Task.class.getClassLoader(), new Class<?>[]{Task.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getTaskType" ->
                taskType;
            case "getAssigneeCount" ->
                assignees;
            case "getTaskLocation" ->
                location;
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
    }

}
