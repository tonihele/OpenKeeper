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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An exploring creature walks to a random tile that nobody owns and that it
 * can walk to, and gives up after 20 tries (DKII.EXE 0x45A290)
 */
class ExploreTest {

    private static final int MAP_SIZE = 10;
    private static final short KEEPER = 3;

    private final Set<Point> solidTiles = new HashSet<>();
    private final Set<Point> ownedTiles = new HashSet<>();
    private final Set<Point> unreachableTiles = new HashSet<>();
    private final Deque<Integer> rolls = new ArrayDeque<>();
    private int rollCount;
    private final ICreatureController creature = stub(ICreatureController.class,
            (name, args) -> name.equals("getPosition") ? new Vector3f(1, 1, 1) : null);

    @Test
    void walksToTheFirstRandomTileThatNobodyOwnsAndItCanWalkTo() {
        solidTiles.add(new Point(2, 3));
        ownedTiles.add(new Point(4, 5));
        unreachableTiles.add(new Point(6, 7));
        rolls.addAll(List.of(2, 3, 4, 5, 6, 7, 8, 9));
        Explore explore = createTask();

        assertTrue(explore.chooseDestination(creature, random()));

        assertEquals(new Point(8, 9), explore.getTaskLocation());
        assertEquals(new Vector2f(8, 9), explore.getTarget(creature));
        assertTrue(explore.isValid(creature));
    }

    @Test
    void isDoneOnceTheCreatureIsThere() {
        rolls.addAll(List.of(8, 9));
        Explore explore = createTask();
        explore.chooseDestination(creature, random());

        explore.executeTask(creature, 0);

        assertFalse(explore.isValid(creature));
    }

    @Test
    void givesUpAfterTwentyTiles() {
        solidTiles.add(new Point(0, 0));
        Explore explore = createTask();

        // The rolls run out and every further roll is 0, the solid tile
        assertFalse(explore.chooseDestination(creature, random()));

        assertEquals(40, rollCount);
        assertFalse(explore.isValid(creature));
    }

    private RandomGenerator random() {
        return new RandomGenerator() {
            @Override
            public int nextInt(int bound) {
                rollCount++;
                return rolls.isEmpty() ? 0 : rolls.poll();
            }

            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Explore createTask() {
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth", "getHeight" -> MAP_SIZE;
            case "getTile" -> {
                Point p = args.length == 1 ? (Point) args[0] : new Point((int) args[0], (int) args[1]);
                yield p.x < 0 || p.y < 0 || p.x >= MAP_SIZE || p.y >= MAP_SIZE ? null
                        : stub(IMapTileInformation.class, (tileMethod, tileArgs) -> switch (tileMethod) {
                    case "getOwnerId" -> ownedTiles.contains(p) ? KEEPER : Player.NEUTRAL_PLAYER_ID;
                    case "getLocation" -> p;
                    default -> null;
                });
            }
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "isSolid" -> solidTiles.contains((Point) args[0]);
            default -> null;
        });
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> true;
            case "findPath" -> unreachableTiles.contains((Point) args[1]) ? null : stub(GraphPath.class, (pathMethod, pathArgs) -> null);
            default -> null;
        });

        return new Explore(navigationService, mapController);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> answer.answer(method.getName(), args));
    }

    private interface Answer {

        Object answer(String method, Object[] args);
    }
}
