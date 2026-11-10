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
package toniarts.openkeeper.view.fogofwar;

import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.DungeonHeart;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.ChickenSpawnFixtures;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.ResearchFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Dungeon Heart's start-of-level reveal runs on the first updates after
 * the level start seeding, also for a heart that was already there when the
 * fog of war started.
 */
class FogOfWarControllerTest {

    private static final short VIEWER = Player.KEEPER1_ID;
    private static final short FLOOR_ID = 1;
    private static final short WOODEN_DOOR_ID = 1;
    private static final short BARRICADE_DOOR_ID = 2;
    private static final int MAP_SIZE = 21;

    /**
     * More than enough to cover the heart reveal's start delay and steps
     */
    private static final int UPDATES = 100;

    private DefaultEntityData entityData;
    private FogOfWarController controller;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        Terrain floor = TerrainFixtures.terrain(FLOOR_ID, TerrainFixtures.flags(), 0, 0, 0, FLOOR_ID);
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> variables = Map.of(
                Variable.MiscVariable.MiscType.DUNGEON_HEART_CLAIM_SCAN_RADIUS_TILES,
                ChickenSpawnFixtures.miscVariable(Variable.MiscVariable.MiscType.DUNGEON_HEART_CLAIM_SCAN_RADIUS_TILES, 15));
        Map<Integer, Door> doors = Map.of(
                (int) WOODEN_DOOR_ID, door(WOODEN_DOOR_ID, false),
                (int) BARRICADE_DOOR_ID, door(BARRICADE_DOOR_ID, true));
        IKwdFile kwdFile = proxy(IKwdFile.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTerrain" ->
                floor;
            case "getDoorById" ->
                doors.get(args[0]);
            case "getThings" ->
                List.of();
            case "getVariables" ->
                variables;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        controller = new FogOfWarController(entityData, kwdFile, neutralFloorMap(), VIEWER,
                points -> {
                }, entityId -> {
                });
    }

    @AfterEach
    void tearDown() {
        controller.stop();
        entityData.close();
    }

    @Test
    void heartAddedAfterStartIsRevealed() {
        controller.start();
        createHeart(10, 10);

        assertHeartRevealedAfterSeeding();
    }

    @Test
    void heartPresentAtStartIsRevealed() {
        createHeart(10, 10);
        controller.start();

        assertHeartRevealedAfterSeeding();
    }

    @Test
    void doorSightIndexPreservesDoorAndBarricadeRulesAndClearsMovedDoors() {
        controller.start();
        EntityId woodenDoor = createDoor(10, 10, WOODEN_DOOR_ID);
        createDoor(12, 10, BARRICADE_DOOR_ID);

        controller.update(0.2f);
        assertTrue(blocksSightAt(controller, 10, 10));
        assertFalse(blocksSightAt(controller, 11, 10));
        assertFalse(blocksSightAt(controller, 12, 10));

        moveDoor(woodenDoor, 14, 14, WOODEN_DOOR_ID);
        controller.update(0.2f);
        assertFalse(blocksSightAt(controller, 10, 10));
        assertTrue(blocksSightAt(controller, 14, 14));
        assertFalse(blocksSightAt(controller, 12, 10));
    }

    private void assertHeartRevealedAfterSeeding() {
        Point nearEdgeOfReveal = new Point(10, 18);

        controller.seedLevelStart();
        assertFalse(controller.isExplored(nearEdgeOfReveal));
        for (int i = 0; i < UPDATES; i++) {
            controller.update(0.2f);
        }

        assertTrue(controller.isExplored(nearEdgeOfReveal), "The heart reveal runs once the fog is seeded");
    }

    private void createHeart(int x, int y) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id,
                new RoomComponent((short) 1, false, new Point(x, y)),
                new Owner(VIEWER, VIEWER),
                new DungeonHeart());
    }

    private EntityId createDoor(int x, int y, short doorId) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id, new DoorComponent(doorId, false, false),
                new DoorViewState(doorId, false, false, false),
                new Position(0f, WorldUtils.pointToVector3f(x, y)));
        return id;
    }

    private void moveDoor(EntityId id, int x, int y, short doorId) {
        entityData.setComponents(id, new DoorComponent(doorId, false, false),
                new DoorViewState(doorId, false, false, false),
                new Position(0f, WorldUtils.pointToVector3f(x, y)));
    }

    private static boolean blocksSightAt(FogOfWarController controller, int x, int y) {
        try {
            Method method = FogOfWarController.class.getDeclaredMethod("blocksSight", int.class, int.class);
            method.setAccessible(true);
            return (boolean) method.invoke(controller, x, y);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to check the fog controller's sight rule", e);
        }
    }

    private static Door door(short id, boolean barricade) {
        EnumSet<Door.DoorFlag> flags = barricade
                ? EnumSet.of(Door.DoorFlag.IS_BARRICADE)
                : EnumSet.noneOf(Door.DoorFlag.class);
        return ResearchFixtures.door(id, 1, flags);
    }

    @SuppressWarnings("unchecked")
    private static IMapDataInformation<IMapTileInformation> neutralFloorMap() {
        IMapTileInformation[][] tiles = new IMapTileInformation[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                Point location = new Point(x, y);
                tiles[x][y] = proxy(IMapTileInformation.class, (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrainId" ->
                        FLOOR_ID;
                    case "getOwnerId" ->
                        Player.NEUTRAL_PLAYER_ID;
                    case "getLocation" ->
                        location;
                    case "getX" ->
                        location.x;
                    case "getY" ->
                        location.y;
                    default ->
                        throw new UnsupportedOperationException(method.getName());
                });
            }
        }
        return proxy(IMapDataInformation.class, (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            return switch (method.getName()) {
                case "getWidth", "getHeight" ->
                    MAP_SIZE;
                case "getTile" -> {
                    int x = (int) args[0];
                    int y = (int) args[1];
                    yield x >= 0 && y >= 0 && x < MAP_SIZE && y < MAP_SIZE ? tiles[x][y] : null;
                }
                default ->
                    throw new UnsupportedOperationException(method.getName());
            };
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

}
