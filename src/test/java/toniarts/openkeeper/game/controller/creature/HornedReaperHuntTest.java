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
package toniarts.openkeeper.game.controller.creature;

import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.SummonReaperFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A Horned Reaper picks his prey over the whole map and hunts it down, also
 * when it runs from him and is far out of his senses (DKII.EXE 0x45E1B0)
 */
class HornedReaperHuntTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final Point ENEMY_TILE = new Point(30, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final EntityId reaper = entityData.createEntity();
    private final EntityId enemy = entityData.createEntity();

    @AfterEach
    void close() {
        entityData.close();
    }

    @Test
    void huntsAFleeingEnemyAcrossTheMap() {
        CreatureController controller = reaperController(SummonReaperFixtures.creature((short) 27, Creature.CreatureFlag.IS_HORNY));
        entityData.setComponent(reaper, new AttackTarget(enemy));
        controller.getStateMachine().changeState(CreatureState.FIGHT);

        var target = controller.getAttackTarget();
        assertNotNull(target);
        assertEquals(enemy, target.getEntityId());

        controller.getStateMachine().update();

        assertEquals(CreatureState.FIGHT, controller.getStateMachine().getCurrentState());
        assertEquals(enemy, entityData.getComponent(reaper, AttackTarget.class).entityId);
        Navigation navigation = entityData.getComponent(reaper, Navigation.class);
        assertNotNull(navigation);
        assertEquals(ENEMY_TILE, navigation.target);
    }

    private CreatureController reaperController(Creature reaperDefinition) {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = reaperDefinition.getCreatureId();
        entityData.setComponents(reaper, creatureComponent, new Owner(PLAYER_ID, PLAYER_ID), new Health(1000, 1000),
                new Position(0, WorldUtils.pointToVector3f(new Point(2, 10))), new CreatureMeleeAttack(0, 100, 1, 1));

        // The enemy runs away, far beyond what the Reaper can see or hear
        entityData.setComponents(enemy, new CreatureComponent(), new Owner(ENEMY_ID, ENEMY_ID), new Health(100, 100),
                new Position(0, WorldUtils.pointToVector3f(ENEMY_TILE)), new CreatureAi(0, CreatureState.FLEE, (short) 1));

        Keeper keeper = new Keeper(false, PLAYER_ID);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> keeper;
            default -> throw new UnsupportedOperationException(method);
        });
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getSensedEntities" -> Set.of();
            case "getEntitiesInLocation" -> List.of();
            default -> throw new UnsupportedOperationException(method);
        });
        INavigationService navigationService = proxy(INavigationService.class, (method, args) -> switch (method) {
            case "findPath" -> {
                DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
                path.add(proxy(IMapTileInformation.class, (tileMethod, tileArgs) -> switch (tileMethod) {
                    case "getLocation" -> args[1];
                    default -> throw new UnsupportedOperationException(tileMethod);
                }));
                yield path;
            }
            default -> throw new UnsupportedOperationException(method);
        });
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "createController" -> proxy(ICreatureController.class, (targetMethod, targetArgs) -> switch (targetMethod) {
                case "getEntityId" -> args[0];
                default -> throw new UnsupportedOperationException(targetMethod);
            });
            default -> throw new UnsupportedOperationException(method);
        });

        return new CreatureController(reaper, entityData, reaperDefinition, navigationService, null, () -> 0,
                null, creaturesController, lookup, null, levelInfo, null, null,
                proxy(GameEventService.class, (method, args) -> null));
    }

    @FunctionalInterface
    private interface Invocation {

        Object call(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation call) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (object, method, args) -> switch (method.getName()) {
                    case "toString" -> type.getSimpleName();
                    case "hashCode" -> System.identityHashCode(object);
                    case "equals" -> object == args[0];
                    default -> call.call(method.getName(), args);
                });
    }

}
