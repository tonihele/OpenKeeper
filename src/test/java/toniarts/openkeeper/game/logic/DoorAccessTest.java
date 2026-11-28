/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoorAccessTest {

    private static final short OWNER = 3, ALLY = 4, ENEMY = 5, DOOR_TYPE = 1;
    private final DefaultEntityData data = new DefaultEntityData();
    private final FakeMapData map = new FakeMapData(8, 8);
    private final Map<Short, Keeper> keepers = Map.of(OWNER, new Keeper(false, OWNER),
            ALLY, new Keeper(false, ALLY), ENEMY, new Keeper(false, ENEMY));
    private EntityId door;
    private PositionSystem positions;
    private DoorViewSystem views;
    private IMapController mapController;

    @BeforeEach
    void setUp() {
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> throw new AssertionError(method.getName());
        });
        Terrain floor = TerrainFixtures.terrain((short) 0, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> map;
            case "getTerrain" -> floor;
            default -> throw new AssertionError(method.getName());
        });
        door = data.createEntity();
        data.setComponents(door, new Position(0, new Vector3f(4, 0, 4)), new Owner(OWNER, OWNER),
                new DoorComponent(DOOR_TYPE, false, false), new DoorViewState(DOOR_TYPE, false, false, false));
        positions = new PositionSystem(mapController, data, null, null, null, levelInfo);
        views = new DoorViewSystem(data, positions, levelInfo);
    }

    @AfterEach
    void releaseSets() {
        views.stop();
        positions.stop();
        data.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void alliedAiAndPossessedCreaturesRequestOpening(boolean possessed) {
        ally();
        EntityId creature = creature(ALLY, 4, 4, possessed);
        views.processTick(0);
        assertTrue(isOpen());
        data.removeEntity(creature);
        positions.processTick(0);
        views.processTick(0);
        assertFalse(isOpen());
    }

    @Test
    void anEnemyDoesNotRequestOpening() {
        creature(ENEMY, 4, 4, false);
        views.processTick(0);
        assertFalse(isOpen());
    }

    @Test
    void diplomacyChangesUpdateAccessWithoutMovingTheDoor() {
        assertEquals(door, positions.getBlockingDoor(4, 4, ALLY));
        ally();
        assertNull(positions.getBlockingDoor(4, 4, ALLY));
        keepers.get(OWNER).breakAlliance(ALLY);
        assertEquals(door, positions.getBlockingDoor(4, 4, ALLY));
    }

    @Test
    void ownershipChangesRecheckOpeningForAStationaryCreature() {
        creature(OWNER, 4, 4, false);
        views.processTick(0);
        assertTrue(isOpen());
        data.setComponent(door, new Owner(ENEMY, ENEMY));
        views.processTick(0);
        assertFalse(isOpen());
        assertEquals(door, positions.getBlockingDoor(4, 4, OWNER));
        keepers.get(ENEMY).createAlliance(OWNER);
        views.processTick(0);
        assertTrue(isOpen());
    }

    @Test
    void lockingBlocksAlliesAndUnlockingRestoresAccess() {
        ally();
        creature(ALLY, 4, 4, true);
        views.processTick(0);
        assertTrue(isOpen());
        // The world controller closes the logical state when locking a door.
        data.setComponents(door, new DoorComponent(DOOR_TYPE, true, false),
                new DoorViewState(DOOR_TYPE, true, false, false));
        views.processTick(0);
        assertFalse(isOpen());
        assertEquals(door, positions.getBlockingDoor(4, 4, ALLY));
        data.setComponents(door, new DoorComponent(DOOR_TYPE, false, false),
                new DoorViewState(DOOR_TYPE, false, false, false));
        assertNull(positions.getBlockingDoor(4, 4, ALLY));
        views.processTick(0);
        assertTrue(isOpen());
    }

    @Test
    void blueprintBuildAndRemovalRecheckAccessWithoutPositionChanges() {
        data.setComponents(door, new DoorComponent(DOOR_TYPE, false, true),
                new DoorViewState(DOOR_TYPE, false, true, false));
        assertNull(positions.getBlockingDoor(4, 4, ENEMY));
        data.setComponents(door, new DoorComponent(DOOR_TYPE, false, false),
                new DoorViewState(DOOR_TYPE, false, false, false));
        assertEquals(door, positions.getBlockingDoor(4, 4, ENEMY));
        data.removeEntity(door);
        assertNull(positions.getBlockingDoor(4, 4, ENEMY));
    }

    @Test
    void alliedRoutesCostNormalMovementAndEnemyRoutesStillRequireBreaking() {
        ally();
        assertEquals(INavigable.DEFAULT_COST, cost(ALLY, false));
        assertNull(cost(ENEMY, false));
        assertEquals(INavigable.DOOR_COST, cost(ENEMY, true));
        data.setComponent(door, new DoorComponent(DOOR_TYPE, true, false));
        assertNull(cost(ALLY, false));
    }

    @Test
    void breakingAnAllianceStopsAnOldAiRouteAtTheDoor() {
        ally();
        EntityId creature = creature(ALLY, 3, 4, false);
        keepers.get(OWNER).breakAlliance(ALLY);
        data.setComponent(creature, new Position(0, new Vector3f(4, 0, 4)));
        positions.processTick(0);
        assertNull(data.getComponent(creature, Navigation.class));
        assertEquals(3, data.getComponent(creature, Position.class).position.x);
    }

    private EntityId creature(short owner, float x, float z, boolean possessed) {
        EntityId id = data.createEntity();
        data.setComponents(id, new Position(0, new Vector3f(x, 0, z)), new Owner(owner, owner), new CreatureComponent());
        if (possessed) {
            data.setComponent(id, new Possessed(0, 0));
        } else {
            data.setComponent(id, new Navigation(new Point(4, 6), null, List.of()));
        }
        positions.processTick(0);
        return id;
    }

    private boolean isOpen() {
        return data.getComponent(door, DoorViewState.class).open;
    }

    private void ally() {
        keepers.get(OWNER).createAlliance(ALLY);
        keepers.get(ALLY).createAlliance(OWNER);
    }

    private Float cost(short owner, boolean breaksDoors) {
        INavigable navigable = new INavigable() {
            @Override public short getOwnerId() { return owner; }
            @Override public boolean canFly() { return false; }
            @Override public boolean canWalkOnWater() { return false; }
            @Override public boolean canWalkOnLava() { return false; }
            @Override public boolean canMoveDiagonally() { return false; }
            @Override public boolean canBreakDoor(EntityId id) { return breaksDoors; }
        };
        return navigable.getCost(map.getTile(3, 4), map.getTile(4, 4), mapController, positions);
    }
}
