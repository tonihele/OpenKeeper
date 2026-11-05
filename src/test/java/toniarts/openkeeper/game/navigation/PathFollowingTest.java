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
package toniarts.openkeeper.game.navigation;

import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.MovementSystem;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.pathfinding.INavigable;
import toniarts.openkeeper.game.navigation.steering.FollowWaypoints;
import toniarts.openkeeper.game.navigation.steering.SteerableEntity;
import toniarts.openkeeper.game.navigation.steering.SteeringUtils;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creatures walk the way DK II's do: straight lines between the waypoints
 * where the way turns round a wall (0x4CEF40), heading for the next waypoint
 * from wherever they are (0x4D3C40).
 */
class PathFollowingTest {

    private static final short KNIGHT = 13;
    private static final short IMP = 1;
    private static final float KNIGHT_RADIUS = 0.165f;

    /**
     * Tile n spans n - 0.5 to n + 0.5. Water (~) costs more than floor.
     */
    private static final String[] MAP = {
        "############",
        "#.....#....#",
        "#.....#....#",
        "#.....#....#",
        "#..........#",
        "#~~~~~~....#",
        "#..........#",
        "############"};

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Point, IMapTileInformation> tiles = new HashMap<>();
    private final IMapController mapController = map();
    private final NavigationService navigationService = new NavigationService(mapController, positionLookup());
    private final Map<Short, Creature> creatures = Map.of(
            KNIGHT, ShotFixtures.creature(KNIGHT, 2 * KNIGHT_RADIUS, 4),
            IMP, ShotFixtures.creature(IMP, EnumSet.of(Creature.CreatureFlag.IS_WORKER), EnumSet.noneOf(Creature.CreatureFlag3.class)));

    @Test
    void crossesAnOpenRoomInAStraightLine() {
        List<Vector2> waypoints = waypoints(new Vector2(1.2f, 1.1f), new Point(5, 3));

        assertEquals(List.of(new Vector2(1.2f, 1.1f), new Vector2(5, 3)), waypoints);
    }

    @Test
    void turnsOnlyWhereTheWallIsInTheWay() {
        List<Vector2> waypoints = waypoints(new Vector2(1, 1), new Point(10, 1));

        assertTrue(waypoints.size() > 2, "Through the wall: " + waypoints);
        assertTrue(waypoints.size() <= 5, "Every tile kept: " + waypoints);
        for (int i = 1; i < waypoints.size(); i++) {
            assertClear(waypoints.get(i - 1), waypoints.get(i), KNIGHT_RADIUS);
        }
    }

    @Test
    void doesNotCutAcrossWaterThePathWentRound() {
        List<Vector2> path = List.of(new Vector2(2, 4), new Vector2(7, 4), new Vector2(7, 6), new Vector2(2, 6));

        assertEquals(path, navigationService.smoothPath(path, KNIGHT_RADIUS, navigable(false)));
        assertEquals(List.of(new Vector2(2, 4), new Vector2(2, 6)), navigationService.smoothPath(path, KNIGHT_RADIUS, navigable(true)),
                "Flying, water is no dearer than floor");
    }

    @Test
    void headsStraightForTheWaypointFromWhereverItIs() {
        SteerableEntity knight = new SteerableEntity(entityData.createEntity(), 0.5f, KNIGHT_RADIUS, 3, 2.6f, 0);
        FollowWaypoints followWaypoints = new FollowWaypoints(knight,
                List.of(new Vector2(1, 2), new Vector2(6, 2), new Vector2(6, 6)), waypoint -> 0, waypoint -> false, false, false, 4);

        Vector2 steering = followWaypoints.calculateSteering(new SteeringAcceleration<>(new Vector2())).linear;

        Vector2 toWaypoint = new Vector2(6, 2).sub(3, 2.6f);
        assertEquals(0, toWaypoint.angle(steering), 0.01f, "Pulled back onto the line");
    }

    @Test
    void nearlyStopsForACrowdRoundTheWaypoint() {
        List<Vector2> waypoints = List.of(new Vector2(1, 2), new Vector2(2, 2), new Vector2(2, 6));

        // Wants (1.02 - crowd) of the way to the waypoint in a tick (0x4D3C40, 0x4D15E0)
        assertEquals(1.02f / 0.02f, steering(waypoints, 0, false).len() / steering(waypoints, 1.5f, false).len(), 0.5f);
    }

    @Test
    void aChaseSlowsDownForACrowdAtItsEndToo() {
        List<Vector2> waypoints = List.of(new Vector2(1, 2), new Vector2(3, 2));

        assertEquals(steering(waypoints, 0, false), steering(waypoints, 1, false), "A walk arrives at its end");
        assertTrue(steering(waypoints, 1, true).len() < steering(waypoints, 0, true).len() / 10, "DK II's path flag 2 (0x471D10)");
    }

