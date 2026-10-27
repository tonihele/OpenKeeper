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

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Hypnotised;
import toniarts.openkeeper.game.component.Mana;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.component.Unconscious;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.HealthSystem;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.logic.PossessedMovementSystem;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.PossessionFixtures;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A possessed creature uses its first person abilities as its keeper presses
 * their keys
 *
 * @author OpenKeeper
 */
class PossessedAbilityTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;

    private EntityData entityData;
    private Creature creature;
    private Set<EntityId> workers;
    private Set<Point> solidTiles;
    private Map<Short, Creature> creatures;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private Map<EntityId, CreatureState> states;
    private Map<EntityId, EntityId> followTargets;
    private double gameTime;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        gameTime = 10;
        workers = new HashSet<>();
        solidTiles = new HashSet<>();
        gameSettings = new HashMap<>();
        states = new HashMap<>();
        followTargets = new HashMap<>();

        // A Knight can be hypnotised, King Reginald can't and Prince Felix is immune to turncoat
        creatures = new HashMap<>();
        creatures.put((short) 13, ShotFixtures.creature((short) 13, EnumSet.of(Creature.CreatureFlag.CAN_BE_HYPNOTIZED),
                EnumSet.noneOf(Creature.CreatureFlag3.class)));
        creatures.put((short) 21, ShotFixtures.creature((short) 21, EnumSet.noneOf(Creature.CreatureFlag.class),
                EnumSet.noneOf(Creature.CreatureFlag3.class)));
        creatures.put((short) 26, ShotFixtures.creature((short) 26, EnumSet.of(Creature.CreatureFlag.CAN_BE_HYPNOTIZED,
                Creature.CreatureFlag.IS_IMMUNE_TO_TURNCOAT), EnumSet.noneOf(Creature.CreatureFlag3.class)));

        // A Vampire, it doesn't fly on its own
        creature = ShotFixtures.creature((short) 8, EnumSet.noneOf(Creature.CreatureFlag.class), EnumSet.noneOf(Creature.CreatureFlag3.class));
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.TURN_TO_BAT, Creature.SpecialAbility.HYPNOTISE);
        us = createCreature(KEEPER, new Vector3f(10, 0, 2));
        entityData.setComponents(us, new Possessed(0, 0), new Mobile(false, false, false, 1));
    }

    @Test
    void vampireTurnsIntoABatThatFliesAndBack() {
        CreatureController controller = createController();

        controller.usePossessedAbility(1);

        assertEquals(Creature.SpecialAbility.TURN_TO_BAT, entityData.getComponent(us, PossessedAbility.class).ability);
        assertTrue(entityData.getComponent(us, Mobile.class).canFly);

        controller.usePossessedAbility(1);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertFalse(entityData.getComponent(us, Mobile.class).canFly);
    }

    @Test
    void secondAbilityDoesNotTurnIntoABat() {
        createController().usePossessedAbility(2);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
    }

    @Test
    void batTurnsBackWhenThePossessionEnds() {
        CreatureController controller = createController();
        controller.usePossessedAbility(1);

        controller.setPossession(false);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertFalse(entityData.getComponent(us, Mobile.class).canFly);
    }

    @Test
    void monkPraysInPlaceAndStops() {
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.PRAY, Creature.SpecialAbility.NONE);
        CreatureController controller = createController();

        controller.usePossessedAbility(1);
        controller.setPossessedMovement(new Vector2f(0, 1), 1, 0, PossessedMovement.SPEED_WALK);

        assertEquals(Creature.SpecialAbility.PRAY, entityData.getComponent(us, PossessedAbility.class).ability);
        assertEquals(Vector2f.ZERO, entityData.getComponent(us, PossessedMovement.class).direction);
        assertEquals(1, entityData.getComponent(us, PossessedMovement.class).rotation);

        controller.usePossessedAbility(1);
        controller.setPossessedMovement(new Vector2f(0, 1), 1, 0, PossessedMovement.SPEED_WALK);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertEquals(new Vector2f(0, 1), entityData.getComponent(us, PossessedMovement.class).direction);
    }

    @Test
    void prayingHealsOnceASecond() {
        Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings = new HashMap<>();
        for (Variable.MiscVariable.MiscType type : List.of(Variable.MiscVariable.MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                Variable.MiscVariable.MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                Variable.MiscVariable.MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            gameSettings.put(type, PossessionFixtures.miscVariable(type, 0));
        }
        gameSettings.put(Variable.MiscVariable.MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.MODIFY_CREATURE_HEALTH_WHILE_PRAYING_PER_SECOND, 33));
        entityData.setComponents(us, new Health(100, 1000), new PossessedAbility(Creature.SpecialAbility.PRAY, 0));
        double[] time = {0};
        HealthSystem healthSystem = new HealthSystem(entityData, null, gameSettings, null, null, List.of(), null, () -> time[0], null);

        time[0] += 0.5;
        healthSystem.processTick(0.5f);
        assertEquals(100, entityData.getComponent(us, Health.class).health);

        time[0] += 0.5;
        healthSystem.processTick(0.5f);
        time[0] += 0.5;
        healthSystem.processTick(0.5f);
        assertEquals(133, entityData.getComponent(us, Health.class).health);
    }

    @Test
    void bileDemonPicksUpTheNearestOwnWorkerInFrontAndHoldsIt() {
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.PICK_UP_WORKERS, Creature.SpecialAbility.NONE);
        ShotFixtures.setSize(creature, 1, 0.65f);
        EntityId worker = createWorker(KEEPER, new Vector3f(10, 0, 2.6f));
        createWorker(HERO, new Vector3f(10, 0, 2.4f));
        createCreature(KEEPER, new Vector3f(10, 0, 2.3f));
        createWorker(KEEPER, new Vector3f(10, 0, 1.5f));
        createWorker(KEEPER, new Vector3f(10, 0, 3.2f));
        createWorker(KEEPER, new Vector3f(10.7f, 0, 2.2f));

        createController().usePossessedAbility(1);

        assertEquals(worker, entityData.getComponent(us, PossessedAbility.class).entity);
        assertNull(entityData.getComponent(worker, CreatureAi.class));
        assertEquals(Creature.AnimationType.IN_HAND, entityData.getComponent(worker, CreatureViewState.class).state);

        // It is held half a tile in front, at the height of the Bile Demon
        entityData.setComponent(us, new PossessedMovement(Vector2f.ZERO, 0, 0, 0));
        PossessedMovementSystem movementSystem = new PossessedMovementSystem(entityData, createMapController(),
                createPositionLookup(), createCreaturesController());
        movementSystem.processTick(0.1f);
        assertEquals(new Vector3f(10, 0.65f, 2.5f), entityData.getComponent(worker, Position.class).position);
    }

    @Test
    void bileDemonThrowsTheWorkerForwardAndUp() {
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.PICK_UP_WORKERS, Creature.SpecialAbility.NONE);
        ShotFixtures.setSize(creature, 1, 0.65f);
        EntityId worker = createWorker(KEEPER, new Vector3f(10, 0, 2.6f));
        CreatureController controller = createController();
        controller.usePossessedAbility(1);

        controller.usePossessedAbility(1);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertEquals(new Vector3f(10, 0.65f, 2.5f), entityData.getComponent(worker, Position.class).position);
        CreatureFall fall = entityData.getComponent(worker, CreatureFall.class);
        assertEquals(new Vector3f(0, 0.25f, 2), fall.velocity);
        assertEquals(50, fall.damage);
        assertNull(entityData.getComponent(worker, Damage.class));
    }

    @Test
    void carriedWorkerFallsWhenThePossessionEnds() {
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.PICK_UP_WORKERS, Creature.SpecialAbility.NONE);
        EntityId worker = createWorker(KEEPER, new Vector3f(10, 0, 2.6f));
        CreatureController controller = createController();
        controller.usePossessedAbility(1);

        controller.setPossession(false);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertEquals(2.5f, entityData.getComponent(worker, Position.class).position.z);
        CreatureFall fall = entityData.getComponent(worker, CreatureFall.class);
        assertEquals(Vector3f.ZERO, fall.velocity);
        assertEquals(50, fall.damage);
    }

    @Test
    void vampireHypnotisesTheNearestEnemyInFrontAndInSight() {
        EntityId knight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 6));
        createCreature(KEEPER, (short) 13, new Vector3f(10, 0, 3));
        createCreature(HERO, (short) 21, new Vector3f(10, 0, 3.5f));
        createCreature(HERO, (short) 13, new Vector3f(10, 0, 1));
        createCreature(HERO, (short) 13, new Vector3f(12, 0, 3));
        createCreature(HERO, (short) 13, new Vector3f(12, 0, 5));
        solidTiles.add(new Point(11, 4));

        createController().usePossessedAbility(2);

        assertEquals(us, entityData.getComponent(knight, Hypnotised.class).entity);
        assertEquals(HERO, entityData.getComponent(knight, Hypnotised.class).ownerId);
        assertEquals(KEEPER, entityData.getComponent(knight, Owner.class).ownerId);

        // The creature's own tick starts the following, not this call from the render thread
        assertEquals(us, entityData.getComponent(knight, FollowTarget.class).entityId);
        assertNull(states.get(knight));
        assertEquals(1, entityData.getEntities(Hypnotised.class).size());
    }

    @Test
    void nearestCreatureImmuneToTurncoatIsNotHypnotised() {
        EntityId prince = createCreature(HERO, (short) 26, new Vector3f(10, 0, 3));
        EntityId knight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 4));

        createController().usePossessedAbility(2);

        assertNull(entityData.getComponent(prince, Hypnotised.class));
        assertNull(entityData.getComponent(knight, Hypnotised.class));
        assertEquals(HERO, entityData.getComponent(knight, Owner.class).ownerId);
    }

    @Test
    void keeperPaysTheManaDrainWhileTheCreatureIsHypnotised() {
        gameSettings.put(Variable.MiscVariable.MiscType.HYPNOTISM_MANA_DRAIN_PER_SECOND,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.HYPNOTISM_MANA_DRAIN_PER_SECOND, 5));
        EntityId knight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 4));
        CreatureController controller = createController();

        controller.usePossessedAbility(2);

        assertEquals(-5, entityData.getComponent(knight, Mana.class).manaGeneration);

        controller.usePossessedAbility(2);

        assertNull(entityData.getComponent(knight, Mana.class));
    }

    @Test
    void usingItAgainLetsTheCreatureGo() {
        EntityId knight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 4));
        CreatureController controller = createController();
        controller.usePossessedAbility(2);

        controller.usePossessedAbility(2);

        assertNull(entityData.getComponent(knight, Hypnotised.class));
        assertEquals(HERO, entityData.getComponent(knight, Owner.class).ownerId);
        assertNull(entityData.getComponent(knight, FollowTarget.class));
        assertNull(states.get(knight));
    }

    @Test
    void creatureIsLetGoWhenThePossessionEnds() {
        EntityId knight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 4));
        CreatureController controller = createController();
        controller.usePossessedAbility(2);

        controller.setPossession(false);

        assertNull(entityData.getComponent(knight, Hypnotised.class));
        assertEquals(HERO, entityData.getComponent(knight, Owner.class).ownerId);
        assertNull(entityData.getComponent(knight, FollowTarget.class));
        assertNull(states.get(knight));
    }

    @Test
    void deadCreatureIsNoLongerHypnotisedAndAnotherCanBe() {
        gameSettings.put(Variable.MiscVariable.MiscType.HYPNOTISM_MANA_DRAIN_PER_SECOND,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.HYPNOTISM_MANA_DRAIN_PER_SECOND, 5));
        for (Variable.MiscVariable.MiscType type : List.of(Variable.MiscVariable.MiscType.CREATURE_DYING_STATE_DURATION_SECONDS,
                Variable.MiscVariable.MiscType.PRISON_MODIFY_CREATURE_HEALTH_PER_SECOND,
                Variable.MiscVariable.MiscType.MODIFY_HEALTH_OF_CREATURE_IN_LAIR_PER_SECOND)) {
            gameSettings.put(type, PossessionFixtures.miscVariable(type, 0));
        }
        EntityId knight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 4));
        CreatureController controller = createController();
        controller.usePossessedAbility(2);

        entityData.setComponent(knight, new Unconscious(0));
        new HealthSystem(entityData, null, gameSettings, null, null, List.of(), null, () -> gameTime,
                stub(GameEventService.class, (name, args) -> null)).processTick(0.25f);

        assertNull(entityData.getComponent(knight, Hypnotised.class));
        assertNull(entityData.getComponent(knight, Mana.class));

        EntityId otherKnight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 5));
        controller.usePossessedAbility(2);

        assertEquals(us, entityData.getComponent(otherKnight, Hypnotised.class).entity);
    }

    @Test
    void hypnotisedCreatureAttacksWhatWeHit() {
        EntityId knight = createCreature(HERO, (short) 13, new Vector3f(10, 0, 4));
        EntityId king = createCreature(HERO, (short) 21, new Vector3f(10, 0, 2.8f));
        CreatureController controller = createController();
        controller.usePossessedAbility(2);

        controller.executePossessedAttack();

        assertNotNull(entityData.getComponent(king, Damage.class));
        assertEquals(king, entityData.getComponent(knight, AttackTarget.class).entityId);
    }

    private EntityId createCreature(short ownerId, short creatureId, Vector3f position) {
        EntityId entity = createCreature(ownerId, position);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponent(entity, creatureComponent);
        return entity;
    }

    private EntityId createWorker(short ownerId, Vector3f position) {
        EntityId entity = createCreature(ownerId, position);
        entityData.setComponents(entity, new CreatureAi(0, CreatureState.WORK, (short) 1),
                new CreatureViewState((short) 1, 0, Creature.AnimationType.WALK));
        workers.add(entity);
        return entity;
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, position), new CreatureMeleeAttack(0, 10, 1, 0.5f));
        return entity;
    }

    private CreatureController createController() {
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null);
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getCreatureFirstPersonStats" -> new HashMap<>();
            case "getCreature" -> creatures.getOrDefault((Short) args[0], creature);
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwdFile;
            case "getPlayer" -> new Keeper(false, (Short) args[0]);
            default -> null;
        });

        return new CreatureController(us, entityData, creature, null, null, gameTimer, gameSettings, createCreaturesController(),
                createPositionLookup(), createMapController(), levelInfo, null, null, stub(GameEventService.class, (name, args) -> null));
    }

    private ICreaturesController createCreaturesController() {
        return stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null;
            case "createController" -> stub(ICreatureController.class, (controllerName, controllerArgs) -> switch (controllerName) {
                case "isWorker" -> workers.contains((EntityId) args[0]);
                case "getCreature" -> creature;
                case "getStateMachine" -> stub(StateMachine.class, (stateName, stateArgs) -> "changeState".equals(stateName)
                        ? states.put((EntityId) args[0], (CreatureState) stateArgs[0]) : null);
                case "setFollowTarget" -> followTargets.put((EntityId) args[0], (EntityId) controllerArgs[0]);
                default -> null;
            });
            default -> null;
        });
    }

    private IEntityPositionLookup createPositionLookup() {
        return stub(IEntityPositionLookup.class, (name, args) -> {
            if (!"getEntitiesInLocation".equals(name)) {
                return null;
            }
            List<EntityId> entities = new ArrayList<>();
            for (Entity entity : entityData.getEntities(Position.class)) {
                if (WorldUtils.vectorToPoint(entity.get(Position.class).position).equals(args[0])) {
                    entities.add(entity.getId());
                }
            }
            return entities;
        });
    }

    private IMapController createMapController() {
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> {
            if (!"getTile".equals(name)) {
                return null;
            }
            Point tile = args.length == 1 ? (Point) args[0] : new Point((Integer) args[0], (Integer) args[1]);
            return stub(IMapTileInformation.class, (tileName, tileArgs) -> "getLocation".equals(tileName) ? tile : null);
        });
        return stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "isSolid" -> solidTiles.contains((Point) args[0]);
            default -> null;
        });
    }

    @FunctionalInterface
    private interface Answer {

        Object answer(String name, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T stub(Class<T> type, Answer answer) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            Object result = answer.answer(method.getName(), args);
            if (result == null && method.getReturnType() == boolean.class) {
                return false;
            }
            return result;
        });
    }

}
