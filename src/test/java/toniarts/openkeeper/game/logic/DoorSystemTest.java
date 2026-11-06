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

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;

class DoorSystemTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;
    private static final short ALLY = Player.KEEPER3_ID;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final Point TILE = new Point(2, 2);

    private final EntityData entityData = new DefaultEntityData();
    private final Door woodenDoor = DoorFixtures.door((short) 1, 250);
    private final Door barricade = DoorFixtures.door((short) 6, 400, Door.DoorFlag.IS_BARRICADE);
    private final Door secretDoor = DoorFixtures.door((short) 5, 3000, Door.DoorFlag.IS_SECRET);
    private FakeMapData mapData;
    private DoorSystem doorSystem;

    @BeforeEach
    void setUp() {
        mapData = new FakeMapData(9, 9);
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 9; y++) {
                setTile(new Point(x, y), CLAIMED_PATH, PLAYER);
            }
        }
        FakeMapInformation mapInformation = new FakeMapInformation(mapData, Map.of(
                CLAIMED_PATH, terrain(CLAIMED_PATH, OWNABLE),
                REINFORCED_WALL, terrain(REINFORCED_WALL, SOLID, OWNABLE)));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getDoorById" -> door(((Number) args[0]).shortValue());
            default -> throw new UnsupportedOperationException(method.getName());
        });
        Keeper player = new Keeper(false, PLAYER);
        player.createAlliance(ALLY);
        Map<Short, Keeper> keepers = Map.of(PLAYER, player, ENEMY, new Keeper(false, ENEMY), ALLY, new Keeper(false, ALLY));
        ILevelInfo levelInfo = (ILevelInfo) Proxy.newProxyInstance(ILevelInfo.class.getClassLoader(),
                new Class<?>[]{ILevelInfo.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        doorSystem = new DoorSystem(entityData, kwdFile, mapInformation, levelInfo);
    }

    @AfterEach
    void tearDown() {
        doorSystem.stop();
    }

    @Test
    void doorBetweenItsWallsStands() {
        setWallsEastAndWest();
        EntityId door = addDoor(woodenDoor, false, 0);

        doorSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void doorFallsWhenItsTileIsClaimedByAnother() {
        setWallsEastAndWest();
        EntityId door = addDoor(woodenDoor, false, 0);
        setTile(TILE, CLAIMED_PATH, ENEMY);

        doorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void blueprintFallsWhenItsTileIsClaimedByAnother() {
        setWallsEastAndWest();
        EntityId door = addDoor(woodenDoor, true, 0);
        setTile(TILE, CLAIMED_PATH, ENEMY);

        doorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void doorFallsWhenOneOfItsWallsGoes() {
        setWallsEastAndWest();
        EntityId door = addDoor(woodenDoor, false, 0);
        setTile(new Point(3, 2), CLAIMED_PATH, PLAYER);

        doorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void blueprintFallsWhenOneOfItsWallsGoes() {
        setWallsEastAndWest();
        EntityId door = addDoor(woodenDoor, true, 0);
        setTile(new Point(1, 2), CLAIMED_PATH, PLAYER);

        doorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void onlyTheWallsTheDoorStandsBetweenHoldIt() {

        // Turned to stand between north and south, walls east and west don't do
        setTile(new Point(2, 1), REINFORCED_WALL, PLAYER);
        EntityId door = addDoor(woodenDoor, false, -FastMath.HALF_PI);
        setWallsEastAndWest();

        doorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void wallsOfAnotherKeeperHoldTheDoor() {
        setTile(new Point(2, 1), REINFORCED_WALL, ENEMY);
        setTile(new Point(2, 3), REINFORCED_WALL, ENEMY);
        EntityId door = addDoor(woodenDoor, false, -FastMath.HALF_PI);

        doorSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void barricadeNeedsNoWalls() {
        EntityId door = addDoor(barricade, false, 0);

        doorSystem.processTick(0.25f);

        assertNotNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void barricadeFallsWhenItsTileIsClaimedByAnother() {
        EntityId door = addDoor(barricade, false, 0);
        setTile(TILE, CLAIMED_PATH, ENEMY);

        doorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void enemyNearbyDiscoversOpenSecretDoor() {
        EntityId door = addSecretDoor(true);
        addCreature(ENEMY, new Point(2, 5));

        doorSystem.processTick(0.25f);

        assertFalse(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void closedSecretDoorStaysSecret() {
        EntityId door = addSecretDoor(false);
        addCreature(ENEMY, new Point(2, 3));

        doorSystem.processTick(0.25f);

        assertTrue(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void ownAndAlliedCreaturesDontDiscoverSecretDoor() {
        EntityId door = addSecretDoor(true);
        addCreature(PLAYER, new Point(2, 3));
        addCreature(ALLY, new Point(2, 3));

        doorSystem.processTick(0.25f);

        assertTrue(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void enemyTooFarDoesntDiscoverSecretDoor() {
        EntityId door = addSecretDoor(true);
        addCreature(ENEMY, new Point(2, 6));

        doorSystem.processTick(0.25f);

        assertTrue(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void enemyBehindAWallDoesntDiscoverSecretDoor() {
        EntityId door = addSecretDoor(true);
        setTile(new Point(2, 4), REINFORCED_WALL, PLAYER);
        addCreature(ENEMY, new Point(2, 5));

        doorSystem.processTick(0.25f);

        assertTrue(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void unconsciousEnemyDoesntDiscoverSecretDoor() {
        EntityId door = addSecretDoor(true);
        EntityId creature = addCreature(ENEMY, new Point(2, 3));
        entityData.setComponent(creature, new Unconscious(0));

        doorSystem.processTick(0.25f);

        assertTrue(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void fallenSecretDoorIsNotDiscovered() {
        EntityId door = addSecretDoor(true);
        addCreature(ENEMY, new Point(2, 3));
        setTile(TILE, CLAIMED_PATH, ENEMY);

        doorSystem.processTick(0.25f);

        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    private EntityId addSecretDoor(boolean open) {
        setWallsEastAndWest();
        EntityId door = addDoor(secretDoor, false, 0);
        entityData.setComponent(door, new DoorComponent(secretDoor.getDoorId(), false, false, true));
        entityData.setComponent(door, new DoorViewState(secretDoor.getDoorId(), false, false, open));
        return door;
    }

    private EntityId addCreature(short ownerId, Point tile) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new CreatureComponent());
        entityData.setComponent(entity, new Owner(ownerId, ownerId));
        entityData.setComponent(entity, new Position(0, WorldUtils.pointToVector3f(tile.x, tile.y)));
        return entity;
    }

    private Door door(short doorId) {
        if (doorId == barricade.getDoorId()) {
            return barricade;
        }
        if (doorId == secretDoor.getDoorId()) {
            return secretDoor;
        }
        return woodenDoor;
    }

    private EntityId addDoor(Door door, boolean blueprint, float rotation) {
        EntityId entity = entityData.createEntity();
        entityData.setComponent(entity, new DoorComponent(door.getDoorId(), false, blueprint));
        entityData.setComponent(entity, new Owner(PLAYER, PLAYER));
        Vector3f position = WorldUtils.pointToVector3f(TILE.x, TILE.y);
        entityData.setComponent(entity, new Position(rotation, position));
        return entity;
    }

    private void setWallsEastAndWest() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
    }

    private void setTile(Point point, short terrainId, short ownerId) {
        FakeMapTile tile = mapData.getTile(point.x, point.y);
        tile.terrainId = terrainId;
        tile.ownerId = ownerId;
    }

    private static Terrain terrain(short terrainId, Terrain.TerrainFlag... flags) {
        return TerrainFixtures.terrain(terrainId, TerrainFixtures.flags(flags), 0, 1, 1, terrainId);
    }

}
