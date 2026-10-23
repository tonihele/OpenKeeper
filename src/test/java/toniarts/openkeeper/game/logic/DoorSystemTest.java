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
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;

class DoorSystemTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final Point TILE = new Point(2, 2);

    private final EntityData entityData = new DefaultEntityData();
    private final Door woodenDoor = DoorFixtures.door((short) 1, 250);
    private final Door barricade = DoorFixtures.door((short) 6, 400, Door.DoorFlag.IS_BARRICADE);
    private FakeMapData mapData;
    private DoorSystem doorSystem;

    @BeforeEach
    void setUp() {
        mapData = new FakeMapData(5, 5);
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                setTile(new Point(x, y), CLAIMED_PATH, PLAYER);
            }
        }
        FakeMapInformation mapInformation = new FakeMapInformation(mapData, Map.of(
                CLAIMED_PATH, terrain(CLAIMED_PATH, OWNABLE),
                REINFORCED_WALL, terrain(REINFORCED_WALL, SOLID, OWNABLE)));
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getDoorById" -> ((Number) args[0]).shortValue() == barricade.getDoorId() ? barricade : woodenDoor;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        doorSystem = new DoorSystem(entityData, kwdFile, mapInformation);
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
