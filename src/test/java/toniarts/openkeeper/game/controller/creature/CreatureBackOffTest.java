/*
 * Copyright (C) 2014-2017 OpenKeeper
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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.common.RoomInstance;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Fearless;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Threat;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.controller.room.IRoomController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature fighting enemies that threaten it more than its fear backs off
 * for a while, as DK II's level 1 Goblins do against a Giant
 */
class CreatureBackOffTest {

    private static final int GOBLIN_FEAR = 50;
    private static final int GIANT_THREAT = 160;

    private final EntityData data = new DefaultEntityData();
    private final Set<EntityId> creatures = new HashSet<>();
    private final Keeper keeper = new Keeper(false, Player.KEEPER1_ID);
    private final Map<MiscType, MiscVariable> settings = new EnumMap<>(MiscType.class);
    private final AtomicReference<Boolean> cornered = new AtomicReference<>(false);
    private final AtomicReference<Boolean> enemyAtGuardRoom = new AtomicReference<>(false);
    private final List<IRoomController> guardRooms = new ArrayList<>();
    private final AtomicReference<Boolean> withdraws = new AtomicReference<>(false);
    private final AtomicReference<Boolean> findsSomewhereToWithdrawTo = new AtomicReference<>(true);
    private final Set<Point> walkable = new HashSet<>();
    private double time;

