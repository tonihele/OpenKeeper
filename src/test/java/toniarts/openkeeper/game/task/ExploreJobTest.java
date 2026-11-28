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
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where a creature with the Explore job goes. DK II (0x45A290) tries up to 20
 * random tiles of the map and walks to the first that nobody has claimed,
 * isn't solid and that it can get to.
 */
class ExploreJobTest {

    private static final short PLAYER_ID = 3;

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Point, Short> owners = new HashMap<>();
    private final Set<Point> solid = new HashSet<>();
    private final Set<Point> unreachable = new HashSet<>();
    private int width;
    private Task assignedTask;
    private int unassigned;
    private final ICreatureController creature = stub(ICreatureController.class, (name, args) -> switch (name) {
        case "getOwnerId" -> PLAYER_ID;
        case "getPosition" -> new Vector3f(0, 0, 0);
        case "getCreatureCoordinates" -> new Point(0, 0);
        case "setAssignedTask" -> assignedTask = (Task) args[0];
        case "unassingCurrentTask" -> unassigned++;
        default -> null;
    });

    @Test
    void walksToATileNobodyHasClaimedAndIsDoneOnceThere() {
        TaskManager taskManager = taskManager();
        owners.put(new Point(0, 0), PLAYER_ID);
        owners.put(new Point(1, 0), Player.NEUTRAL_PLAYER_ID);
        width = 2;

        assertTrue(taskManager.assignTask(creature, Creature.JobType.EXPLORE));

        assertEquals(new Vector2f(1, 0), assignedTask.getTarget(creature));
        assignedTask.executeTask(creature, 0);
        assertEquals(1, unassigned);
    }

    @Test
    void leavesClaimedSolidAndUnreachableTilesAlone() {
        TaskManager taskManager = taskManager();
        owners.put(new Point(0, 0), PLAYER_ID);
        owners.put(new Point(1, 0), Player.NEUTRAL_PLAYER_ID);
        owners.put(new Point(2, 0), Player.NEUTRAL_PLAYER_ID);
        solid.add(new Point(1, 0));
        unreachable.add(new Point(2, 0));
        width = 3;

        assertFalse(taskManager.assignTask(creature, Creature.JobType.EXPLORE));
    }

    private TaskManager taskManager() {

        // No tiles to scan for tasks at the start, the map is given its width afterwards
        IMapDataInformation<?> map = stub(IMapDataInformation.class, (name, args) -> switch (name) {
            case "getWidth" -> width;
            case "getHeight" -> width > 0 ? 1 : 0;
            case "getTile" -> args.length == 1 ? tile((Point) args[0]) : tile(new Point((int) args[0], (int) args[1]));
            default -> null;
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> map;
            case "isSolid" -> solid.contains((Point) args[0]);
            case "getRoomControllers" -> List.of();
            default -> null;
        });
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> switch (name) {
            case "isAccessible" -> true;
            case "findPath" -> unreachable.contains((Point) args[1]) ? null : new DefaultGraphPath<IMapTileInformation>();
            case "isReachable" -> !unreachable.contains((Point) args[1]);
            default -> null;
        });
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> "getKeeper".equals(name) ? keeper : null);
        return new TaskManager(entityData, stub(IGameWorldController.class, (name, args) -> null), mapController, null, null,
                navigationService, List.of(player), stub(ILevelInfo.class, (name, args) -> null),
                stub(IEntityPositionLookup.class, (name, args) -> null), Map.of());
    }

    private IMapTileController tile(Point location) {
        return stub(IMapTileController.class, (name, args) -> switch (name) {
            case "getLocation" -> location;
            case "getOwnerId" -> owners.getOrDefault(location, (short) 0);
            default -> null;
        });
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
