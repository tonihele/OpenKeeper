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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IObjectsController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.listener.PlayerActionListener;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileController;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.RoomFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DK II sends the keeper's creatures that are on their way to a job back to
 * idle whenever the keeper's rooms change (0x4E8D30)
 */
class TaskManagerRoomChangeTest {

    private final EntityData entityData = new DefaultEntityData();
    private final Map<EntityId, Boolean> atTaskTarget = new HashMap<>();
    private final List<EntityId> sentToIdle = new ArrayList<>();
    private PlayerActionListener playerActions;
    private EntityId walking;
    private EntityId working;
    private EntityId otherKeepers;

    @BeforeEach
    void setUp() {
        walking = createCreature(Player.KEEPER1_ID, false);
        working = createCreature(Player.KEEPER1_ID, true);
        otherKeepers = createCreature(Player.KEEPER2_ID, false);

        // A room on land, no bridge for the tasks to look around
        IMapController mapController = stub(IMapController.class, Map.of(
                "getMapData", stub(IMapDataInformation.class, Map.of("getWidth", 0, "getHeight", 0,
                        "getTile", stub(IMapTileController.class, Map.of("getOwnerId", Player.NEUTRAL_PLAYER_ID)))),
                "getTerrain", TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(Terrain.TerrainFlag.ROOM), 0, 0, 0, (short) 0)));
        IGameWorldController gameWorldController = (IGameWorldController) Proxy.newProxyInstance(
                IGameWorldController.class.getClassLoader(), new Class<?>[]{IGameWorldController.class},
                (proxy, method, args) -> {
                    if (args != null && args[0] instanceof PlayerActionListener listener) {
                        playerActions = listener;
                    }
                    return null;
                });
        ICreaturesController creaturesController = (ICreaturesController) Proxy.newProxyInstance(ICreaturesController.class.getClassLoader(),
                new Class<?>[]{ICreaturesController.class}, (proxy, method, args) -> createController((EntityId) args[0]));
        new TaskManager(entityData, gameWorldController, mapController, stub(IObjectsController.class, Map.of()),
                creaturesController, stub(INavigationService.class, Map.of()),
                List.of(stub(IPlayerController.class, Map.of("getKeeper", new Keeper(false, Player.KEEPER1_ID)))),
                stub(ILevelInfo.class, Map.of("getLevelData", stub(IKwdFile.class, Map.of("getRoomByTerrain", RoomFixtures.room((short) 1, 0))))),
                stub(IEntityPositionLookup.class, Map.of()), Map.of());
    }

    @Test
    void buildingARoomSendsCreaturesOnTheirWayBackToIdle() {
        playerActions.onBuild(Player.KEEPER1_ID, List.of(new Point(1, 1)));

        assertEquals(List.of(walking), sentToIdle);
    }

    @Test
    void sellingARoomSendsCreaturesOnTheirWayBackToIdle() {
        playerActions.onSold(Player.KEEPER1_ID, List.of(new Point(1, 1)));

        assertEquals(List.of(walking), sentToIdle);
    }

    private EntityId createCreature(short ownerId, boolean atTarget) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new Owner(ownerId, ownerId),
                new TaskComponent(entityId.getId(), null, new Point(5, 5), TaskType.TRAIN, 0, false));
        atTaskTarget.put(entityId, atTarget);
        return entityId;
    }

    private ICreatureController createController(EntityId entityId) {
        StateMachine<?, ?> stateMachine = (StateMachine<?, ?>) Proxy.newProxyInstance(StateMachine.class.getClassLoader(),
                new Class<?>[]{StateMachine.class}, (proxy, method, args) -> switch (method.getName()) {
            case "isInState" ->
                args[0] == CreatureState.WORK;
            case "changeState" ->
                sentToIdle.add(args[0] == CreatureState.IDLE ? entityId : null);
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
        return stub(ICreatureController.class, Map.of("isWorker", false, "getStateMachine", stateMachine,
                "isAtAssignedTaskTarget", atTaskTarget.get(entityId)));
    }

    /**
     * Answers the given methods, void methods do nothing and anything else
     * fails the test
     */
    private static <T> T stub(Class<T> type, Map<String, Object> answers) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (answers.containsKey(method.getName())) {
                return answers.get(method.getName());
            }
            return switch (method.getName()) {
                case "hashCode" ->
                    System.identityHashCode(proxy);
                case "equals" ->
                    proxy == args[0];
                case "toString" ->
                    type.getSimpleName();
                default -> {
                    if (method.getReturnType() == void.class) {
                        yield null;
                    }
                    throw new UnsupportedOperationException(type.getSimpleName() + "." + method.getName());
                }
            };
        }));
    }

}
