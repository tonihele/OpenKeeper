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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards stand at the corners of the Guard Room and walk on to another corner
 * after a while, as in DK II
 */
class GuardTest {

    private static final int TICKS_PER_SECOND = 4;
    private static final int MAX_EXTRA_TICKS = 60;

    @Test
    void guardPostsAreTheCornersOfARectangularRoom() {
        List<Point> posts = Guard.getGuardPosts(rectangle(10, 10, 4, 3));

        assertEquals(Set.of(new Point(10, 10), new Point(13, 10), new Point(10, 12), new Point(13, 12)), Set.copyOf(posts));
    }

    @Test
    void guardPostsAreTheEndsOfAOneTileWideRoom() {
        List<Point> posts = Guard.getGuardPosts(rectangle(5, 5, 1, 4));

        assertEquals(Set.of(new Point(5, 5), new Point(5, 8)), Set.copyOf(posts));
    }

    @Test
    void aOneTileRoomIsItsOwnGuardPost() {
        assertEquals(List.of(new Point(3, 3)), Guard.getGuardPosts(List.of(new Point(3, 3))));
    }

    @Test
    void guardGoesToTheGuardPostNearestToIt() {
        Guard guard = guard(rectangle(10, 10, 4, 4), fixedRandom(0));

        Vector2f target = guard.getTarget(creatureAt(new Point(15, 14)));

        assertEquals(13, (int) target.x);
        assertEquals(13, (int) target.y);
        assertEquals(TaskType.GUARD, guard.getTaskType());
    }

    @Test
    void guardStandsAtItsPostForTheGuardTimeAndThenMovesToAnotherPost() {
        Guard guard = guard(rectangle(10, 10, 4, 4), fixedRandom(20));
        ICreatureController creature = creatureAt(new Point(15, 14));
        Vector2f post = guard.getTarget(creature);

        // 75 + 20 ticks at four ticks a second
        guard.executeTask(creature, 2f);
        guard.executeTask(creature, 2f + 23.5f);
        assertEquals(post, guard.getTarget(creature));

        guard.executeTask(creature, 2f + 23.75f);
        Vector2f nextPost = guard.getTarget(creature);
        assertNotEquals(post, nextPost);
        assertTrue(Guard.getGuardPosts(rectangle(10, 10, 4, 4)).contains(new Point((int) nextPost.x, (int) nextPost.y)));
    }

    @Test
    void theTimerStartsAgainWhenTheGuardReachesTheNextPost() {
        Guard guard = guard(rectangle(10, 10, 4, 4), fixedRandom(0));
        ICreatureController creature = creatureAt(new Point(10, 10));
        guard.getTarget(creature);
        guard.executeTask(creature, 0f);
        guard.executeTask(creature, 18.75f);
        Vector2f secondPost = guard.getTarget(creature);

        // The walk took a while, the guard stands its full time at the new post
        guard.executeTask(creature, 30f);
        guard.executeTask(creature, 48f);
        assertEquals(secondPost, guard.getTarget(creature));
    }

    private static Guard guard(List<Point> roomTiles, RandomGenerator random) {
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinates(roomTiles);
        IRoomController room = (IRoomController) Proxy.newProxyInstance(IRoomController.class.getClassLoader(), new Class<?>[]{IRoomController.class}, (proxy, method, args) -> {
            if ("getRoomInstance".equals(method.getName())) {
                return roomInstance;
            }
            throw new UnsupportedOperationException(method.getName());
        });
        return new Guard(null, null, roomTiles.get(0), (short) 3, room, null, TICKS_PER_SECOND, MAX_EXTRA_TICKS, random);
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
