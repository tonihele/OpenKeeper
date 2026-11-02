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

import com.badlogic.gdx.math.Vector2;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
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
 * Creatures walking to the same spot are kept apart like DK II's separation
 * steering does (0x4D2DF0, 0x4D2B80), but never pushed into walls.
 */
class CreatureSeparationTest {

    private static final short KNIGHT = 13;
    private static final short GIANT = 9;
    private static final short IMP = 1;
    private static final float KNIGHT_WIDTH = 0.33f;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Creature> creatures = Map.of(
            KNIGHT, ShotFixtures.creature(KNIGHT, KNIGHT_WIDTH, 4),
            GIANT, ShotFixtures.creature(GIANT, 0.9f, 5),
            IMP, ShotFixtures.creature(IMP, 0.9f, 1));
    private final Set<Point> solidTiles = Set.of(new Point(2, 1), new Point(3, 1), new Point(4, 1), new Point(5, 1), new Point(6, 1),
            new Point(2, 3), new Point(3, 3), new Point(4, 3), new Point(5, 3), new Point(6, 3));
    private final Map<Point, IMapTileInformation> tiles = new HashMap<>();
    private final Map<IMapTileInformation, Point> tileLocations = new HashMap<>();

    @Test
    void creaturesSentToTheSameSpotEndApartByTheirWidth() {
        EntityId first = creature(KNIGHT, new Point(5, 6), new Point(5, 5), new Point(5, 4));
        EntityId second = creature(KNIGHT, new Point(7, 4), new Point(6, 4), new Point(5, 4));

        run(new MovementSystem(entityData, kwdFile(), map(), positionLookup()), 600);

        assertNull(entityData.getComponent(first, Navigation.class));
        assertNull(entityData.getComponent(second, Navigation.class));
        float distance = position(first).dst(position(second));
        assertEquals(KNIGHT_WIDTH, distance, 0.08f, "DK II keeps them apart by the sum of their radii");
    }

    @Test
    void separationDoesNotPushIntoAWall() {
        CreatureComponent standing = new CreatureComponent();
        standing.creatureId = GIANT;
        entityData.setComponents(entityData.createEntity(), standing, new Position(0, new Vector3f(4, 0, 2.4f)));
        EntityId walker = creature(IMP, new Point(2, 2), new Point(3, 2), new Point(4, 2), new Point(5, 2), new Point(6, 2));

        MovementSystem movementSystem = new MovementSystem(entityData, kwdFile(), map(), positionLookup());
        for (int i = 0; i < 600; i++) {
            movementSystem.processTick(1 / 60f);
            assertEquals(2, WorldUtils.vectorToPoint(position(walker)).y, "Pushed into the wall at frame " + i);
        }
    }

    private EntityId creature(short creatureId, Point... path) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        List<Vector2> navigationPath = new ArrayList<>();
        for (Point p : path) {
            navigationPath.add(WorldUtils.pointToVector2(p));
        }
        entityData.setComponents(entityId, creatureComponent,
                new Position(0, new Vector3f(path[0].x, 0, path[0].y)),
                new Mobile(false, false, false, 1),
                new Navigation(path[path.length - 1], null, navigationPath));
        return entityId;
    }

    private static void run(MovementSystem movementSystem, int frames) {
        for (int i = 0; i < frames; i++) {
            movementSystem.processTick(1 / 60f);
        }
    }

    private Vector2 position(EntityId entityId) {
        Vector3f position = entityData.getComponent(entityId, Position.class).position;
        return new Vector2(position.x, position.z);
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getCreature" -> creatures.get((Short) args[0]);
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IEntityPositionLookup positionLookup() {
        return (IEntityPositionLookup) Proxy.newProxyInstance(IEntityPositionLookup.class.getClassLoader(), new Class<?>[]{IEntityPositionLookup.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getEntitiesInLocation" -> entitiesIn(new Point((int) args[0], (int) args[1]));
            case "isTileBlocked" -> false;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private List<EntityId> entitiesIn(Point tile) {
        List<EntityId> entities = new ArrayList<>();
        EntitySet positioned = entityData.getEntities(Position.class);
        positioned.forEach(entity -> {
            if (WorldUtils.vectorToPoint(entity.get(Position.class).position).equals(tile)) {
                entities.add(entity.getId());
            }
        });
        positioned.release();
        return entities;
    }

    private IMapController map() {
        Terrain floor = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
        Terrain rock = TerrainFixtures.terrain((short) 2, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, (short) 0);
        IMapDataInformation<?> mapData = (IMapDataInformation<?>) Proxy.newProxyInstance(IMapDataInformation.class.getClassLoader(),
                new Class<?>[]{IMapDataInformation.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" -> 10;
            case "getTile" -> tile(args.length == 1 ? (Point) args[0] : new Point((int) args[0], (int) args[1]));
            default -> throw new UnsupportedOperationException(method.getName());
        });

        return (IMapController) Proxy.newProxyInstance(IMapController.class.getClassLoader(), new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getMapData" -> mapData;
            case "getTerrain" -> solidTiles.contains(tileLocations.get(args[0])) ? rock : floor;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private IMapTileInformation tile(Point point) {
        return tiles.computeIfAbsent(point, p -> {
            IMapTileInformation tile = (IMapTileInformation) Proxy.newProxyInstance(IMapTileInformation.class.getClassLoader(),
                    new Class<?>[]{IMapTileInformation.class}, (proxy, method, args) -> switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
            tileLocations.put(tile, p);
            return tile;
        });
    }
}