    CreatureBackOffTest() {
        settings.put(MiscType.BACK_OFF_MINIMUM_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.BACK_OFF_MINIMUM_DURATION_SECONDS, 0));
        settings.put(MiscType.BACK_OFF_MAXIMUM_DURATION_SECONDS, CreatureSleepFixtures.variable(MiscType.BACK_OFF_MAXIMUM_DURATION_SECONDS, 30));
    }

    @AfterEach
    void close() {
        data.close();
    }

    @Test
    void takesThreeFightTurnsOverFearToScare() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        assertFalse(controller.isScared());
        assertFalse(controller.isScared());
        assertTrue(controller.isScared());
    }

    @Test
    void enemyFoughtFromADistanceStillScares() {

        // A Warlock casting at a Knight out of its sense range, as in DK II where it backs off before the first spell
        EntityId warlock = creature(Player.KEEPER1_ID, 10, 50, 20, 1.08f);
        EntityId knight = creature(Player.GOOD_PLAYER_ID, 16, 533, 213, 0.9f);
        creatures.remove(knight);
        fight(warlock, knight);
        CreatureController controller = controller(warlock);

        assertFalse(controller.isScared());
        assertFalse(controller.isScared());
        assertTrue(controller.isScared());
    }

    @Test
    void threatIsSharedBetweenTheTargetAndTheAttackers() {
        EntityId target = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId other = creature(Player.KEEPER1_ID, 12, GOBLIN_FEAR, 37, 1.26f);
        EntityId third = creature(Player.KEEPER1_ID, 13, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(target, giant);
        fight(other, giant);
        fight(third, giant);
        fight(giant, target);

        // Three attackers and a target, a quarter each. The target gets two shares
        CreatureController otherController = controller(other);
        CreatureController targetController = controller(target);
        for (int i = 0; i < 5; i++) {
            assertFalse(otherController.isScared());
            targetController.isScared();
        }
        assertTrue(targetController.isScared());
        assertFalse(controller(giant).isScared());
    }

    @Test
    void workerIsScaredAtOnceWhenAnEnemyFighterTurnsOnIt() {
        EntityId imp = creature(Player.KEEPER1_ID, 10, 5, 2, 1.84f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        CreatureController controller = controller(imp, CreatureSleepFixtures.worker());

        assertFalse(controller.isScared());
        fight(giant, imp);
        assertTrue(controller.isScared());
    }

    @Test
    void workerIsNotScaredAtOnceByAnEnemyImp() {
        EntityId imp = creature(Player.KEEPER1_ID, 10, 5, 2, 1.84f);
        EntityId enemyImp = creature(Player.GOOD_PLAYER_ID, 11, 5, 2, 1.84f);
        data.getComponent(enemyImp, CreatureComponent.class).creatureId = 1;
        fight(enemyImp, imp);
        CreatureController controller = controller(imp, CreatureSleepFixtures.worker());

        for (int i = 0; i < 5; i++) {
            assertFalse(controller.isScared());
        }
    }

    @Test
    void ownHeartNearbyLightensTheThreat() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        keeper.setDungeonHeartLocation(new Point(10, 7));
        for (int i = 0; i < 5; i++) {
            assertFalse(controller.isScared());
        }
        keeper.setDungeonHeartLocation(new Point(10, 6));
        controller.isScared();
        controller.isScared();
        assertTrue(controller.isScared());
    }

    @Test
    void backOffLastsLongerTheLessScared() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        // Threat 160 is three times the fear: 30 turns, at 4 turns a second
        controller.startBackOff();
        time = 7.25;
        assertFalse(controller.isBackOffOver());
        time = 7.5;
        assertTrue(controller.isBackOffOver());

        // Near the heart the threat is 64, just over the fear: 90 turns
        keeper.setDungeonHeartLocation(new Point(10, 6));
        controller.startBackOff();
        time += 22.25;
        assertFalse(controller.isBackOffOver());
        time += 0.25;
        assertTrue(controller.isBackOffOver());
    }

    @Test
    void noBackingOffFromAFasterEnemy() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 2f);
        fight(goblin, giant);
        fight(giant, goblin);
        CreatureController controller = controller(goblin);

        controller.startBackOff();

        assertTrue(controller.isBackOffOver());
    }

    @Test
    void fearlessCreatureIsNeverScared() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 11, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);
        data.setComponent(goblin, new Fearless(null));
        CreatureController controller = controller(goblin);

        for (int i = 0; i < 5; i++) {
            assertFalse(controller.isScared());
        }
    }

    @Test
    void tooScaredToStepBackWalksAway() {

        // The Knight's threat is four times the Warlock's fear, DK II's Warlocks walk away at once
        EntityId warlock = creature(Player.KEEPER1_ID, 10, 50, 20, 1.08f);
        EntityId knight = creature(Player.GOOD_PLAYER_ID, 11, 533, 213, 0.9f);
        fight(warlock, knight);
        fight(knight, warlock);
        EntityId goblin = creature(Player.KEEPER1_ID, 12, GOBLIN_FEAR, 37, 1.26f);
        EntityId giant = creature(Player.GOOD_PLAYER_ID, 13, 400, GIANT_THREAT, 0.9f);
        fight(goblin, giant);
        fight(giant, goblin);

        assertFalse(controller(warlock).canStepBack());
        assertTrue(controller(goblin).canStepBack());
    }

    @Test
    void creatureThatWalkedAwayTakesNoShare() {

        // Two Warlocks cast at a Knight and one has walked away, so the other takes the Knight's whole threat
        EntityId warlock = creature(Player.KEEPER1_ID, 10, 50, 20, 1.08f);
        EntityId walkedAway = creature(Player.KEEPER1_ID, 12, 50, 20, 1.08f);
        EntityId knight = creature(Player.GOOD_PLAYER_ID, 11, 533, 213, 0.9f);
        fight(warlock, knight);
        fight(walkedAway, knight);
        fight(knight, warlock);
        CreatureController controller = controller(warlock);
        assertTrue(controller.canStepBack());

        data.setComponent(walkedAway, new CreatureAi(0, CreatureState.FLEE, (short) 5));
        assertFalse(controller.canStepBack());
    }

    @Test
    void scaredCasterStepsBackCastingOrWalksAwayCuttingItShort() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.CAST_SPELL);
        AtomicReference<Boolean> stepsBack = new AtomicReference<>(true);
        ICreatureController creature = creature(state, new AtomicReference<>(true), new AtomicReference<>(false),
                new AtomicReference<>(true), stepsBack);

        CreatureState.CAST_SPELL.update(creature);
        assertEquals(CreatureState.CAST_SPELL, state.get());

        stepsBack.set(false);
        CreatureState.CAST_SPELL.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());
    }

    @Test
    void scaredFighterStepsBackSwingingOrWalksAwayCuttingItShort() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.MELEE_ATTACK);
        AtomicReference<Boolean> stepsBack = new AtomicReference<>(true);
        ICreatureController creature = creature(state, new AtomicReference<>(true), new AtomicReference<>(false),
                new AtomicReference<>(true), stepsBack);

        CreatureState.MELEE_ATTACK.update(creature);
        assertEquals(CreatureState.MELEE_ATTACK, state.get());

        stepsBack.set(false);
        CreatureState.MELEE_ATTACK.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());
    }

    @Test
    void scaredFighterBacksOffThenFightsAgainOrWithdraws() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.FIGHT);
        AtomicReference<Boolean> scared = new AtomicReference<>(true);
        AtomicReference<Boolean> over = new AtomicReference<>(false);
        AtomicReference<Boolean> canBackOff = new AtomicReference<>(true);
        ICreatureController creature = creature(state, scared, over, canBackOff, new AtomicReference<>(true));

        CreatureState.FIGHT.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());

        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());

        canBackOff.set(false);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FLEE, state.get());

        state.set(CreatureState.BACK_OFF);
        canBackOff.set(true);
        over.set(true);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FLEE, state.get());

        state.set(CreatureState.BACK_OFF);
        over.set(false);
        scared.set(false);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FIGHT, state.get());
    }

    @Test
    void scaredCreatureDoesNotWithdrawFromAnEnemyAtItsGuardRoom() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.BACK_OFF);
        AtomicReference<Boolean> scared = new AtomicReference<>(true);
        ICreatureController creature = creature(state, scared, new AtomicReference<>(true), new AtomicReference<>(true),
                new AtomicReference<>(true));
        enemyAtGuardRoom.set(true);

        // The back-off time is up, but it stays, and fights again once it isn't scared
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());

        scared.set(false);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FIGHT, state.get());
    }

    @Test
    void enemyIsAtOurGuardRoomWithinItsReportingDistanceOfTheRoomsBounds() {
        settings.put(MiscType.GUARD_ROOM_REPORTING_DISTANCE_TILES, CreatureSleepFixtures.variable(MiscType.GUARD_ROOM_REPORTING_DISTANCE_TILES, 7));
        EntityId goblin = creature(Player.KEEPER1_ID, 30, 100, 100, 1);
        EntityId knight = creature(Player.GOOD_PLAYER_ID, 31, 100, 100, 1);
        fight(goblin, knight);
        fight(knight, goblin);
        CreatureController controller = controller(goblin);
        assertFalse(controller.isEnemyAtOurGuardRoom());

        // A Guard Room from 20 to 24 across: the knight at 31 is just outside its bounds widened by 7
        RoomInstance roomInstance = new RoomInstance(new Room());
        roomInstance.addCoordinates(List.of(new Point(20, 8), new Point(24, 12)));
        guardRooms.add(proxy(IRoomController.class, (method, args) -> switch (method) {
            case "getRoomInstance" -> roomInstance;
            default -> throw new UnsupportedOperationException(method);
        }));
        assertFalse(controller.isEnemyAtOurGuardRoom());

        data.setComponent(knight, new Position(0, new Vector3f(30, WorldUtils.FLOOR_HEIGHT, 10)));
        assertTrue(controller.isEnemyAtOurGuardRoom());

        // The heroes have no Guard Rooms to hold
        assertFalse(controller(knight).isEnemyAtOurGuardRoom());
    }

    @Test
    void corneredCreatureFightsOnInsteadOfBackingOff() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.FIGHT);
        ICreatureController creature = creature(state, new AtomicReference<>(true), new AtomicReference<>(false), new AtomicReference<>(false),
                new AtomicReference<>(false));
        cornered.set(true);

        // DK II's fight state checks the fear only when the creature isn't cornered (0x45F030, 0x460C50)
        CreatureState.FIGHT.update(creature);
        assertEquals(CreatureState.FIGHT, state.get());

        // Backing off with no step back, cornered it fights again (0x4602E0)
        state.set(CreatureState.BACK_OFF);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FIGHT, state.get());
    }

    @Test
    void corneredWhenEveryTileItCanWalkToHoldsAnEnemyFightingIt() {
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 10, 1);
        EntityId east = creature(Player.GOOD_PLAYER_ID, 11, 100, GIANT_THREAT, 1);
        EntityId west = creature(Player.GOOD_PLAYER_ID, 9, 100, GIANT_THREAT, 1);
        walkable.addAll(List.of(new Point(9, 10), new Point(11, 10)));
        fight(east, goblin);

        assertFalse(controller(goblin).isCornered(), "The west tile is free");

        fight(west, goblin);
        assertTrue(controller(goblin).isCornered());
    }

    @Test
    void badlyHurtCreatureFarFromItsHeartWithdrawsToIt() {

        // Below a sixth of its health and more than 5 tiles from the heart along either axis (DK II 0x45EDA0, 0x48F250)
        EntityId goblin = creature(Player.KEEPER1_ID, 10, GOBLIN_FEAR, 37, 1.26f);
        keeper.setDungeonHeartLocation(new Point(4, 12));
        data.setComponent(goblin, new Health(166, 1000));
        assertFalse(controller(goblin).shouldWithdrawToHeart());

        data.setComponent(goblin, new Health(165, 1000));
        assertTrue(controller(goblin).shouldWithdrawToHeart());

        keeper.setDungeonHeartLocation(new Point(5, 15));
        assertFalse(controller(goblin).shouldWithdrawToHeart(), "5 tiles away");

        keeper.setDungeonHeartLocation(new Point(10, 16));
        assertTrue(controller(goblin).shouldWithdrawToHeart());

        data.setComponent(goblin, new Fearless(null));
        assertFalse(controller(goblin).shouldWithdrawToHeart());
    }

    @Test
    void badlyHurtHeroLeavesTheHeartWithdrawToTheLairCheck() {
        EntityId knight = creature(Player.GOOD_PLAYER_ID, 10, 533, 213, 0.9f);
        data.setComponent(knight, new Health(1, 1000));

        assertFalse(controller(knight).shouldWithdrawToHeart());
    }

    @Test
    void badlyHurtCreatureWithdrawsFromTheFightBeforeWeighingItsFear() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.FIGHT);
        ICreatureController creature = creature(state, new AtomicReference<>(true), new AtomicReference<>(false),
                new AtomicReference<>(true), new AtomicReference<>(true));
        withdraws.set(true);

        // DK II's fight state withdraws before it weighs the fear (0x45F030 calls 0x45EDA0, then 0x45EC80)
        CreatureState.FIGHT.update(creature);
        assertEquals(CreatureState.FLEE, state.get());

        // Cornered, it fights on (0x460C50 comes first)
        state.set(CreatureState.FIGHT);
        cornered.set(true);
        CreatureState.FIGHT.update(creature);
        assertEquals(CreatureState.FIGHT, state.get());
    }

    @Test
    void creatureWithNowhereToWithdrawToStaysInItsFightOrBackOff() {
        AtomicReference<CreatureState> state = new AtomicReference<>(CreatureState.FIGHT);
        AtomicReference<Boolean> scared = new AtomicReference<>(false);
        AtomicReference<Boolean> over = new AtomicReference<>(false);
        AtomicReference<Boolean> canBackOff = new AtomicReference<>(true);
        ICreatureController creature = creature(state, scared, over, canBackOff, new AtomicReference<>(true));
        withdraws.set(true);
        findsSomewhereToWithdrawTo.set(false);

        // DK II's fight state goes on to weigh the fear when the withdraw finds nowhere to go (0x45F030, 0x4611D0)
        CreatureState.FIGHT.update(creature);
        assertEquals(CreatureState.FIGHT, state.get());
        scared.set(true);
        CreatureState.FIGHT.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());

        // Backing off, its time up or with no way back, it stays backing off until it finds somewhere (0x4602E0)
        over.set(true);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());
        over.set(false);
        canBackOff.set(false);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.BACK_OFF, state.get());

        findsSomewhereToWithdrawTo.set(true);
        CreatureState.BACK_OFF.update(creature);
        assertEquals(CreatureState.FLEE, state.get());
    }

    private EntityId creature(short ownerId, float x, int fear, int threat, float runSpeed) {
        EntityId entity = data.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.fear = fear;
        creatureComponent.runSpeed = runSpeed;
        data.setComponents(entity, creatureComponent, new Threat(threat), new Health(1000, 1000),
                new Owner(ownerId, ownerId), new Position(0, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, 10)));
        creatures.add(entity);
        return entity;
    }

    private void fight(EntityId attacker, EntityId target) {
        data.setComponent(attacker, new AttackTarget(target));
    }

    private CreatureController controller(EntityId entity) {
        return controller(entity, CreatureSleepFixtures.sleeper(1));
    }

    private CreatureController controller(EntityId entity, Creature definition) {
        IEntityPositionLookup lookup = proxy(IEntityPositionLookup.class, (method, args) -> switch (method) {
            case "getSensedEntities" -> {
                Set<EntityId> sensed = new HashSet<>(creatures);
                sensed.remove(entity);
                yield sensed;
            }
            case "getEntitiesInLocation" -> creatures.stream()
                    .filter(creature -> WorldUtils.vectorToPoint(data.getComponent(creature, Position.class).position).equals(args[0])).toList();
            default -> throw new UnsupportedOperationException(method);
        });
        INavigationService navigation = proxy(INavigationService.class, (method, args) -> switch (method) {
            case "findPath" -> walkable.contains((Point) args[1]) ? new DefaultGraphPath<>() : null;
            default -> throw new UnsupportedOperationException(method);
        });
        IKwdFile kwd = proxy(IKwdFile.class, (method, args) -> switch (method) {
            case "getGameLevel" -> CreatureSleepFixtures.level(4);
            case "getCreature" -> (Short) args[0] == 1 ? CreatureSleepFixtures.worker() : CreatureSleepFixtures.sleeper(1);
            default -> throw new UnsupportedOperationException(method);
        });
        Keeper heroes = new Keeper(false, Player.GOOD_PLAYER_ID);
        ILevelInfo levelInfo = proxy(ILevelInfo.class, (method, args) -> switch (method) {
            case "getPlayer" -> (Short) args[0] == Player.KEEPER1_ID ? keeper : heroes;
            case "getLevelData" -> kwd;
            default -> throw new UnsupportedOperationException(method);
        });
        IMapController mapController = proxy(IMapController.class, (method, args) -> switch (method) {
            case "getRoomsByFunction" -> (Short) args[1] == Player.KEEPER1_ID ? guardRooms : List.of();
            default -> throw new UnsupportedOperationException(method);
        });
        return new CreatureController(entity, data, definition, navigation, null, () -> time,
                settings, null, lookup, mapController, levelInfo, null, null, null);
    }

    private ICreatureController creature(AtomicReference<CreatureState> state, AtomicReference<Boolean> scared,
            AtomicReference<Boolean> over, AtomicReference<Boolean> canBackOff, AtomicReference<Boolean> stepsBack) {
        StateMachine<?, ?> stateMachine = proxy(StateMachine.class, (method, args) -> switch (method) {
            case "changeState" -> {
                state.set((CreatureState) args[0]);
                yield null;
            }
            case "getCurrentState" -> state.get();
            default -> throw new UnsupportedOperationException(method);
        });
        AtomicReference<ICreatureController> self = new AtomicReference<>();
        self.set(proxy(ICreatureController.class, (method, args) -> switch (method) {
            case "getStateMachine" -> stateMachine;
            case "getAttackTarget" -> self.get();
            case "isScared" -> scared.get();
            case "isCornered" -> cornered.get();
            case "isEnemyAtOurGuardRoom" -> enemyAtGuardRoom.get();
            case "isWithinAttackDistance" -> true;
            case "stopCreature", "executeAttack", "getEntityId" -> null;
            case "isStopped", "turnToAttackTarget" -> true;
            case "isBackOffOver" -> over.get();
            case "backOff" -> canBackOff.get();
            case "withdrawToHeroLair" -> false;
            case "shouldWithdrawToHeart" -> withdraws.get();
            case "flee" -> {

                // Like the controller, one that withdraws goes to FLEE itself
                if (findsSomewhereToWithdrawTo.get()) {
                    state.set(CreatureState.FLEE);
                }
                yield findsSomewhereToWithdrawTo.get();
            }
            case "canStepBack" -> stepsBack.get();
            case "isStateTimeExceeded" -> false;
            case "releaseCastShot", "releaseMeleeHit" -> null;
            case "castSupportSpell", "hasPendingMeleeHit" -> false;
            default -> throw new UnsupportedOperationException(method);
        }));
        return self.get();
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
