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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.DoorViewState;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The explored map of a loaded game isn't saved. It is seeded like the level
 * start, plus what the creatures see from the keeper's own land.
 */
class SeedLoadedGameTest {

    private static final short ROCK_ID = 1;
    private static final short FLOOR_ID = 2;
    private static final int MAP_SIZE = 20;

    /**
     * The keeper's room, x 2 - 4, y 6 - 8
     */
    private static final int ROOM_MAX_X = 4;

    /**
     * An unclaimed corridor going east from the room along y 7, and a
     * second one going north from it along x 3, behind a door at (3, 5)
     */
    private static final int CORRIDOR_Y = 7;
    private static final int DOOR_X = 3;
    private static final int DOOR_Y = 5;

    private DefaultEntityData entityData;
    private FogOfWarController controller;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        entityData = new DefaultEntityData();
        Terrain rock = TerrainFixtures.terrain(ROCK_ID, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, ROCK_ID);
        Terrain floor = TerrainFixtures.terrain(FLOOR_ID, TerrainFixtures.flags(), 0, 0, 0, FLOOR_ID);
        Creature creature = new Creature();
        Field distanceCanSee = Creature.Attributes.class.getDeclaredField("distanceCanSee");
        distanceCanSee.setAccessible(true);
        distanceCanSee.set(creature.getAttributes(), 4f);
        Door door = new Door();
        Field flags = Door.class.getDeclaredField("flags");
        flags.setAccessible(true);
        flags.set(door, EnumSet.noneOf(Door.DoorFlag.class));
        IKwdFile kwdFile = proxy(IKwdFile.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTerrain" ->
                (short) args[0] == ROCK_ID ? rock : floor;
            case "getCreatureList" ->
                List.of(creature);
            case "getDoorById" ->
                door;
            case "getThings" ->
                List.of();
            default ->
                throw new UnsupportedOperationException(method.getName());
        });

        EntityId doorEntity = entityData.createEntity();
        entityData.setComponents(doorEntity, new Position(0, WorldUtils.pointToVector3f(DOOR_X, DOOR_Y)),
                new DoorComponent((short) 1, false, false), new DoorViewState((short) 1, false, false, false));

        controller = new FogOfWarController(entityData, kwdFile, map(), Player.KEEPER1_ID, points -> {
        }, entity -> {
        });
    }

    @AfterEach
    void tearDown() {
        entityData.close();
    }

    @Test
    void levelStartExploresOnlyTheOwnedLandAndNextToIt() {
        controller.seedLevelStart();

        assertTrue(controller.isExplored(new Point(ROOM_MAX_X + 1, CORRIDOR_Y)));
        assertFalse(controller.isExplored(new Point(ROOM_MAX_X + 2, CORRIDOR_Y)));
    }

    @Test
    void loadedGameExploresWhatTheCreaturesSeeFromTheOwnedLand() {
        controller.seedLoadedGame();

        // Four tiles along the corridor, not five
        assertTrue(controller.isExplored(new Point(ROOM_MAX_X + 4, CORRIDOR_Y)));
        assertFalse(controller.isExplored(new Point(ROOM_MAX_X + 5, CORRIDOR_Y)));

        // The walls block the sight: the rock along the corridor is seen, not the rock behind it
        assertTrue(controller.isExplored(new Point(ROOM_MAX_X + 3, CORRIDOR_Y + 1)));
        assertFalse(controller.isExplored(new Point(ROOM_MAX_X + 3, CORRIDOR_Y + 2)));

        // So does the door, the corridor behind it stays dark
        assertTrue(controller.isExplored(new Point(DOOR_X, DOOR_Y)));
        assertFalse(controller.isExplored(new Point(DOOR_X, DOOR_Y - 1)));

        // The map border is never explored
        assertFalse(controller.isExplored(new Point(0, CORRIDOR_Y)));
    }

    @SuppressWarnings("unchecked")
    private static IMapDataInformation<IMapTileInformation> map() {
        IMapTileInformation[][] tiles = new IMapTileInformation[MAP_SIZE][MAP_SIZE];
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int y = 0; y < MAP_SIZE; y++) {
                boolean room = x >= 2 && x <= ROOM_MAX_X && y >= CORRIDOR_Y - 1 && y <= CORRIDOR_Y + 1;
                boolean corridor = (x > ROOM_MAX_X && x < MAP_SIZE - 1 && y == CORRIDOR_Y)
                        || (x == DOOR_X && y > 0 && y <= DOOR_Y);
                short terrainId = room || corridor ? FLOOR_ID : ROCK_ID;
                short ownerId = room ? Player.KEEPER1_ID : Player.NEUTRAL_PLAYER_ID;
                tiles[x][y] = proxy(IMapTileInformation.class, (proxy, method, args) -> switch (method.getName()) {
                    case "getTerrainId" ->
                        terrainId;
                    case "getOwnerId" ->
                        ownerId;
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
