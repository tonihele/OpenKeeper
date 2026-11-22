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
package toniarts.openkeeper.game.task.creature;

import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards walk round the edge of the Guard Room and now and then go to a piece
 * of its furniture and a random tile of the room, as in DK II (DKII.EXE
 * 0x462930, 0x462D00, 0x463190, 0x4632A0)
 */
class GuardTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int MAX_EXTRA_TICKS = 60;
    private static final int NO_FURNITURE_EXTRA_TICKS = 5;

    private final EntityData entityData = new DefaultEntityData();
    private final Set<EntityId> furniture = new HashSet<>();

    @Test
    void guardGoesToTheEdgeTileNearestToIt() {
        Guard guard = guard(rectangle(10, 10, 5, 5), fixedRandom(0));

        assertEquals(new Vector2f(14, 14), guard.getTarget(creatureAt(new Point(16, 15))));
        assertEquals(TaskType.GUARD, guard.getTaskType());
    }

    @Test
    void anInnerTileIsNotOnTheEdge() {
        Guard guard = guard(rectangle(10, 10, 5, 5), fixedRandom(0));

        // The tile beside it is not on the edge, the one beyond is
        assertEquals(new Vector2f(10, 12), guard.getTarget(creatureAt(new Point(11, 12))));
    }

    @Test
    void guardWalksRoundTheEdgeAStretchAtATime() {
        Guard guard = guard(rectangle(10, 10, 5, 4), fixedRandom(0));
        ICreatureController creature = creatureAt(new Point(10, 9));
        guard.getTarget(creature);

        // From the corner it walks a whole side of the room, then the next one, round and back
        List<Vector2f> stops = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            guard.executeTask(creature, i);
            stops.add(guard.getTarget(creature));
        }
        assertEquals(List.of(new Vector2f(14, 10), new Vector2f(14, 13), new Vector2f(10, 13), new Vector2f(10, 10), new Vector2f(14, 10)), stops);
    }

    @Test
    void guardGoesToTheFurnitureAndARoomTileNowAndThenAndBackToTheEdge() {
        Guard guard = guard(rectangle(10, 10, 5, 5), fixedRandom(20));
        EntityId post = entityData.createEntity();
        entityData.setComponent(post, new Position(0, WorldUtils.pointToVector3f(12, 12)));
        furniture.add(post);
        ICreatureController creature = creatureAt(new Point(10, 9));
        guard.getTarget(creature);

        // 75 + 20 ticks at four ticks a second after it reached the edge
        guard.executeTask(creature, 2f);
        guard.executeTask(creature, 2f + 23.5f);
        assertEquals(new Vector2f(14, 14), guard.getTarget(creature));

        guard.executeTask(creature, 2f + 23.75f);
        assertEquals(new Vector2f(12, 12), guard.getTarget(creature));

        // Then a random room tile, the 21st with this random number, and back to the nearest edge tile
        guard.executeTask(creature, 30f);
        assertEquals(new Vector2f(10, 14), guard.getTarget(creature));
        guard.executeTask(creature, 31f);
        assertEquals(new Vector2f(10, 14), guard.getTarget(creature));
    }

    @Test
    void withoutFurnitureTheGuardKeepsWalkingTheEdge() {
        Guard guard = guard(rectangle(10, 10, 5, 4), fixedRandom(0));
        ICreatureController creature = creatureAt(new Point(10, 9));
        guard.getTarget(creature);

        guard.executeTask(creature, 0f);
        guard.executeTask(creature, 100f);

        assertEquals(new Vector2f(14, 13), guard.getTarget(creature));
    }

    private Guard guard(List<Point> roomTiles, RandomGenerator random) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinates(roomTiles);
        IRoomController room = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getRoomInstance" -> roomInstance;
                case "getFloorFurniture" -> furniture;
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
        return new Guard(null, null, roomTiles.get(0), (short) 3, room, null, entityData, TICKS_PER_SECOND, MAX_EXTRA_TICKS,
                NO_FURNITURE_EXTRA_TICKS, random);
    }

    private static ICreatureController creatureAt(Point p) {
        return (ICreatureController) Proxy.newProxyInstance(ICreatureController.class.getClassLoader(), new Class<?>[]{ICreatureController.class}, (proxy, method, args) -> {
            if ("getCreatureCoordinates".equals(method.getName())) {
                return p;
            }
            throw new UnsupportedOperationException(method.getName());
        });
    }

    private static RandomGenerator fixedRandom(int value) {
        return new RandomGenerator() {

            @Override
            public long nextLong() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(value, bound - 1);
            }
        };
    }

    private static List<Point> rectangle(int x, int y, int width, int height) {
        List<Point> tiles = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                tiles.add(new Point(x + i, y + j));
            }
        }
        return tiles;
    }

}
