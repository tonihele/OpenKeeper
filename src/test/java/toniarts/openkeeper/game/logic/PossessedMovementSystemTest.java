/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.logic;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import static org.junit.jupiter.api.Assertions.*;

class PossessedMovementSystemTest {
    @ParameterizedTest
    @ValueSource(floats = {.125f, .25f, .5f})
    void fullAuthoredWidthLimitsBothAxesAndDirections(float width) {
        for (boolean xAxis : new boolean[]{false, true}) {
            for (int direction : new int[]{-1, 1}) {
                for (boolean blocked : new boolean[]{false, true}) {
                    DoorMovementFixtures f = new DoorMovementFixtures();
                    f.creatureWidth = width;
                    f.door((short) 1, xAxis ? -FastMath.HALF_PI : 0, blocked, false, false);
                    float from = direction > 0 ? 3 : 5;
                    var creature = f.possessed(xAxis ? from : 4, xAxis ? 4 : from, 2,
                            xAxis ? new Vector2f(direction, 0) : new Vector2f(0, direction), .5f);
                    f.tick(1);
                    float expected = blocked ? (direction > 0 ? 3.5f - width : 4.5f + width)
                            : from + direction * .5f;
                    assertEquals(expected, xAxis ? f.position(creature).x : f.position(creature).z, 1e-6f);
                    assertEquals(.7f, f.data.getComponent(creature, Position.class).rotation);
                }
            }
        }
    }

    @Test
    void aBreakableEnemyDoorCostDoesNotPermitWalkingThroughItsTile() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        var door = f.door((short) 1, 0, false, false, false);
        f.data.setComponent(door, new Owner(DoorMovementFixtures.ENEMY, DoorMovementFixtures.ENEMY));
        var creature = f.possessed(4, 3, 1, new Vector2f(0, 1), 3);
        assertEquals(INavigable.DOOR_COST,
                f.creatureController(creature).getCost(f.map.getTile(4, 3), f.map.getTile(4, 4), f.mapController, f.positions));
        f.tick(1);
        assertEquals(3.25f, f.position(creature).z);
    }

    @Test
    void unlockingAndRemovalUseTheExistingTileAccessPolicy() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        var door = f.door((short) 1, 0, true, false, false);
        var creature = f.possessed(4, 3, 1, new Vector2f(0, 1), 1);
        f.tick(1);
        assertEquals(3.25f, f.position(creature).z);
        f.data.setComponent(door, new DoorComponent((short) 1, false, false));
        f.open(door, true);
        f.tick(1);
        assertEquals(4.25f, f.position(creature).z);
        f.data.removeEntity(door);
        f.tick(1);
        assertEquals(5.25f, f.position(creature).z);
    }

    @Test
    void largeStepsCannotSkipBlockedDoorsWallsOrMapBoundaries() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        var door = f.door((short) 3, -FastMath.HALF_PI, true, false, false);
        var creature = f.possessed(1, 4, 1, new Vector2f(1, 0), 20);
        f.tick(1);
        assertEquals(3.25f, f.position(creature).x);
        f.data.removeEntity(door);
        f.map.getTile(4, 4).terrainId = 1;
        f.tick(1);
        assertEquals(3.25f, f.position(creature).x);
        f.data.setComponent(creature, new Position(0, new Vector3f(8, 0, 8)));
        f.tick(1);
        assertEquals(9.25f, f.position(creature).x);
    }

    @Test
    void playerCanSlideAlongABlockedDoorAndRetreat() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        f.door((short) 1, 0, true, false, false);
        var creature = f.possessed(3.75f, 3, 1, new Vector2f(.25f, 1), 1);
        f.tick(1);
        assertEquals(4, f.position(creature).x);
        assertEquals(3.25f, f.position(creature).z);
        f.move(creature, new Vector2f(0, -1), 1);
        f.tick(1);
        assertEquals(2.25f, f.position(creature).z);
    }

    @Test
    void aClosingDoorOccupantCanLeaveButCannotReenterItsBlockedTile() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        var door = f.door((short) 1, 0, false, false, true);
        var creature = f.possessed(4, 4, 1, new Vector2f(0, 1), 1);
        f.data.setComponent(door, new DoorComponent((short) 1, true, false));
        f.open(door, false);
        f.tick(1);
        assertEquals(5, f.position(creature).z);
        f.move(creature, new Vector2f(0, -1), 1);
        f.tick(1);
        assertEquals(4.75f, f.position(creature).z);
    }

    @Test
    void sizeChangesRefreshClearanceWithoutScalingByVisualHeight() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        f.door((short) 1, 0, true, false, false);
        f.creatureWidth = .125f;
        var creature = f.possessed(4, 3, 3, new Vector2f(0, 1), 1);
        f.tick(1);
        assertEquals(3.375f, f.position(creature).z);
        f.creatureWidth = .5f;
        f.tick(1);
        assertEquals(3, f.position(creature).z);
    }

    @Test
    void adjacentWallsPermitSlidingThroughAnAccessibleDoorway() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        f.door((short) 1, 0, false, false, true);
        f.map.getTile(3, 3).terrainId = 1;
        f.map.getTile(3, 4).terrainId = 1;
        var creature = f.possessed(4, 3, 1, new Vector2f(-1, 1).normalizeLocal(), 1);
        for (int i = 0; i < 4; i++) f.tick(.25f);
        assertTrue(f.position(creature).x >= 3.75f);
        assertTrue(f.position(creature).z > 3.5f);
    }

    @Test
    void waterLavaAndRoomAccessAreRetained() {
        DoorMovementFixtures f = new DoorMovementFixtures();
        f.map.getTile(4, 4).terrainId = 2;
        var creature = f.possessed(4, 3, 1, new Vector2f(0, 1), 1);
        f.tick(1);
        assertEquals(3.25f, f.position(creature).z);
        f.canWalkOnWater = true;
        f.tick(1);
        assertTrue(f.position(creature).z > 4);
        f.data.setComponent(creature, new Position(0, new Vector3f(4, 0, 3)));
        f.map.getTile(4, 4).terrainId = 3;
        f.tick(1);
        assertEquals(3.25f, f.position(creature).z);
        f.canFly = true;
        f.tick(1);
        assertTrue(f.position(creature).z > 4);
        f.data.setComponent(creature, new Position(0, new Vector3f(4, 0, 3)));
        f.map.getTile(4, 4).terrainId = 4;
        f.roomAccessible = false;
        f.tick(1);
        assertEquals(3.25f, f.position(creature).z);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void barricadesRemainBlockedByTheTileRule(int type) {
        DoorMovementFixtures f = new DoorMovementFixtures();
        f.door((short) type, 0, true, false, false);
        var creature = f.possessed(4, 3, 1, new Vector2f(0, 1), 2);
        f.tick(1);
        assertEquals(3.25f, f.position(creature).z);
    }
}
