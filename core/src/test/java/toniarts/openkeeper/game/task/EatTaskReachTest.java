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
package toniarts.openkeeper.game.task;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector2f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hungry creature goes for the nearest food it can walk to. A path search
 * to food out of its reach goes through all of the creature's land before it
 * fails, so that should not happen for every such food.
 */
class EatTaskReachTest {

    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Set<Point> land = new HashSet<>();
    private int pathSearches;
    private Task assignedTask;
    private final ICreatureController creature = stub(ICreatureController.class, (name, args) -> switch (name) {
        case "getOwnerId" -> PLAYER_ID;
        case "getCreatureCoordinates" -> new Point(7, 10);
        case "setAssignedTask" -> assignedTask = (Task) args[0];
        default -> null;
    });

    @Test
    void goesForTheNearestFoodInReachWithoutSearchingToAllTheFoodOutOfReach() {

        // The creature's land and, past the rock at 8, an island with the closest food
        addLand(0, 7);
        addLand(9, 12);
        createFood(9);
        createFood(10);
        createFood(11);
        createFood(0);
        createFood(1);

        assertTrue(taskManager().assignEatTask(creature));

        assertEquals(new Vector2f(1, 10), assignedTask.getTarget(creature));
        assertEquals(2, pathSearches, "The one that fails and the one to the food");
    }

    @Test
    void findsNoFoodWhenAllOfItIsOutOfReach() {
        addLand(0, 7);
        addLand(9, 12);
        createFood(9);
        createFood(10);
        createFood(11);

        assertFalse(taskManager().assignEatTask(creature));

        assertNull(assignedTask);
        assertEquals(1, pathSearches);
    }

    @Test
    void goesStraightForTheNearestFoodInReach() {
        addLand(0, 12);
        createFood(9);
        createFood(1);

        assertTrue(taskManager().assignEatTask(creature));

        assertEquals(new Vector2f(9, 10), assignedTask.getTarget(creature));
        assertEquals(1, pathSearches);
    }

    private void addLand(int fromX, int toX) {
        for (int x = fromX; x <= toX; x++) {
            land.add(new Point(x, 10));
        }
    }

    private void createFood(int x) {
        entityData.setComponents(entityData.createEntity(), new Food(), new Owner(PLAYER_ID, PLAYER_ID),
                new Position(0, WorldUtils.pointToVector3f(x, 10)));
    }

    private TaskManager taskManager() {
        // No tiles to scan for tasks at the start, but every tile can be looked up
        IMapDataInformation<?> map = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth", "getHeight" -> 0;
            case "getTile" -> args.length == 1 ? tile((Point) args[0]) : tile(new Point((int) args[0], (int) args[1]));
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> "getMapData".equals(name) ? map : null);

        // Walking goes over the land, tile by tile
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> land.contains(((IMapTileInformation) args[1]).getLocation());
            case "findPath" -> {
                pathSearches++;
                Point start = (Point) args[0];
                Point end = (Point) args[1];
                boolean connected = true;
                for (int x = Math.min(start.x, end.x); x <= Math.max(start.x, end.x); x++) {
                    connected &= land.contains(new Point(x, 10));
                }
                yield connected ? new DefaultGraphPath<IMapTileInformation>() : null;
            }
            default -> null;
        });
        IEntityPositionLookup entityPositionLookup = stub(IEntityPositionLookup.class, (name, args) -> "getEntityLocation".equals(name)
                ? tile(WorldUtils.vectorToPoint(entityData.getComponent((EntityId) args[0], Position.class).position)) : null);
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> "getKeeper".equals(name) ? keeper : null);
        return new TaskManager(entityData, stub(IGameWorldController.class, (name, args) -> null), mapController, null, null,
                navigationService, List.of(player), stub(ILevelInfo.class, (name, args) -> null), entityPositionLookup, Map.of());
    }

    private static IMapTileInformation tile(Point location) {
        return stub(IMapTileInformation.class, (name, args) -> "getLocation".equals(name) ? location : null);
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> switch (method.getName()) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> answer.answer(method.getName(), args);
        });
    }

}
