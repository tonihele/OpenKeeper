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
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameWorldController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerCreatureControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hero told to kill a keeper's creatures only looks at the creatures that
 * still exist
 */
class KillCreaturesObjectiveTest {

    @Test
    void creatureRemovedThisTickIsNotHunted() {
        EntityData entityData = new DefaultEntityData();
        Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
        Creature imp = new Creature();
        PlayerCreatureControl creatureControl = new PlayerCreatureControl(keeper, imp, List.of(imp));

        // The player creature system has not yet heard that the first creature is gone
        EntityId removed = entityData.createEntity();
        creatureControl.onCreatureAdded(removed, imp);
        EntityId alive = entityData.createEntity();
        entityData.setComponent(alive, new CreatureComponent());
        creatureControl.onCreatureAdded(alive, imp);

        Point preyTile = new Point(6, 2);
        ICreatureController prey = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> preyTile;
            case "getOwnerId" -> Player.KEEPER1_ID;
            default -> null;
        });
        ICreaturesController creatures = stub(ICreaturesController.class, (name, args) -> {
            if ("createController".equals(name)) {
                if (entityData.getComponent((EntityId) args[0], CreatureComponent.class) == null) {
                    throw new RuntimeException("Entity " + args[0] + " doesn't represent a creature!");
                }
                return prey;
            }
            return null;
        });
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> 0);
        IMapController map = stub(IMapController.class, (name, args) -> "getMapData".equals(name) ? mapData : null);
        INavigationService navigation = stub(INavigationService.class, (name, args) -> "findPath".equals(name) ? new DefaultGraphPath<>() : null);
        IPlayerController player = stub(IPlayerController.class, (name, args) -> switch (name) {
            case "getKeeper" -> keeper;
            case "getCreatureControl" -> creatureControl;
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> null);
        TaskManager taskManager = new TaskManager(entityData, stub(IGameWorldController.class, (name, args) -> null), map, null, creatures, navigation, List.of(player),
                levelInfo, null, Map.of());

        Task[] assigned = new Task[1];
        ICreatureController hero = stub(ICreatureController.class, (name, args) -> switch (name) {
            case "getCreatureCoordinates" -> new Point(2, 2);
            case "getOwnerId" -> Player.GOOD_PLAYER_ID;
            case "getObjectiveTargetPlayerId" -> Player.KEEPER1_ID;
            case "setAssignedTask" -> assigned[0] = (Task) args[0];
            default -> null;
        });

        assertTrue(taskManager.assignObjectiveTask(hero, Thing.HeroParty.Objective.KILL_CREATURES));
        assertEquals(WorldUtils.pointToVector2f(preyTile), assigned[0].getTarget(hero));
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "hashCode" -> {
                    return System.identityHashCode(proxy);
                }
                case "equals" -> {
                    return proxy == args[0];
                }
                default -> {
                }
            }
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }
}
