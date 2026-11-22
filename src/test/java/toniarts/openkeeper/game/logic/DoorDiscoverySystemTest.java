/* Copyright (C) 2026 OpenKeeper. SPDX-License-Identifier: GPL-3.0-or-later */
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.*;

class DoorDiscoverySystemTest {
    private static final short OWNER = 3, ALLY = 4, ENEMY = 5, THIRD = 6, TYPE = 1;
    private final DefaultEntityData data = new DefaultEntityData();
    private final FakeMapData map = new FakeMapData(16, 16);
    private final Map<Short, Keeper> keepers = Map.of(OWNER, new Keeper(false, OWNER),
            ALLY, new Keeper(false, ALLY), ENEMY, new Keeper(false, ENEMY), THIRD, new Keeper(false, THIRD));
    private EntityId door;
    private PositionSystem positions;
    private DoorViewSystem opening;
    private DoorDiscoverySystem discovery;
    private IMapController mapController;
    private boolean wall;

    @BeforeEach
    void setUp() {
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> throw new AssertionError(method.getName());
        });
        Terrain floor = TerrainFixtures.terrain((short) 0, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        Terrain solid = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, (short) 0);
        mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> map;
            case "getTerrain" -> wall && ((toniarts.openkeeper.game.map.IMapTileInformation) args[0]).getX() == 7 ? solid : floor;
            default -> throw new AssertionError(method.getName());
        });
        door = data.createEntity();
        data.setComponents(door, new Position(0, new Vector3f(8, 0, 8)), new Owner(OWNER, OWNER),
                new Health(100, 100), new DoorComponent(TYPE, false, false, true),
                new DoorViewState(TYPE, false, false, true, true));
        positions = new PositionSystem(mapController, data, null, null, null, levelInfo);
        opening = new DoorViewSystem(data, positions, levelInfo);
        discovery = new DoorDiscoverySystem(data, positions, levelInfo);
        keepers.get(OWNER).createAlliance(ALLY);
        keepers.get(ALLY).createAlliance(OWNER);
    }

    @AfterEach
    void stop() {
        discovery.stop();
        opening.stop();
        positions.stop();
        data.close();
    }

    @Test
    void ownerKnowsButAllianceDoesNotGrantRouteKnowledgeOrBreaking() {
        assertNotNull(cost(OWNER, false));
        assertNull(cost(ALLY, true));
        assertNull(cost(ENEMY, true));
        assertFalse(positions.isDoorKnown(door, ALLY));
        assertTrue(positions.isDoorKnown(door, OWNER));
    }

    @Test
    void alliedPhysicalOpeningDoesNotRevealTheRoute() {
        creature(ALLY, 8, 8);
        opening.processTick(0);
        discovery.processTick(0);
        assertTrue(view().open);
        assertTrue(component().hidden);
        assertNull(cost(ALLY, true));
        assertEquals(INavigable.DEFAULT_COST, movementCost(ALLY));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void enemyAiOrPossessedObserverRevealsForEveryone(boolean possessed) {
        EntityId observer = creature(ENEMY, 9, 8);
        if (possessed) {
            data.removeComponent(observer, CreatureAi.class);
            data.setComponent(observer, new Possessed(0, 0));
        }
        discovery.processTick(0);
        assertFalse(component().hidden);
        assertFalse(view().hidden);
        assertTrue(positions.isDoorKnown(door, THIRD));
        assertEquals(INavigable.DEFAULT_COST, cost(ALLY, false));
        assertEquals(INavigable.DOOR_COST, cost(ENEMY, true));
        assertNull(cost(ENEMY, false));
    }

    @Test
    void discoveryPersistsAcrossClosingAndCapture() {
        EntityId observer = creature(ENEMY, 9, 8);
        discovery.processTick(0);
        data.removeEntity(observer);
        positions.processTick(0);
        opening.processTick(0);
        assertFalse(view().open);
        assertFalse(view().hidden);
        data.setComponent(door, new Owner(ENEMY, ENEMY));
        discovery.processTick(0);
        assertTrue(positions.isDoorKnown(door, THIRD));
        assertEquals(INavigable.DOOR_COST, cost(OWNER, true));
    }

    @Test
    void captureOfAnUndiscoveredDoorChangesWhoKnowsWithoutRevealing() {
        data.setComponent(door, new Owner(ENEMY, ENEMY));
        assertTrue(positions.isDoorKnown(door, ENEMY));
        assertFalse(positions.isDoorKnown(door, OWNER));
        assertTrue(component().hidden);
    }

    @Test
    void closedDoorAndBlueprintCannotBeDiscovered() {
        creature(ENEMY, 9, 8);
        data.setComponent(door, new DoorViewState(TYPE, false, false, false, true));
        discovery.processTick(0);
        assertTrue(component().hidden);
        data.setComponents(door, new DoorComponent(TYPE, false, true, true),
                new DoorViewState(TYPE, false, true, true, true));
        discovery.processTick(0);
        assertTrue(component().hidden);
        assertEquals(INavigable.DEFAULT_COST, cost(ENEMY, false));
    }

    @Test
    void ownerAndAllyDoNotDiscoverButChangingTheirDiplomacyCan() {
        creature(OWNER, 8, 8);
        creature(ALLY, 9, 8);
        discovery.processTick(0);
        assertTrue(component().hidden);
        keepers.get(ALLY).breakAlliance(OWNER);
        discovery.processTick(0);
        assertFalse(component().hidden);
    }

    @Test
    void lineOfSightUsesTerrainAndDoesNotUseHearingOrExploration() {
        creature(ENEMY, 6, 8);
        wall = true;
        discovery.processTick(0);
        assertTrue(component().hidden);
        wall = false;
        discovery.processTick(0);
        assertFalse(component().hidden);
    }

    @Test
    void exactFourTileBoundaryIsExcludedAndHeightDoesNotMatter() {
        EntityId observer = creature(ENEMY, 12, 8);
        discovery.processTick(0);
        assertTrue(component().hidden);
        data.setComponent(observer, new Position(0, new Vector3f(12 - 1f / 4096, 100, 8)));
        positions.processTick(0);
        discovery.processTick(0);
        assertFalse(component().hidden);
        assertFalse(DoorDiscoverySystem.isInRange(new Vector3f(11, 0, 11), new Vector3f(8, 0, 8)));
    }

    @ParameterizedTest
    @EnumSource(value = CreatureState.class, names = {"FIGHT", "FLEE", "BACK_OFF", "CELEBRATING", "STUNNED", "SLEEPING",
        "UNCONSCIOUS", "IMPRISONED", "TORTURED", "CHICKEN", "ENTERING_DUNGEON", "DEAD"})
    void unavailableOrBusyObserversCannotDiscover(CreatureState state) {
        EntityId observer = creature(ENEMY, 9, 8);
        data.setComponent(observer, new CreatureAi(0, state, (short) 1));
        discovery.processTick(0);
        assertTrue(component().hidden);
    }

    @Test
    void deadUnconsciousAndHeldCreaturesCannotRevealEvenWithoutAi() {
        EntityId observer = creature(ENEMY, 9, 8);
        data.removeComponent(observer, CreatureAi.class);
        data.setComponents(observer, new Possessed(0, 0), new Health(0, 100));
        discovery.processTick(0);
        assertTrue(component().hidden);
        data.setComponents(observer, new Health(100, 100), new Unconscious());
        discovery.processTick(0);
        assertTrue(component().hidden);
        data.removeComponent(observer, Unconscious.class);
        data.setComponent(observer, new InHand());
        discovery.processTick(0);
        assertTrue(component().hidden);
    }

    @Test
    void removalAndReplacementDoNotCarryDiscoveryToANewDoor() {
        creature(ENEMY, 9, 8);
        discovery.processTick(0);
        data.removeEntity(door);
        positions.processTick(0);
        discovery.processTick(0);
        assertEquals(INavigable.DEFAULT_COST, cost(ALLY, false));
        door = data.createEntity();
        data.setComponents(door, new Position(0, new Vector3f(8, 0, 8)), new Owner(OWNER, OWNER),
                new DoorComponent(TYPE, false, false, true), new DoorViewState(TYPE, false, false, false, true));
        positions.processTick(0);
        discovery.processTick(0);
        assertNull(cost(ALLY, true));
    }

    @Test
    void peacefulCreaturesDoNotDiscoverUntilTheyAreAllowedToFight() {
        EntityId observer = creature(ENEMY, 9, 8);
        data.setComponent(observer, new Peaceful());
        discovery.processTick(0);
        assertTrue(component().hidden);
        data.removeComponent(observer, Peaceful.class);
        discovery.processTick(0);
        assertFalse(component().hidden);
    }

    @Test
    void manualMovementUsesPhysicalAccessBeforeAndAfterDiscovery() {
        assertEquals(INavigable.DEFAULT_COST, movementCost(ENEMY)); // already open
        data.setComponent(door, new DoorViewState(TYPE, false, false, false, true));
        assertNull(movementCost(ENEMY));
        assertEquals(INavigable.DEFAULT_COST, movementCost(ALLY)); // permitted to open
        creature(ENEMY, 9, 8);
        data.setComponent(door, new DoorViewState(TYPE, false, false, true, true));
        discovery.processTick(0);
        assertEquals(INavigable.DEFAULT_COST, movementCost(ENEMY));
        data.setComponent(door, new DoorViewState(TYPE, false, false, false, false));
        assertNull(movementCost(ENEMY));
    }

    @Test
    void hiddenDoorCannotBeSelectedForAttackAndDiscoveryRestoresExistingRules() {
        assertFalse(toniarts.openkeeper.game.controller.creature.DoorAttackTarget.canBreak(data, door,
                toniarts.openkeeper.tools.convert.map.Creature.FightStyle.BLITZER, true));
        creature(ENEMY, 9, 8);
        discovery.processTick(0);
        assertTrue(toniarts.openkeeper.game.controller.creature.DoorAttackTarget.canBreak(data, door,
                toniarts.openkeeper.tools.convert.map.Creature.FightStyle.BLITZER, true));
    }

    @ParameterizedTest(name = "{0}: hidden={1}, open={2}, locked={3}")
    @CsvSource({
        "ALLY,true,false,false", "ALLY,true,true,false", "ALLY,true,false,true", "ALLY,true,true,true",
        "ALLY,false,false,false", "ALLY,false,true,false", "ALLY,false,false,true", "ALLY,false,true,true",
        "ENEMY,true,false,false", "ENEMY,true,true,false", "ENEMY,true,false,true", "ENEMY,true,true,true",
        "ENEMY,false,false,false", "ENEMY,false,true,false", "ENEMY,false,false,true", "ENEMY,false,true,true"
    })
    void alliedAndEnemyDoorMatrix(String relation, boolean hidden, boolean open, boolean locked) {
        short viewer = relation.equals("ALLY") ? ALLY : ENEMY;
        boolean allied = viewer == ALLY;
        data.setComponents(door, new DoorComponent(TYPE, locked, false, hidden),
                new DoorViewState(TYPE, locked, false, open, hidden));
        assertEquals(!hidden, positions.isDoorKnown(door, viewer));
        assertEquals(!hidden, view().isShownTo(OWNER, viewer));
        assertEquals(!hidden && (!allied || !locked), cost(viewer, !allied) != null,
                "AI must know the door before opening or attacking it");
        assertEquals(!hidden && allied && !locked, cost(viewer, false) != null);
        assertEquals(open || allied && !locked, movementCost(viewer) != null,
                "Manual passage follows physical state independently of discovery");
    }

    @ParameterizedTest(name = "{0} observer, locked={1}")
    @CsvSource({"ALLY,false", "ALLY,true", "ENEMY,false", "ENEMY,true"})
    void openingAndDiscoveryWithAlliedAndEnemyObservers(String relation, boolean locked) {
        short observerOwner = relation.equals("ALLY") ? ALLY : ENEMY;
        data.setComponents(door, new DoorComponent(TYPE, locked, false, true),
                new DoorViewState(TYPE, locked, false, false, true));
        creature(observerOwner, 8, 8);
        opening.processTick(0);
        discovery.processTick(0);
        assertEquals(observerOwner == ALLY && !locked, view().open);
        assertTrue(component().hidden, "An ally cannot reveal it; an enemy cannot open it");
        if (!locked) {
            creature(OWNER, 8, 8);
            opening.processTick(0);
            discovery.processTick(0);
            assertTrue(view().open);
            assertEquals(observerOwner == ALLY, component().hidden,
                    "Only an enemy witnessing the owner's open door reveals it");
        }
    }

    private EntityId creature(short owner, float x, float z) {
        EntityId id = data.createEntity();
        data.setComponents(id, new Position(0, new Vector3f(x, 0, z)), new Owner(owner, owner),
                new CreatureComponent(), new Health(100, 100), new CreatureAi(0, CreatureState.IDLE, (short) 1));
        positions.processTick(0);
        return id;
    }
    private DoorComponent component() { return data.getComponent(door, DoorComponent.class); }
    private DoorViewState view() { return data.getComponent(door, DoorViewState.class); }
    private Float cost(short owner, boolean breaks) {
        return navigable(owner, breaks).getCost(map.getTile(7, 8), map.getTile(8, 8), mapController, positions);
    }
    private Float movementCost(short owner) {
        return navigable(owner, false).getMovementCost(map.getTile(7, 8), map.getTile(8, 8), mapController, positions);
    }
    private INavigable navigable(short owner, boolean breaks) {
        return new INavigable() {
            public short getOwnerId() { return owner; }
            public boolean canFly() { return false; }
            public boolean canWalkOnWater() { return false; }
            public boolean canWalkOnLava() { return false; }
            public boolean canMoveDiagonally() { return false; }
            public boolean canBreakDoor(EntityId id) { return breaks; }
        };
    }
}
