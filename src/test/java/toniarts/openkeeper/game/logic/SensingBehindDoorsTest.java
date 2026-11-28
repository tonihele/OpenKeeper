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
package toniarts.openkeeper.game.logic;

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Senses;
import toniarts.openkeeper.game.controller.IDoorsController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.door.IDoorController;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;

/**
 * Creatures see over barricades, but what stands behind a door that blocks
 * them is out of their reach, and a door can stop their line of sight
 */
class SensingBehindDoorsTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;

    private static final short WOODEN_DOOR = 1;
    private static final short BARRICADE = 6;

    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;

    /**
     * A corridor between walls, our creature at one end, the neutral creature
     * four tiles away at the other and the door in between
     */
    private static final Point CREATURE = new Point(1, 1);
    private static final Point DOOR = new Point(3, 1);
    private static final Point NEUTRAL_CREATURE = new Point(5, 1);

    private final EntityData entityData = new DefaultEntityData();
    private EntityId creature;
    private EntityId neutralCreature;
    private PositionSystem positionSystem;

    @BeforeEach
    void setUp() {
        creature = entity(CREATURE, PLAYER);
        entityData.setComponent(creature, new Senses(4, 4));
        neutralCreature = entity(NEUTRAL_CREATURE, Player.NEUTRAL_PLAYER_ID);
    }

    @AfterEach
    void tearDown() {
        if (positionSystem != null) {
            positionSystem.stop();
        }
        entityData.close();
    }

    @Test
    void creatureIsSensedOverAnEnemyBarricade() {
        door(ENEMY, BARRICADE, false);

        assertTrue(positionSystem().getSensedEntities(creature).contains(neutralCreature));
    }

    @Test
    void creatureBehindAnEnemyBarricadeIsBehindABlockingDoor() {
        door(ENEMY, BARRICADE, false);

        assertTrue(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void creatureBehindAnEnemyDoorIsBehindABlockingDoor() {
        door(ENEMY, WOODEN_DOOR, false);

        assertTrue(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void creatureBehindALockedDoorIsBehindABlockingDoor() {
        door(PLAYER, WOODEN_DOOR, true);

        assertTrue(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void aDoorThatBlocksTheSightStopsTheLineOfSight() {
        door(PLAYER, WOODEN_DOOR, false);

        assertFalse(positionSystem().hasLineOfSight(WorldUtils.pointToVector3f(CREATURE.x, CREATURE.y),
                WorldUtils.pointToVector3f(NEUTRAL_CREATURE.x, NEUTRAL_CREATURE.y), door -> true));
    }

    @Test
    void creatureBehindOurOwnDoorIsNotBehindABlockingDoor() {
        door(PLAYER, WOODEN_DOOR, false);

        assertFalse(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void creatureWithoutADoorInBetweenIsNotBehindABlockingDoor() {
        assertFalse(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void aDoorThatLetsTheSightThroughLeavesTheLineOfSightClear() {
        door(PLAYER, WOODEN_DOOR, false);

        assertTrue(positionSystem().hasLineOfSight(WorldUtils.pointToVector3f(CREATURE.x, CREATURE.y),
                WorldUtils.pointToVector3f(NEUTRAL_CREATURE.x, NEUTRAL_CREATURE.y), door -> false));
    }

    private EntityId entity(Point p, short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Position(0, WorldUtils.pointToVector3f(p.x, p.y)), new Owner(ownerId, ownerId));
        return entity;
    }

    private void door(short ownerId, short doorId, boolean locked) {
        EntityId door = entity(DOOR, ownerId);
        entityData.setComponent(door, new DoorComponent(doorId, locked, false));
    }

    private PositionSystem positionSystem() {
        FakeMapData mapData = new FakeMapData(7, 3);
        for (int x = 0; x < mapData.getWidth(); x++) {
            for (int y = 0; y < mapData.getHeight(); y++) {
                mapData.getTile(x, y).terrainId = y == 1 ? CLAIMED_PATH : REINFORCED_WALL;
            }
        }
        FakeMapInformation mapInformation = new FakeMapInformation(mapData, Map.of(
                CLAIMED_PATH, TerrainFixtures.terrain(CLAIMED_PATH, TerrainFixtures.flags(OWNABLE), 0, 0, 0, (short) 0),
                REINFORCED_WALL, TerrainFixtures.terrain(REINFORCED_WALL, TerrainFixtures.flags(SOLID, OWNABLE), 0, 0, 0, (short) 0)));
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" ->
                mapData;
            case "getTerrain" ->
                mapInformation.getTerrain((IMapTileInformation) args[0]);
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        IDoorsController doorsController = (IDoorsController) Proxy.newProxyInstance(IDoorsController.class.getClassLoader(),
                new Class<?>[]{IDoorsController.class}, (doors, createController, door) -> Proxy.newProxyInstance(
                IDoorController.class.getClassLoader(), new Class<?>[]{IDoorController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isBarricade" ->
                entityData.getComponent((EntityId) door[0], DoorComponent.class).doorId == BARRICADE;
            default ->
                throw new UnsupportedOperationException(method.getName());
        }));
        positionSystem = new PositionSystem(mapController, entityData, null, doorsController, null);
        return positionSystem;
    }

}
