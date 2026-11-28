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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureImprisoned;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SummonedReaper;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.PayDayFixtures;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.SummonReaperFixtures;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A Horned Reaper picks his prey over the whole map and hunts it down, also
 * when it runs from him and is far out of his senses (DKII.EXE 0x45E1B0). A
 * summoned one does so by its own creature states (DKII.EXE 0x4748E0).
 */
class HornedReaperHuntTest {

    private static final short PLAYER_ID = 3;
    private static final short ENEMY_ID = 4;
    private static final short ALLY_ID = 5;
    private static final Point REAPER_TILE = new Point(10, 10);
    private static final Point ENEMY_TILE = new Point(30, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Keeper> keepers = new HashMap<>();
    private final Set<Point> unreachableTiles = new HashSet<>();
    private final EntityId reaper = entityData.createEntity();
    private final EntityId enemy = entityData.createEntity();
    private double now;

    HornedReaperHuntTest() {
        Keeper keeper = new Keeper(false, PLAYER_ID);
        keeper.createAlliance(ALLY_ID);
        keepers.put(PLAYER_ID, keeper);
        keepers.put(ENEMY_ID, new Keeper(false, ENEMY_ID));
        keepers.put(ALLY_ID, new Keeper(false, ALLY_ID));
        keepers.put(Player.GOOD_PLAYER_ID, new Keeper(false, Player.GOOD_PLAYER_ID));
    }

    @AfterEach
    void close() {
        entityData.close();
    }

    @Test
    void summonedReaperGoesHuntingWhereOthersLookForWork() {
        CreatureController controller = summonedReaper();
        addCreature(ENEMY_ID, new Point(16, 10));
        EntityId nearest = addCreature(ENEMY_ID, new Point(13, 10));

        // Idle is where a creature picks its job, the Reaper's own state machine takes it from there
        entityData.setComponent(reaper, new Fearless());
        controller.getStateMachine().changeState(CreatureState.IDLE);
        assertEquals(CreatureState.REAPER_HUNT, controller.getStateMachine().getCurrentState());
        hunt(controller);

        assertEquals(CreatureState.FIGHT, controller.getStateMachine().getCurrentState());
        assertEquals(nearest, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void arrivingReaperStandsForTwentyTicksAndRoarsBeforeItHunts() {
        CreatureController controller = summonedReaper();
        controller.getStateMachine().changeState(CreatureState.REAPER_ARRIVE);
        entityData.setComponent(reaper, new CreatureAi(now, CreatureState.REAPER_ARRIVE, (short) 27));

        // 12 ticks of waiting and 8 of fading in, at 4 ticks a second
        now = 5;
        controller.getStateMachine().update();
        assertEquals(CreatureState.REAPER_ARRIVE, controller.getStateMachine().getCurrentState());
        now = 5.25;
        controller.getStateMachine().update();
        assertEquals(CreatureState.REAPER_ROAR, controller.getStateMachine().getCurrentState());
        entityData.setComponent(reaper, new CreatureAi(now, CreatureState.REAPER_ROAR, (short) 27));

        // The roar here has no animation, so it is over at once
        now = 5.5;
        controller.getStateMachine().update();
        assertEquals(CreatureState.REAPER_HUNT, controller.getStateMachine().getCurrentState());
    }

    @Test
    void heroesAreEnemiesToo() {
        CreatureController controller = summonedReaper();
        EntityId hero = addCreature(Player.GOOD_PLAYER_ID, new Point(14, 10));

        hunt(controller);

        assertEquals(hero, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void leavesWhenOnlyOwnAlliedAndNeutralCreaturesAreAround() {
        CreatureController controller = summonedReaper();
        addCreature(PLAYER_ID, new Point(11, 10));
        addCreature(ALLY_ID, new Point(12, 10));
        addCreature(Player.NEUTRAL_PLAYER_ID, new Point(13, 10));

        hunt(controller);

        assertEquals(CreatureState.REAPER_LEAVE, controller.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(reaper, AttackTarget.class));
    }

    @Test
    void skipsEnemiesItCanNotReachOrAttack() {
        CreatureController controller = summonedReaper();
        Point walledIn = new Point(11, 10);
        unreachableTiles.add(walledIn);
        addCreature(ENEMY_ID, walledIn);
        entityData.setComponent(addCreature(ENEMY_ID, new Point(12, 10)), new Unconscious(0));
        entityData.setComponent(addCreature(ENEMY_ID, new Point(13, 10)), new CreatureImprisoned(0, 0));
        entityData.setComponent(addCreature(ENEMY_ID, new Point(14, 10)), new SummonedReaper(0, 0, 0, SummonedReaper.NOT_YET, 5));
        EntityId target = addCreature(ENEMY_ID, new Point(18, 10));

        hunt(controller);

        assertEquals(target, entityData.getComponent(reaper, AttackTarget.class).entityId);
    }

    @Test
    void goesForTheEnemyDungeonHeartWhenThereAreNoEnemyCreatures() {
        CreatureController controller = summonedReaper();
        Point heart = new Point(30, 12);
        keepers.get(ENEMY_ID).setDungeonHeartLocation(heart);
        keepers.get(ALLY_ID).setDungeonHeartLocation(new Point(12, 10));

        huntUntil(controller, () -> entityData.getComponent(reaper, Navigation.class) != null);

        assertEquals(heart, entityData.getComponent(reaper, Navigation.class).target);
        assertEquals(CreatureState.REAPER_WANDER, controller.getStateMachine().getCurrentState());

        // It walks there before it decides again
        controller.getStateMachine().update();
        assertEquals(CreatureState.REAPER_WANDER, controller.getStateMachine().getCurrentState());
    }

    @Test
    void goesForTheHeartThatIsNearestTheWayDungeonKeeperIIMeasuresIt() {
        CreatureController controller = summonedReaper();
        keepers.get(ENEMY_ID).setDungeonHeartLocation(new Point(13, 10));
        Point below = new Point(10, 14);
        keepers.get(Player.GOOD_PLAYER_ID).setDungeonHeartLocation(below);

        // 3 * 3 = 9 for the one across, 2 * 4 = 8 for the one further away below
        huntUntil(controller, () -> entityData.getComponent(reaper, Navigation.class) != null);

        assertEquals(below, entityData.getComponent(reaper, Navigation.class).target);
    }

    @Test
    void leavesWhenItCanReachNothing() {
        CreatureController controller = summonedReaper();
        Point walledIn = new Point(12, 10);
        unreachableTiles.add(walledIn);
        addCreature(ENEMY_ID, walledIn);
        Point heart = new Point(30, 12);
        keepers.get(ENEMY_ID).setDungeonHeartLocation(heart);
        unreachableTiles.add(heart);
        keepers.get(Player.GOOD_PLAYER_ID).setDungeonHeartLocation(new Point(12, 12));
        keepers.get(Player.GOOD_PLAYER_ID).setDestroyed(PLAYER_ID);

        hunt(controller);

        assertEquals(CreatureState.REAPER_LEAVE, controller.getStateMachine().getCurrentState());
        assertNull(entityData.getComponent(reaper, AttackTarget.class));
        assertNull(entityData.getComponent(reaper, Navigation.class));
    }

    @Test
    void huntsAFleeingEnemyAcrossTheMap() {
        CreatureController controller = reaperController(SummonReaperFixtures.creature((short) 27, Creature.CreatureFlag.IS_HORNY));
        entityData.setComponents(enemy, new CreatureComponent(), new Owner(ENEMY_ID, ENEMY_ID), new Health(100, 100),
                new Position(0, WorldUtils.pointToVector3f(ENEMY_TILE)), new CreatureAi(0, CreatureState.FLEE, (short) 1));
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

    private CreatureController summonedReaper() {
        CreatureController controller = reaperController(SummonReaperFixtures.creature((short) 27, Creature.CreatureFlag.IS_HORNY));
        entityData.setComponent(reaper, new SummonedReaper(0, 0, 100));
        controller.getStateMachine().changeState(CreatureState.REAPER_HUNT);
        return controller;
    }

    /**
     * Lets the Reaper decide until it does something else than wander, which
     * it does one time in three
     */
    private void hunt(CreatureController controller) {
        huntUntil(controller, () -> controller.getStateMachine().getCurrentState() != CreatureState.REAPER_HUNT
                && controller.getStateMachine().getCurrentState() != CreatureState.REAPER_WANDER);
    }

    private void huntUntil(CreatureController controller, BooleanSupplier done) {
        for (int i = 0; i < 1000 && !done.getAsBoolean(); i++) {
            controller.getStateMachine().update();
        }
    }

    private EntityId addCreature(short ownerId, Point tile) {
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(100, 100),
                new Position(0, WorldUtils.pointToVector3f(tile)));
        return entityId;
    }

    private CreatureController reaperController(Creature reaperDefinition) {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = reaperDefinition.getCreatureId();
        entityData.setComponents(reaper, creatureComponent, new Owner(PLAYER_ID, PLAYER_ID), new Health(1000, 1000),
                new Position(0, WorldUtils.pointToVector3f(REAPER_TILE)), new CreatureMeleeAttack(0, 100, 1, 1));

        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> keepers.get((Short) args[0]);
            case "getPlayers" -> keepers;
            case "getLevelData" -> proxy(IKwdFile.class, (kwdMethod, kwdArgs) -> switch (kwdMethod) {
                case "getGameLevel" -> PayDayFixtures.gameLevel(4);
                default -> throw new UnsupportedOperationException(kwdMethod);
            });
            default -> throw new UnsupportedOperationException(method);
        });
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getSensedEntities" -> Set.of();
            case "getEntitiesInLocation" -> List.of();
            case "getBlockingDoor" -> null;
            default -> throw new UnsupportedOperationException(method);
        });
        INavigationService navigationService = proxy(INavigationService.class, (method, args) -> switch (method) {
            case "findRandomAccessibleTile" -> null;
            case "findPath" -> {
                if (unreachableTiles.contains((Point) args[1])) {
                    yield null;
                }
                DefaultGraphPath<IMapTileInformation> path = new DefaultGraphPath<>();
                path.add(proxy(IMapTileInformation.class, (tileMethod, tileArgs) -> switch (tileMethod) {
                    case "getLocation" -> args[1];
                    default -> throw new UnsupportedOperationException(tileMethod);
                }));
                yield path;
            }
            case "isReachable" -> !unreachableTiles.contains((Point) args[1]);
            default -> throw new UnsupportedOperationException(method);
        });
        ICreaturesController creaturesController = proxy(ICreaturesController.class, (method, args) -> switch (method) {
            case "createController" -> proxy(ICreatureController.class, (targetMethod, targetArgs) -> switch (targetMethod) {
                case "getEntityId" -> args[0];
                default -> throw new UnsupportedOperationException(targetMethod);
            });
            default -> throw new UnsupportedOperationException(method);
        });

        return new CreatureController(reaper, entityData, reaperDefinition, navigationService, null, () -> now,
                Map.of(), creaturesController, lookup, null, levelInfo, null, null,
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