    private Vector2 steering(List<Vector2> waypoints, float crowd, boolean chase) {
        SteerableEntity knight = new SteerableEntity(entityData.createEntity(), 0.5f, KNIGHT_RADIUS, 1, 2, 0);
        FollowWaypoints followWaypoints = new FollowWaypoints(knight, waypoints, waypoint -> crowd, waypoint -> false, false, chase, 4);
        return new Vector2(followWaypoints.calculateSteering(new SteeringAcceleration<>(new Vector2())).linear);
    }

    @ParameterizedTest
    @ValueSource(shorts = {KNIGHT, IMP})
    void walksRoundTheWallAndStopsAtTheEnd(short creatureId) {
        Vector2 start = new Vector2(1, 1);
        EntityId walker = walker(creatureId, start, new Point(10, 1));
        MovementSystem movementSystem = new MovementSystem(entityData, kwdFile(), mapController);

        for (int frame = 0; frame < 60 * 60 && entityData.getComponent(walker, Navigation.class) != null; frame++) {
            movementSystem.processTick(1 / 60f);
            Point tile = WorldUtils.vectorToPoint(position(walker));
            assertTrue(MAP[tile.y].charAt(tile.x) != '#', "In the wall at " + position(walker) + " on frame " + frame);
        }

        assertNull(entityData.getComponent(walker, Navigation.class), "Still walking");
        float tolerance = creatureId == IMP ? 0.1f : KNIGHT_RADIUS / 2;
        assertEquals(0, position(walker).dst(10, 1), tolerance, "DK II stops within half a radius, a tenth of a tile for workers");
    }

    private List<Vector2> waypoints(Vector2 start, Point end) {
        List<Vector2> path = SteeringUtils.pathToList(navigationService.findPath(WorldUtils.vectorToPoint(start), end, navigable(false)));
        path.addFirst(start);
        return navigationService.smoothPath(path, KNIGHT_RADIUS, navigable(false));
    }

    private EntityId walker(short creatureId, Vector2 start, Point end) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(entityId, creatureComponent,
                new Position(0, new Vector3f(start.x, 0, start.y)),
                new Mobile(false, false, false, 0.5f),
                new Navigation(end, null, waypoints(start, end)));
        return entityId;
    }

    private Vector2 position(EntityId entityId) {
        Vector3f position = entityData.getComponent(entityId, Position.class).position;
        return new Vector2(position.x, position.z);
    }

    private static void assertClear(Vector2 from, Vector2 to, float radius) {
        Vector2 side = new Vector2(to).sub(from).nor().rotate90(1).scl(radius);
        for (float t = 0; t <= 1; t += 0.01f) {
            for (int s = -1; s <= 1; s++) {
                Vector2 point = new Vector2(from).lerp(to, t).mulAdd(side, s);
                Point tile = WorldUtils.vectorToPoint(point);
                assertTrue(MAP[tile.y].charAt(tile.x) != '#', "Through the wall at " + point + " between " + from + " and " + to);
            }
        }
    }

    private static INavigable navigable(boolean canFly) {
        return new INavigable() {

            @Override
            public short getOwnerId() {
                return 3;
            }

            @Override
            public boolean canFly() {
                return canFly;
            }

            @Override
            public boolean canWalkOnWater() {
                return true;
            }

            @Override
            public boolean canWalkOnLava() {
                return false;
            }

            @Override
            public boolean canMoveDiagonally() {
                return true;
            }
        };
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> creatures.get((Short) args[0]);
            case "getGameLevel" -> CreatureSleepFixtures.level(4);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static IEntityPositionLookup positionLookup() {
        return (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "isTileBlocked" -> false;
            default -> null;
        });
    }

    private IMapController map() {
        Terrain floor = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        Terrain rock = TerrainFixtures.terrain((short) 2, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, (short) 0);
        Terrain water = TerrainFixtures.terrain((short) 4, TerrainFixtures.flags(Terrain.TerrainFlag.WATER), 0, 0, 0, (short) 0);
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth" -> MAP[0].length();
            case "getHeight" -> MAP.length;
            case "getTile" -> tile(args.length == 1 ? (Point) args[0] : new Point((int) args[0], (int) args[1]));
            default -> throw new UnsupportedOperationException(method.getName());
        });

        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "isSolid" -> MAP[((Point) args[0]).y].charAt(((Point) args[0]).x) == '#';
            case "isWater" -> MAP[((Point) args[0]).y].charAt(((Point) args[0]).x) == '~';
            case "isLava" -> false;
            case "getRoomControllerByCoordinates" -> null;
            case "getTerrain" -> {
                Point p = ((IMapTileInformation) args[0]).getLocation();
                yield switch (MAP[p.y].charAt(p.x)) {
                    case '#' -> rock;
                    case '~' -> water;
                    default -> floor;
                };
            }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IMapTileInformation tile(Point point) {
        if (point.x < 0 || point.y < 0 || point.y >= MAP.length || point.x >= MAP[0].length()) {
            return null;
        }
        return tiles.computeIfAbsent(point, p -> (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getX" -> p.x;
            case "getY" -> p.y;
            case "getLocation" -> p;
            case "getIndex" -> p.y * MAP[0].length() + p.x;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> p.toString();
            default -> throw new UnsupportedOperationException(method.getName());
        }));
    }
}
