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
package toniarts.openkeeper.view;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstructionBlockingTilesTest {

    private static final Point TILE = new Point(3, 4);
    private static final Point OTHER_TILE = new Point(5, 4);

    private final DefaultEntityData entityData = new DefaultEntityData();
    private final EntitySet doors = entityData.getEntities(DoorComponent.class, Position.class);
    private final ConstructionBlockingTiles blockingTiles = new ConstructionBlockingTiles();

    @AfterEach
    void tearDown() {
        doors.release();
        entityData.close();
    }

    @Test
    void entitiesThereFromTheStartBlock() {
        createDoor(TILE);
        EntitySet existingDoors = entityData.getEntities(DoorComponent.class, Position.class);

        blockingTiles.addAll(existingDoors, entity -> true);

        assertTrue(blockingTiles.test(TILE));
        assertFalse(blockingTiles.test(OTHER_TILE));
        existingDoors.release();
    }

    @Test
    void addedEntityBlocksItsTile() {
        createDoor(TILE);

        applyChanges();

        assertTrue(blockingTiles.test(TILE));
    }

    @Test
    void entityThatDoesNotBlockLeavesTheTileFree() {
        createDoor(TILE);

        doors.applyChanges();
        blockingTiles.applyChanges(doors, entity -> false);

        assertFalse(blockingTiles.test(TILE));
    }

    @Test
    void movedEntityBlocksOnlyItsNewTile() {
        EntityId door = createDoor(TILE);
        applyChanges();

        entityData.setComponent(door, new Position(0, toVector(OTHER_TILE)));
        applyChanges();

        assertFalse(blockingTiles.test(TILE));
        assertTrue(blockingTiles.test(OTHER_TILE));
    }

    @Test
    void tileStaysBlockedUntilItsLastEntityIsGone() {
        EntityId door = createDoor(TILE);
        EntityId otherDoor = createDoor(TILE);
        applyChanges();

        entityData.removeEntity(door);
        applyChanges();
        assertTrue(blockingTiles.test(TILE));

        entityData.removeEntity(otherDoor);
        applyChanges();
        assertFalse(blockingTiles.test(TILE));
    }

    private void applyChanges() {
        doors.applyChanges();
        blockingTiles.applyChanges(doors, entity -> true);
    }

    private EntityId createDoor(Point tile) {
        EntityId door = entityData.createEntity();
        entityData.setComponents(door, new DoorComponent(), new Position(0, toVector(tile)));
        return door;
    }

    private static Vector3f toVector(Point tile) {
        return new Vector3f(tile.x, WorldUtils.FLOOR_HEIGHT, tile.y);
    }

}
