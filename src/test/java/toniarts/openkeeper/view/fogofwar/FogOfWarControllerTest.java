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
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.DungeonHeart;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.RoomComponent;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.FogVisionFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fog of war must not run before the level start seeding. The seeding
 * clears everything explored so far, so anything seen or reported before it
 * would be back in the fog but already reported, and the Dungeon Heart's
 * start-of-level reveal would be used up.
 */
class FogOfWarControllerTest {

    private static final short VIEWER = Player.KEEPER1_ID;
    private static final short ENEMY = Player.KEEPER2_ID;
    private static final short FLOOR_ID = 1;
    private static final short CREATURE_ID = 1;
    private static final int MAP_SIZE = 21;

    /**
     * More than enough to cover the heart reveal's start delay and steps
     */
    private static final int UPDATES = 100;

    private final List<EntityId> sightings = new ArrayList<>();

    private DefaultEntityData entityData;
    private FogOfWarController controller;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        Terrain floor = TerrainFixtures.terrain(FLOOR_ID, TerrainFixtures.flags(), 0, 0, 0, FLOOR_ID);
        Creature creature = FogVisionFixtures.creature(CREATURE_ID, 3, 0);
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> variables = Map.of(
                Variable.MiscVariable.MiscType.DUNGEON_HEART_CLAIM_SCAN_RADIUS_TILES,
                FogVisionFixtures.miscVariable(Variable.MiscVariable.MiscType.DUNGEON_HEART_CLAIM_SCAN_RADIUS_TILES, 15));
        IKwdFile kwdFile = proxy(IKwdFile.class, (proxy, method, args) -> switch (method.getName()) {
            case "getTerrain" ->
                floor;
            case "getCreature" ->
                creature;
            case "getThings" ->
                List.of();
            case "getVariables" ->
                variables;
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        controller = new FogOfWarController(entityData, kwdFile, neutralFloorMap(), VIEWER,
                points -> {
                }, sightings::add);
    }

    @AfterEach
    void tearDown() {
        controller.stop();
        entityData.close();
    }

    @Test
    void updateBeforeSeedingExploresAndReportsNothing() {
        createCreature(VIEWER, 10, 10);
        createCreature(ENEMY, 11, 10);
        controller.start();

        update(UPDATES);

        assertTrue(sightings.isEmpty(), "No enemy can be sighted before the fog is seeded");
        assertFalse(controller.isExplored(new Point(10, 10)), "Nothing is explored before the fog is seeded");
    }

    @Test
    void enemyNextToOwnCreatureIsReportedOnceAfterSeeding() {
        createCreature(VIEWER, 10, 10);
        EntityId enemy = createCreature(ENEMY, 11, 10);
        controller.start();
        update(UPDATES);
        assertTrue(sightings.isEmpty(), "No enemy can be sighted before the fog is seeded");

        controller.seedLevelStart();
        update(UPDATES);

        assertEquals(List.of(enemy), sightings);
        assertTrue(controller.isExplored(new Point(11, 10)));
    }

    @Test
    void heartAddedBeforeSeedingIsRevealedAfterSeeding() {
        controller.start();
        createHeart(10, 10);

        assertHeartRevealedOnlyAfterSeeding();
    }

    @Test
    void heartPresentAtStartIsRevealedAfterSeeding() {
        createHeart(10, 10);
        controller.start();

        assertHeartRevealedOnlyAfterSeeding();
    }

    private void assertHeartRevealedOnlyAfterSeeding() {
        Point nearEdgeOfReveal = new Point(10, 18);

        update(UPDATES);
        assertFalse(controller.isExplored(nearEdgeOfReveal), "The heart reveal must wait for the fog seeding");

        controller.seedLevelStart();
        assertFalse(controller.isExplored(nearEdgeOfReveal));
        update(UPDATES);

        assertTrue(controller.isExplored(nearEdgeOfReveal), "The heart reveal runs once the fog is seeded");
    }

    private void update(int times) {
        for (int i = 0; i < times; i++) {
            controller.update(0.2f);
        }
    }

    private EntityId createCreature(short ownerId, int x, int y) {
        EntityId id = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = CREATURE_ID;
        entityData.setComponents(id,
                new Position(0, WorldUtils.pointToVector3f(x, y)),
                new Owner(ownerId, ownerId),
                creatureComponent);
        return id;
    }

    private void createHeart(int x, int y) {
        EntityId id = entityData.createEntity();
        entityData.setComponents(id,
                new RoomComponent((short) 1, false, new Point(x, y)),
                new Owner(VIEWER, VIEWER),
                new DungeonHeart());
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
