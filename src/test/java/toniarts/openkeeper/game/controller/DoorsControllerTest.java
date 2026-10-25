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
package toniarts.openkeeper.game.controller;

import com.jme3.math.FastMath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.FakeMapData;
import toniarts.openkeeper.game.map.FakeMapInformation;
import toniarts.openkeeper.game.map.FakeMapTile;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.OWNABLE;
import static toniarts.openkeeper.tools.convert.map.Terrain.TerrainFlag.SOLID;

class DoorsControllerTest {

    private static final short PLAYER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;
    private static final short CLAIMED_PATH = 8;
    private static final short REINFORCED_WALL = 9;
    private static final Point TILE = new Point(2, 2);

    private final EntityData entityData = new DefaultEntityData();
    private final Door woodenDoor = DoorFixtures.door((short) 1, 250, 800);
    private final Door secretDoor = DoorFixtures.door((short) 5, 3000, Door.DoorFlag.IS_SECRET);
    private FakeMapData mapData;
    private DoorsController doorsController;

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
        IMapController mapController = (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getTerrain" -> mapInformation.getTerrain((IMapTileInformation) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
        IKwdFile kwdFile = (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(),
                new Class<?>[]{IKwdFile.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getThings" -> List.of();
            case "getDoorById" -> ((Number) args[0]).shortValue() == secretDoor.getDoorId() ? secretDoor : woodenDoor;
            default -> throw new UnsupportedOperationException(method.getName());
        });
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = Map.of(
                Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST,
                DoorFixtures.miscVariable(Variable.MiscVariable.MiscType.ROOM_SELL_VALUE_PERCENTAGE_OF_COST, 50),
                Variable.MiscVariable.MiscType.DOOR_SELL_VALUE_PERCENTAGE_OF_COST,
                DoorFixtures.miscVariable(Variable.MiscVariable.MiscType.DOOR_SELL_VALUE_PERCENTAGE_OF_COST, 75));
        doorsController = new DoorsController(kwdFile, entityData, gameSettings, mapController, null, null);
    }

    @Test
    void blueprintWaitsForItsDoor() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);

        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        assertTrue(entityData.getComponent(door, DoorComponent.class).blueprint);
        assertTrue(entityData.getComponent(door, DoorViewState.class).blueprint);
        assertEquals(PLAYER, entityData.getComponent(door, Owner.class).ownerId);
        assertEquals(TILE, WorldUtils.vectorToPoint(entityData.getComponent(door, Position.class).position));
        assertNull(entityData.getComponent(door, Health.class));
    }

    @Test
    void doorBetweenWallsEastAndWestIsNotTurned() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);

        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        assertEquals(0, entityData.getComponent(door, Position.class).rotation);
    }

    @Test
    void doorBetweenWallsNorthAndSouthIsTurned() {

        // The walls of another keeper hold the door as well
        setTile(new Point(2, 1), REINFORCED_WALL, ENEMY);
        setTile(new Point(2, 3), REINFORCED_WALL, ENEMY);

        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        assertEquals(-FastMath.HALF_PI, entityData.getComponent(door, Position.class).rotation);
    }

    @Test
    void deliveredDoorIsBuilt() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        doorsController.buildDoor(door);

        assertFalse(entityData.getComponent(door, DoorComponent.class).blueprint);
        assertFalse(entityData.getComponent(door, DoorViewState.class).blueprint);
        assertEquals(800, entityData.getComponent(door, Health.class).health);
    }

    @Test
    void deliveredSecretDoorIsSecret() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(secretDoor.getDoorId(), TILE, PLAYER);
        assertFalse(entityData.getComponent(door, DoorComponent.class).secret);

        doorsController.buildDoor(door);

        assertTrue(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void deliveredDoorIsNotSecret() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        doorsController.buildDoor(door);

        assertFalse(entityData.getComponent(door, DoorComponent.class).secret);
    }

    @Test
    void builtDoorSellsForAShareOfItsCost() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);
        doorsController.buildDoor(door);

        // The share is the one of the rooms, the door's own variable is not used
        assertEquals(OptionalInt.of(125), doorsController.sellDoor(entitiesOn(TILE), PLAYER));
        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void blueprintSellsForTheSameShare() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        assertEquals(OptionalInt.of(125), doorsController.sellDoor(entitiesOn(TILE), PLAYER));
        assertNull(entityData.getComponent(door, DoorComponent.class));
    }

    @Test
    void doorOfAnotherKeeperIsNotSold() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        assertEquals(OptionalInt.empty(), doorsController.sellDoor(entitiesOn(TILE), ENEMY));
        assertTrue(entityData.getComponent(door, DoorComponent.class) != null);
    }

    @Test
    void nothingIsSoldWithoutADoor() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);

        assertEquals(OptionalInt.empty(), doorsController.sellDoor(entitiesOn(new Point(2, 1)), PLAYER));
        assertTrue(entityData.getComponent(door, DoorComponent.class) != null);
    }

    @Test
    void builtDoorIsNotBuiltAgain() {
        setTile(new Point(1, 2), REINFORCED_WALL, PLAYER);
        setTile(new Point(3, 2), REINFORCED_WALL, PLAYER);
        EntityId door = doorsController.addDoorBlueprint(woodenDoor.getDoorId(), TILE, PLAYER);
        doorsController.buildDoor(door);
        entityData.setComponent(door, new Health(1, 800));

        doorsController.buildDoor(door);

        assertEquals(1, entityData.getComponent(door, Health.class).health);
    }

    private void setTile(Point point, short terrainId, short ownerId) {
        FakeMapTile tile = mapData.getTile(point.x, point.y);
        tile.terrainId = terrainId;
        tile.ownerId = ownerId;
    }

    private static Terrain terrain(short terrainId, Terrain.TerrainFlag... flags) {
        return TerrainFixtures.terrain(terrainId, TerrainFixtures.flags(flags), 0, 1, 1, terrainId);
    }

    private List<EntityId> entitiesOn(Point tile) {
        List<EntityId> result = new ArrayList<>();
        for (EntityId entity : entityData.findEntities(null, Position.class)) {
            if (WorldUtils.vectorToPoint(entityData.getComponent(entity, Position.class).position).equals(tile)) {
                result.add(entity);
            }
        }
        return result;
    }

}
