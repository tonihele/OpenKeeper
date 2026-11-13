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
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Food;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * A hungry creature goes for the nearest food of its own keeper.
 */
public class TaskManagerEatTaskTest {

    private static final short PLAYER_ID = 3;
    private static final short OTHER_PLAYER_ID = 4;

    private final EntityData entityData = new DefaultEntityData();
    private Task assignedTask;
    private final ICreatureController creature = stub(ICreatureController.class, (name, args) -> switch (name) {
        case "getOwnerId" -> PLAYER_ID;
        case "getCreatureCoordinates" -> new Point(1, 1);
        case "setAssignedTask" -> assignedTask = (Task) args[0];
        default -> null;
    });

    @Test
    public void goesForTheNearestFoodOfItsOwnKeeper() {
        createFood(PLAYER_ID, 9, 1);
        createFood(OTHER_PLAYER_ID, 2, 1);
        createFood(PLAYER_ID, 5, 1);

        assertTrue(createTaskManager().assignEatTask(creature));

        assertEquals(new Vector2f(5, 1), assignedTask.getTarget(creature));
    }

    @Test
    public void leavesTheFoodOfOtherKeepers() {
        createFood(OTHER_PLAYER_ID, 2, 1);

        assertFalse(createTaskManager().assignEatTask(creature));

        assertNull(assignedTask);
    }

    private void createFood(short ownerId, int x, int y) {
        entityData.setComponents(entityData.createEntity(), new Food(), new Owner(ownerId, ownerId),
                new Position(0, WorldUtils.pointToVector3f(x, y)));
    }

    private TaskManager createTaskManager() {
        Keeper keeper = new Keeper(false, PLAYER_ID);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> "getKeeper".equals(name) ? keeper : null);
        IEntityPositionLookup entityPositionLookup = stub(IEntityPositionLookup.class, (name, args) -> {
            if (!"getEntityLocation".equals(name)) {
                return null;
            }
            Point location = WorldUtils.vectorToPoint(entityData.getComponent((EntityId) args[0], Position.class).position);
            return stub(IMapTileInformation.class, (tileName, tileArgs) -> "getLocation".equals(tileName) ? location : null);
        });
        INavigationService navigationService = stub(INavigationService.class,
                (name, args) -> "findPath".equals(name) ? new DefaultGraphPath<IMapTileInformation>() : null);

        // An empty map
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> 0);
        IMapController mapController = stub(IMapController.class, (name, args) -> "getMapData".equals(name) ? mapData : null);

        return new TaskManager(entityData, stub(IGameWorldController.class, (name, args) -> null),
                mapController, stub(IObjectsController.class, (name, args) -> null),
                stub(ICreaturesController.class, (name, args) -> null), navigationService, List.of(player),
                stub(ILevelInfo.class, (name, args) -> null), entityPositionLookup, Map.of());
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
