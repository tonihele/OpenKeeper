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
package toniarts.openkeeper.game.controller.room.storage;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Stored;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class RoomCrateControlTest {

    private static final float EPSILON = 1e-5f;
    private static final Point FLOOR = new Point(3, 4);
    private static final Point OTHER_FLOOR = new Point(3, 5);
    private static final Point ANVIL = new Point(4, 4);

    /**
     * The two crates at the bottom of a stack stand this far to the sides of
     * the tile's middle, with a small gap between them
     */
    private static final float BOTTOM_OFFSET = (RoomCrateControl.DEFAULT_CRATE_SIZE + RoomCrateControl.CRATE_GAP) / 2;

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId anvil = entityData.createEntity();

    @Test
    void crateIsStoredOnTheFloorNotOnTheFurniture() {
        RoomCrateControl control = createControl(FLOOR);
        EntityId crate = addCrate(ANVIL);

        assertNull(control.addItem(crate, ANVIL));

        assertEquals(FLOOR, tileOf(crate));
        assertNotNull(entityData.getComponent(crate, Stored.class));
        assertEquals(1, control.getCurrentCapacity());
    }

    @Test
    void singleCrateStandsInTheMiddleOfTheTile() {
        RoomCrateControl control = createControl(FLOOR);
        List<EntityId> crates = addCrates(control, 1);

        assertPosition(0, 0, crates.get(0));
    }

    @Test
    void twoCratesStandSideBySideAndTheThirdOnTop() {
        RoomCrateControl control = createControl(FLOOR);
        List<EntityId> crates = addCrates(control, 3);

        assertPosition(-BOTTOM_OFFSET, 0, crates.get(0));
        assertPosition(BOTTOM_OFFSET, 0, crates.get(1));
        assertPosition(0, RoomCrateControl.DEFAULT_CRATE_SIZE, crates.get(2));
    }

    @Test
    void startedStackIsFilledBeforeANewOne() {
        RoomCrateControl control = createControl(FLOOR, OTHER_FLOOR);
        List<EntityId> crates = addCrates(control, 4);

        Point firstTile = tileOf(crates.get(0));
        assertEquals(firstTile, tileOf(crates.get(1)));
        assertEquals(firstTile, tileOf(crates.get(2)));
        assertEquals(firstTile.equals(FLOOR) ? OTHER_FLOOR : FLOOR, tileOf(crates.get(3)));
    }

    @Test
    void fullWorkshopTakesNoMoreCrates() {
        RoomCrateControl control = createControl(FLOOR);
        addCrates(control, 3);
        EntityId fourthCrate = addCrate(FLOOR);

        assertSame(fourthCrate, control.addItem(fourthCrate, null));
        assertEquals(3, control.getCurrentCapacity());
    }

    @Test
    void topCrateComesDownWhenOneIsTaken() {
        RoomCrateControl control = createControl(FLOOR);
        List<EntityId> crates = addCrates(control, 3);

        control.removeItem(crates.get(0));

        assertPosition(-BOTTOM_OFFSET, 0, crates.get(1));
        assertPosition(BOTTOM_OFFSET, 0, crates.get(2));
        assertEquals(2, control.getCurrentCapacity());
    }

    @Test
    void destroyingTheWorkshopLeavesTheStacksStanding() {
        RoomCrateControl control = createControl(FLOOR);
        List<EntityId> crates = addCrates(control, 3);

        // Selling or destroying a workshop with crates in it used to crash here
        control.destroy();

        for (EntityId crate : crates) {
            assertNull(entityData.getComponent(crate, Stored.class));
        }
        assertPosition(-BOTTOM_OFFSET, 0, crates.get(0));
        assertPosition(BOTTOM_OFFSET, 0, crates.get(1));
        assertPosition(0, RoomCrateControl.DEFAULT_CRATE_SIZE, crates.get(2));
    }

    /**
     * Checks where a crate stands on {@link #FLOOR}
     *
     * @param x how far it is to the side of the tile's middle
     * @param height how far it is above the floor
     * @param crate the crate
     */
    private void assertPosition(float x, float height, EntityId crate) {
        Vector3f middle = WorldUtils.pointToVector3f(FLOOR);
        Vector3f position = entityData.getComponent(crate, Position.class).position;
        assertEquals(middle.x + x, position.x, EPSILON);
        assertEquals(WorldUtils.FLOOR_HEIGHT + height, position.y, EPSILON);
        assertEquals(middle.z, position.z, EPSILON);
    }

    private List<EntityId> addCrates(RoomCrateControl control, int count) {
        List<EntityId> crates = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            EntityId crate = addCrate(ANVIL);
            assertNull(control.addItem(crate, null));
            crates.add(crate);
        }
        return crates;
    }

    private EntityId addCrate(Point tile) {
        EntityId crate = entityData.createEntity();
        entityData.setComponent(crate, new Position(0, new Vector3f(tile.x, WorldUtils.FLOOR_HEIGHT, tile.y)));
        return crate;
    }

    private Point tileOf(EntityId crate) {
        return WorldUtils.vectorToPoint(entityData.getComponent(crate, Position.class).position);
    }

    private RoomCrateControl createControl(Point... floor) {
        entityData.setComponent(anvil, new Position(0, new Vector3f(ANVIL.x, WorldUtils.FLOOR_HEIGHT, ANVIL.y)));
        EntityId roomEntity = entityData.createEntity();
        RoomInstance roomInstance = new RoomInstance(new Room());
        for (Point tile : floor) {
            roomInstance.addCoordinate(tile);
        }
        roomInstance.addCoordinate(ANVIL);
        IRoomController room = (IRoomController) Proxy.newProxyInstance(
                IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getEntityId" -> roomEntity;
                    case "getOwnerId" -> (short) 1;
                    case "getRoomInstance" -> roomInstance;
                    case "isTileAccessible" -> true;
                    case "getFloorFurniture" -> Set.of(anvil);
                    case "getFloorFurnitureCount" -> 1;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new RoomCrateControl(null, room, entityData, () -> 0) {
            @Override
            protected int getNumberOfAccessibleTiles() {
                return getCoordinates().size();
            }
        };
    }

}
