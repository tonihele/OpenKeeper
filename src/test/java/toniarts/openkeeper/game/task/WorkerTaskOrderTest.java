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

        assertEquals(wall, sorted(wall, repair, dig, gold).getLast());
    }

    @Test
    void damagedWallsAreRepairedBeforeTheUsualWork() {

        // The repair is further away and worth less points than the rest, it is still done first
        TaskManager.TaskWorkerPriority repair = pair(TaskType.REPAIR_WALL, 0, 140);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 12);
        TaskManager.TaskWorkerPriority claim = pair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority gold = pair(TaskType.CARRY_GOLD_TO_TREASURY, 0, 105);
        TaskManager.TaskWorkerPriority wall = pair(TaskType.CLAIM_WALL, 0, 177);

        assertEquals(List.of(repair, dig, claim, gold, wall), sorted(wall, gold, claim, dig, repair));
    }

    @Test
    void dropAndStartedWorkComeBeforeRepairs() {
        TaskManager.TaskWorkerPriority repair = pair(TaskType.REPAIR_WALL, 0, 101);
        TaskManager.TaskWorkerPriority digNextToDrop = pair(TaskType.DIG_TILE, 0, 11, true, false);
        TaskManager.TaskWorkerPriority claimInDropArea = pair(TaskType.CLAIM_TILE, 0, 102, true);
        TaskManager.TaskWorkerPriority digOn = carriesOnPair(TaskType.DIG_TILE, 0, 15);
        TaskManager.TaskWorkerPriority claimOn = carriesOnPair(TaskType.CLAIM_TILE, 0, 101);

        assertEquals(repair, sorted(repair, digNextToDrop, claimInDropArea, digOn, claimOn).getLast());
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
    void droppedImpClaimsAroundTheDropFirst() {

        // Dropped 3 tiles from unclaimed floor: it claims, although digging is closer and worth more
        TaskManager.TaskWorkerPriority claimInDropArea = pair(TaskType.CLAIM_TILE, 0, 103, true);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 11);
        TaskManager.TaskWorkerPriority freeClaimElsewhere = pair(TaskType.CLAIM_TILE, 0, 101);

        assertEquals(claimInDropArea, sorted(dig, freeClaimElsewhere, claimInDropArea).getFirst());
    }

    @Test
    void dropAreaIsTheFloorWithinThreeTilesAsTheCrowFlies() {
        Point drop = new Point(10, 10);

        assertTrue(TaskManager.isClaimInDropArea(task(TaskType.CLAIM_TILE, new Point(13, 10)), drop));
        assertTrue(TaskManager.isClaimInDropArea(task(TaskType.CLAIM_TILE, new Point(8, 12)), drop));
        assertTrue(TaskManager.isClaimInDropArea(task(TaskType.CLAIM_TILE, new Point(10, 10)), drop));
        assertFalse(TaskManager.isClaimInDropArea(task(TaskType.CLAIM_TILE, new Point(13, 11)), drop));
        assertFalse(TaskManager.isClaimInDropArea(task(TaskType.CLAIM_TILE, new Point(10, 14)), drop));
    }

    @Test
    void onlyFloorCountsInTheDropArea() {
        Point drop = new Point(10, 10);

        assertFalse(TaskManager.isClaimInDropArea(task(TaskType.CLAIM_WALL, new Point(11, 10)), drop));
        assertFalse(TaskManager.isClaimInDropArea(task(TaskType.DIG_TILE, new Point(11, 10)), drop));
        assertFalse(TaskManager.isClaimInDropArea(task(TaskType.REPAIR_WALL, new Point(11, 10)), drop));
    }

    @Test
    void droppedNextToDiggingDigsFirst() {

        // Dropped right next to a tile to dig, with floor to claim around it: it digs first
        TaskManager.TaskWorkerPriority digNextToDrop = pair(TaskType.DIG_TILE, 0, 11, true, false);
        TaskManager.TaskWorkerPriority claimInDropArea = pair(TaskType.CLAIM_TILE, 0, 101, false, true);

        assertEquals(List.of(digNextToDrop, claimInDropArea), sorted(claimInDropArea, digNextToDrop));
    }

    @Test
    void digNextToDropIsOneOfTheEightTilesAround() {
        Point drop = new Point(10, 10);

        assertTrue(TaskManager.isDigNextToDrop(task(TaskType.DIG_TILE, new Point(11, 10)), drop));
        assertTrue(TaskManager.isDigNextToDrop(task(TaskType.DIG_TILE, new Point(9, 11)), drop));
        assertFalse(TaskManager.isDigNextToDrop(task(TaskType.DIG_TILE, new Point(12, 10)), drop));
        assertFalse(TaskManager.isDigNextToDrop(task(TaskType.CLAIM_TILE, new Point(11, 10)), drop));
    }

    @Test
    void claimingImpCarriesOnNextToIt() {

        // The imp just claimed a tile, the next floor is right next to it: it carries on, although
        // digging is close and there is free floor elsewhere
        TaskManager.TaskWorkerPriority nextClaim = carriesOnPair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority dig = pair(TaskType.DIG_TILE, 0, 12);
        TaskManager.TaskWorkerPriority freeClaimElsewhere = pair(TaskType.CLAIM_TILE, 0, 100);

        assertEquals(nextClaim, sorted(dig, freeClaimElsewhere, nextClaim).getFirst());
    }

    @Test
    void dropAreaStillComesFirst() {
        TaskManager.TaskWorkerPriority nextClaim = carriesOnPair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority claimInDropArea = pair(TaskType.CLAIM_TILE, 0, 103, true);

        assertEquals(List.of(claimInDropArea, nextClaim), sorted(nextClaim, claimInDropArea));
    }

    @Test
    void diggingImpCarriesOnDigging() {

        // The imp just dug a tile, another marked tile is 8 tiles away: it digs on, although there is
        // floor to claim right next to it
        TaskManager.TaskWorkerPriority nextDig = carriesOnPair(TaskType.DIG_TILE, 0, 18);
        TaskManager.TaskWorkerPriority claim = pair(TaskType.CLAIM_TILE, 0, 101);
        TaskManager.TaskWorkerPriority gold = pair(TaskType.CARRY_GOLD_TO_TREASURY, 0, 5);

        assertEquals(nextDig, sorted(claim, gold, nextDig).getFirst());
    }

    @Test
    void diggingImpDoesNotStopToClaimTheDugOutFloor() {

        // A dropped imp started digging next to the drop. The dug out floor in the drop area becomes
        // claimable, but it digs on
        TaskManager.TaskWorkerPriority nextDig = carriesOnPair(TaskType.DIG_TILE, 0, 13);
        TaskManager.TaskWorkerPriority claimInDropArea = pair(TaskType.CLAIM_TILE, 0, 101, true);

        assertEquals(List.of(nextDig, claimInDropArea), sorted(claimInDropArea, nextDig));
    }

    @Test
    void nextDigIsWithinTenTilesAsTheCrowFlies() {
        Point imp = new Point(20, 20);

        assertTrue(TaskManager.isNextDig(task(TaskType.DIG_TILE, new Point(30, 20)), imp));
        assertTrue(TaskManager.isNextDig(task(TaskType.DIG_TILE, new Point(26, 28)), imp));
        assertFalse(TaskManager.isNextDig(task(TaskType.DIG_TILE, new Point(27, 28)), imp));
        assertFalse(TaskManager.isNextDig(task(TaskType.DIG_TILE, new Point(31, 20)), imp));
        assertFalse(TaskManager.isNextDig(task(TaskType.CLAIM_TILE, new Point(21, 20)), imp));
    }

    @Test
    void nextClaimIsOneOfTheEightTilesAround() {
        Point imp = new Point(10, 10);

        assertTrue(TaskManager.isNextClaim(task(TaskType.CLAIM_TILE, new Point(11, 10)), imp));
        assertTrue(TaskManager.isNextClaim(task(TaskType.CLAIM_TILE, new Point(9, 9)), imp));
        assertFalse(TaskManager.isNextClaim(task(TaskType.CLAIM_TILE, new Point(12, 10)), imp));
        assertFalse(TaskManager.isNextClaim(task(TaskType.CLAIM_WALL, new Point(11, 10)), imp));
        assertFalse(TaskManager.isNextClaim(task(TaskType.DIG_TILE, new Point(11, 10)), imp));
    }

    private static List<TaskManager.TaskWorkerPriority> sorted(TaskManager.TaskWorkerPriority... pairs) {
        List<TaskManager.TaskWorkerPriority> result = new ArrayList<>(List.of(pairs));
        result.sort(TaskManager::sortByTaskStatusAndDistance);
        return result;
    }

    private static TaskManager.TaskWorkerPriority pair(TaskType taskType, int assignees, int points) {
        return pair(taskType, assignees, points, false);
    }

    private static TaskManager.TaskWorkerPriority pair(TaskType taskType, int assignees, int points, boolean inDropArea) {
        return pair(taskType, assignees, points, false, inDropArea);
    }

    private static TaskManager.TaskWorkerPriority pair(TaskType taskType, int assignees, int points, boolean digNextToDrop, boolean inDropArea) {
        return pair(taskType, assignees, points, digNextToDrop, false, inDropArea, false);
    }

    /**
     * A task that carries on with the worker's work: digging on, or claiming
     * the floor next to it
     */
    private static TaskManager.TaskWorkerPriority carriesOnPair(TaskType taskType, int assignees, int points) {
        boolean digging = taskType == TaskType.DIG_TILE;
        return pair(taskType, assignees, points, false, digging, false, !digging);
    }

    private static TaskManager.TaskWorkerPriority pair(TaskType taskType, int assignees, int points, boolean digNextToDrop, boolean digsOn,
            boolean inDropArea, boolean claimsOn) {
        return new TaskManager.TaskWorkerPriority(task(taskType, assignees, points, null), null, points, digNextToDrop, digsOn, inDropArea,
                claimsOn, null);
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
