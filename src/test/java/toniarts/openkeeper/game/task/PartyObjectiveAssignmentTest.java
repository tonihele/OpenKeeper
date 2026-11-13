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
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.creature.IPartyController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.objective.PartyObjectiveTaskFactory;
import toniarts.openkeeper.game.task.objective.PartyObjectiveTaskPlan;
import toniarts.openkeeper.game.task.objective.Wait;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartyObjectiveAssignmentTest {

    @Test
    void assignsSupportingActorBeforeLeaderAndRejectsFollowerOrders() {
        EntityData entityData = new DefaultEntityData();
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
        Creature imp = new Creature();
        PlayerCreatureControl creatureControl = new PlayerCreatureControl(keeper, imp, List.of(imp));
        IPlayerController player = stub(IPlayerController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getKeeper" -> keeper;
            case "getCreatureControl" -> creatureControl;
            default -> null;
        });
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (proxy, method, args) -> switch (method.getName()) {
            case "getHeight", "getWidth" -> 0;
            default -> null;
        });
        IMapController map = stub(IMapController.class, (proxy, method, args) ->
                "getMapData".equals(method.getName()) ? mapData : null);
        INavigationService navigation = stub(INavigationService.class, (proxy, method, args) ->
                "findPath".equals(method.getName()) ? new DefaultGraphPath<>() : null);

        ICreatureController[] leaderHolder = new ICreatureController[1];
        IPartyController party = stub(IPartyController.class, (proxy, method, args) -> switch (method.getName()) {
            case "isPartyLeader" -> args[0] == leaderHolder[0];
            default -> null;
        });
        List<String> assignmentOrder = new ArrayList<>();
        ICreatureController leader = creature("leader", party, assignmentOrder);
        ICreatureController actor = creature("actor", party, assignmentOrder);
        leaderHolder[0] = leader;

        PartyObjectiveTaskFactory factory = (creature, objective) -> new PartyObjectiveTaskPlan(List.of(
                new PartyObjectiveTaskPlan.Assignment(leader,
                        new Wait(navigation, map, new Point(3, 3), Player.GOOD_PLAYER_ID)),
                new PartyObjectiveTaskPlan.Assignment(actor,
                        new Wait(navigation, map, new Point(3, 3), Player.GOOD_PLAYER_ID))));
        TaskManager taskManager = new TaskManager(entityData, stub(IGameWorldController.class, (proxy, method, args) -> null),
                map, null, stub(ICreaturesController.class, (proxy, method, args) -> null), navigation,
                List.of(player), null, null, Map.of(), Map.of(Thing.HeroParty.Objective.STEAL_GOLD, factory));
        try {
            assertTrue(taskManager.assignObjectiveTask(leader, Thing.HeroParty.Objective.STEAL_GOLD));
            assertEquals(List.of("actor", "leader"), assignmentOrder);
            assertFalse(taskManager.assignObjectiveTask(actor, Thing.HeroParty.Objective.STEAL_GOLD));
            assertEquals(List.of("actor", "leader"), assignmentOrder);
        } finally {
            taskManager.stop();
        }
    }

    private static ICreatureController creature(String name, IPartyController party, List<String> assignmentOrder) {
        return stub(ICreatureController.class, (proxy, method, args) -> switch (method.getName()) {
            case "getParty" -> party;
            case "getEntityId" -> new EntityId(name.hashCode());
            case "setAssignedTask" -> {
                assignmentOrder.add(name);
                yield null;
            }
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> null;
        });
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(Object proxy, java.lang.reflect.Method method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("equals")) {
                return proxy == args[0];
            }
            if (method.getName().equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            Object result = answer.answer(proxy, method, args);
            if (result == null && method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }
}
