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
import toniarts.openkeeper.game.controller.IMapController;
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
 * Creatures sense through doors, but what stands behind a door that blocks
 * them is out of their reach
 */
class SensingBehindDoorsTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;

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
    void creatureIsSensedThroughAnEnemyDoor() {
        door(ENEMY, false);

        assertTrue(positionSystem().getSensedEntities(creature).contains(neutralCreature));
    }

    @Test
    void creatureBehindAnEnemyDoorIsBehindABlockingDoor() {
        door(ENEMY, false);

        assertTrue(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void creatureBehindALockedDoorIsBehindABlockingDoor() {
        door(PLAYER, true);

        assertTrue(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void creatureBehindOurOwnDoorIsNotBehindABlockingDoor() {
        door(PLAYER, false);

        assertFalse(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    @Test
    void creatureWithoutADoorInBetweenIsNotBehindABlockingDoor() {
        assertFalse(positionSystem().isBehindBlockingDoor(creature, neutralCreature));
    }

    private EntityId entity(Point p, short ownerId) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new Position(0, WorldUtils.pointToVector3f(p.x, p.y)), new Owner(ownerId, ownerId));
        return entity;
    }

    private void door(short ownerId, boolean locked) {
        EntityId door = entity(DOOR, ownerId);
        entityData.setComponent(door, new DoorComponent((short) 1, locked, false));
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
        positionSystem = new PositionSystem(mapController, entityData, null, null, null);
        return positionSystem;
    }

}
