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
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedMovement;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
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
    private double gameTime;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        gameTime = 10;
        workers = new HashSet<>();
        solidTiles = new HashSet<>();

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
    void bileDemonThrowsTheWorkerAhead() {
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.PICK_UP_WORKERS, Creature.SpecialAbility.NONE);
        ShotFixtures.setSize(creature, 1, 0.65f);
        EntityId worker = createWorker(KEEPER, new Vector3f(10, 0, 2.6f));
        CreatureController controller = createController();
        controller.usePossessedAbility(1);

        controller.usePossessedAbility(1);

        assertNull(entityData.getComponent(us, PossessedAbility.class));
        assertEquals(new Vector3f(10, 0.65f, 4.5f), entityData.getComponent(worker, Position.class).position);
        assertNotNull(entityData.getComponent(worker, CreatureFall.class));
        assertEquals(50, entityData.getComponent(worker, Damage.class).damage);
    }

    @Test
    void thrownWorkerStopsShortOfAWall() {
        PossessionFixtures.setAbilities(creature, Creature.SpecialAbility.PICK_UP_WORKERS, Creature.SpecialAbility.NONE);
        EntityId worker = createWorker(KEEPER, new Vector3f(10, 0, 2.6f));
        solidTiles.add(new Point(10, 4));
        CreatureController controller = createController();
        controller.usePossessedAbility(1);

        controller.usePossessedAbility(1);

        assertEquals(3.25f, entityData.getComponent(worker, Position.class).position.z);
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
        assertNotNull(entityData.getComponent(worker, CreatureFall.class));
        assertEquals(50, entityData.getComponent(worker, Damage.class).damage);
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
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> "getCreatureFirstPersonStats".equals(name) ? new HashMap<>() : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> "getLevelData".equals(name) ? kwdFile : null);

        return new CreatureController(us, entityData, creature, null, null, gameTimer, new HashMap<>(), createCreaturesController(),
                createPositionLookup(), createMapController(), levelInfo, null, null, null);
    }

    private ICreaturesController createCreaturesController() {
        return stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null;
            case "createController" -> stub(ICreatureController.class, (controllerName, controllerArgs) -> switch (controllerName) {
                case "isWorker" -> workers.contains((EntityId) args[0]);
                case "getCreature" -> creature;
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
