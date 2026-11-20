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

import com.jme3.math.Vector3f;
import com.simsilica.es.Entity;
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
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureViewState;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.FollowTarget;
import toniarts.openkeeper.game.component.Gold;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Hypnotised;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.Possessed;
import toniarts.openkeeper.game.component.PossessedAbility;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IMapController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.map.IMapDataInformation;
import toniarts.openkeeper.game.map.IMapTileInformation;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.PossessionFixtures;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.TerrainFixtures;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.tools.convert.map.Variable;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The melee attack of a creature its keeper possesses
 */
class PossessedAttackTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final short HORNY = 27;
    private static final int DAMAGE = 100;
    private static final float RECHARGE = 1.5f;
    private static final float RANGE = 0.5f;
    private static final Terrain FLOOR = TerrainFixtures.terrain((short) 1, TerrainFixtures.flags(), 0, 0, 0, (short) 0);
    private static final Terrain DIRT = TerrainFixtures.terrain((short) 2, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID), 0, 0, 0, (short) 0);
    private static final Terrain ROCK = TerrainFixtures.terrain((short) 3, TerrainFixtures.flags(Terrain.TerrainFlag.SOLID,
            Terrain.TerrainFlag.IMPENETRABLE), 0, 0, 0, (short) 0);

    private EntityData entityData;
    private Map<Point, Terrain> terrain;
    private List<Point> dugTiles;
    private Map<Variable.CreatureStats.StatType, Variable.CreatureFirstPerson> firstPersonStats;
    private Map<Variable.MiscVariable.MiscType, Variable.MiscVariable> gameSettings;
    private Map<EntityId, CreatureController> controllers;
    private double gameTime;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        terrain = new HashMap<>();
        dugTiles = new ArrayList<>();
        firstPersonStats = new HashMap<>();
        gameSettings = new HashMap<>();
        controllers = new HashMap<>();
        gameTime = 10;

        // At the middle of tile (5, 5), facing +z
        us = createCreature(KEEPER, new Vector3f(5, 0, 5));
        entityData.setComponent(us, new Possessed(0, 0));
    }

    @Test
    void hitsTheCreatureInFrontWithItsMeleeDamage() {
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
        assertEquals(Creature.AnimationType.MELEE_ATTACK, entityData.getComponent(us, CreatureViewState.class).state);
    }

    @Test
    void batCannotAttack() {
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));
        entityData.setComponent(us, new PossessedAbility(Creature.SpecialAbility.TURN_TO_BAT, 0));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(enemy, Damage.class));
        assertNull(entityData.getComponent(us, CreatureViewState.class));
    }

    @Test
    void firstPersonStatsRaiseTheDamage() {
        firstPersonStats.put(Variable.CreatureStats.StatType.MELEE_DAMAGE,
                PossessionFixtures.firstPersonStat(Variable.CreatureStats.StatType.MELEE_DAMAGE, 60));
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE * 160 / 100, entityData.getComponent(enemy, Damage.class).damage);
        assertEquals(DAMAGE, entityData.getComponent(us, CreatureMeleeAttack.class).damage);
    }

    @Test
    void firstPersonStatsSlowTheRecharge() {
        firstPersonStats.put(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS,
                PossessionFixtures.firstPersonStat(Variable.CreatureStats.StatType.MELEE_RECHARGE_TIME_SECONDS, 25));
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));
        CreatureController controller = createController();

        controller.executePossessedAttack();
        gameTime += RECHARGE;
        controller.executePossessedAttack();
        assertEquals(DAMAGE, entityData.getComponent(enemy, Damage.class).damage);

        gameTime += RECHARGE / 4;
        controller.executePossessedAttack();
        assertEquals(2 * DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
        assertEquals(RECHARGE, entityData.getComponent(us, CreatureMeleeAttack.class).rechargeTime);
    }

    @Test
    void firstPersonRechargeModifyScalesTheRecharge() {
        gameSettings.put(Variable.MiscVariable.MiscType.FIRST_PERSON_RECHARGE_MODIFY_PERCENTAGE,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.FIRST_PERSON_RECHARGE_MODIFY_PERCENTAGE, 50));
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));
        CreatureController controller = createController();

        controller.executePossessedAttack();
        gameTime += RECHARGE / 2;
        controller.executePossessedAttack();

        assertEquals(2 * DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
    }

    @Test
    void reachesTheNextTileNotOnlyTheMeleeRange() {
        EntityId nextTile = createCreature(HERO, new Vector3f(5, 0, 6.3f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(nextTile, Damage.class).damage);
    }

    @Test
    void doesNotReachPastTheNextTile() {
        EntityId twoTilesAway = createCreature(HERO, new Vector3f(5, 0, 6.6f));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(twoTilesAway, Damage.class));
    }

    @Test
    void hitsTheNearestCreatureInFrontOnly() {
        EntityId far = createCreature(HERO, new Vector3f(5, 0, 6.3f));
        EntityId near = createCreature(HERO, new Vector3f(5.3f, 0, 5.6f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(near, Damage.class).damage);
        assertNull(entityData.getComponent(far, Damage.class));
    }

    @Test
    void doesNotHitWhatIsBehindOrToTheSide() {
        EntityId behind = createCreature(HERO, new Vector3f(5, 0, 4.5f));
        EntityId side = createCreature(HERO, new Vector3f(6, 0, 5.1f));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(behind, Damage.class));
        assertNull(entityData.getComponent(side, Damage.class));
    }

    @Test
    void hitsNeutralCreaturesButNotOwnOnes() {
        EntityId own = createCreature(KEEPER, new Vector3f(5, 0, 5.5f));
        EntityId neutral = createCreature(Player.NEUTRAL_PLAYER_ID, new Vector3f(5, 0, 5.8f));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(own, Damage.class));
        assertEquals(DAMAGE, entityData.getComponent(neutral, Damage.class).damage);
    }

    @Test
    void hitsAnEnemyDoorInFront() {
        EntityId door = createDoor(HERO, new Vector3f(5.5f, 0, 6.3f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(door, Damage.class).damage);
        assertEquals(Creature.AnimationType.MELEE_ATTACK, entityData.getComponent(us, CreatureViewState.class).state);
    }

    @Test
    void doesNotHitOwnDoorsOrBlueprints() {
        EntityId own = createDoor(KEEPER, new Vector3f(5.5f, 0, 5.8f));
        EntityId blueprint = createDoor(HERO, new Vector3f(5.5f, 0, 6.3f));
        entityData.setComponent(blueprint, new DoorComponent((short) 1, false, true));

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(own, Damage.class));
        assertNull(entityData.getComponent(blueprint, Damage.class));
    }

    @Test
    void hitsTheNearerOfADoorAndACreature() {
        EntityId door = createDoor(HERO, new Vector3f(5.5f, 0, 5.9f));
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 6.3f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(door, Damage.class).damage);
        assertNull(entityData.getComponent(enemy, Damage.class));
    }

    @Test
    void swingsAgainOnlyOnceRecharged() {
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));
        CreatureController controller = createController();

        controller.executePossessedAttack();
        gameTime += RECHARGE / 2;
        controller.executePossessedAttack();
        assertEquals(DAMAGE, entityData.getComponent(enemy, Damage.class).damage);

        gameTime += RECHARGE / 2;
        controller.executePossessedAttack();
        assertEquals(2 * DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
    }

    @Test
    void doesNotSwingAtNothing() {
        createController().executePossessedAttack();

        assertNull(entityData.getComponent(us, CreatureMeleeAttack.class).attactStartTime);
        assertNull(entityData.getComponent(us, CreatureViewState.class));
    }

    @Test
    void quickClickIsKeptUntilItHasSwung() {
        createCreature(HERO, new Vector3f(5, 0, 5.8f));
        CreatureController controller = createController();

        controller.setPossessedAttack(true, 0);
        controller.setPossessedAttack(false, 0);
        assertFalse(entityData.getComponent(us, Possessed.class).attackHeld);
        assertTrue(entityData.getComponent(us, Possessed.class).isAttackPending());

        controller.executePossessedAttack();
        gameTime += RECHARGE;
        controller.setPossessedAttack(true, 0);
        controller.executePossessedAttack();
        controller.setPossessedAttack(false, 0);
        assertFalse(entityData.getComponent(us, Possessed.class).isAttackPending());
    }

    @Test
    void hitsAnEnemyTrapInFront() {
        EntityId trap = createTrap(HERO, new Vector3f(5.5f, 0, 6.3f), false);

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(trap, Damage.class).damage);
        assertEquals(Creature.AnimationType.MELEE_ATTACK, entityData.getComponent(us, CreatureViewState.class).state);
    }

    @Test
    void doesNotHitOwnOrHiddenTraps() {
        EntityId own = createTrap(KEEPER, new Vector3f(5.5f, 0, 5.8f), false);
        EntityId hidden = createTrap(HERO, new Vector3f(5.5f, 0, 6.3f), true);

        createController().executePossessedAttack();

        assertNull(entityData.getComponent(own, Damage.class));
        assertNull(entityData.getComponent(hidden, Damage.class));
    }

    @Test
    void ownCreatureInViewJoinsTheGroup() {
        EntityId friend = createCreature(KEEPER, new Vector3f(5, 0, 9));
        CreatureController controller = createController();

        controller.addPossessedFollower();
        controller.executePossessedAttack();

        assertEquals(us, entityData.getComponent(friend, FollowTarget.class).entityId);
        assertTrue(createController(friend).getStateMachine().isInState(CreatureState.FOLLOW));
        assertTrue(createController(friend).isInPossessedGroup());
        assertNull(entityData.getComponent(us, CreatureMeleeAttack.class).attactStartTime);
    }

    @Test
    void theGroupAttacksWhatThePossessedCreatureHits() {
        EntityId follower = createCreature(KEEPER, new Vector3f(5, 0, 4));
        entityData.setComponent(follower, new FollowTarget(us));
        EntityId bystander = createCreature(KEEPER, new Vector3f(4, 0, 4));
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.8f));

        createController().executePossessedAttack();

        assertEquals(enemy, entityData.getComponent(follower, AttackTarget.class).entityId);
        assertNull(entityData.getComponent(bystander, AttackTarget.class));
    }

    @Test
    void theCreatureClosestToWhereItLooksJoins() {
        EntityId ahead = createCreature(KEEPER, new Vector3f(5, 0, 10));
        EntityId aside = createCreature(KEEPER, new Vector3f(5.3f, 0, 6.5f));
        CreatureController controller = createController();

        controller.addPossessedFollower();
        controller.executePossessedAttack();

        assertEquals(us, entityData.getComponent(ahead, FollowTarget.class).entityId);
        assertNull(entityData.getComponent(aside, FollowTarget.class));
    }

    @Test
    void creaturesTooFarOrOffToTheSideDoNotJoin() {
        EntityId far = createCreature(KEEPER, new Vector3f(5, 0, 12.1f));
        EntityId side = createCreature(KEEPER, new Vector3f(6.5f, 0, 8));
        CreatureController controller = createController();

        controller.addPossessedFollower();
        controller.executePossessedAttack();

        assertNull(entityData.getComponent(far, FollowTarget.class));
        assertNull(entityData.getComponent(side, FollowTarget.class));
    }

    @Test
    void enemiesHornyAndOtherGroupsDoNotJoin() {
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 7));
        EntityId horny = createCreature(KEEPER, new Vector3f(5, 0, 8));
        CreatureComponent hornyComponent = entityData.getComponent(horny, CreatureComponent.class);
        hornyComponent.creatureId = HORNY;
        entityData.setComponent(horny, hornyComponent);
        EntityId leader = createCreature(KEEPER, new Vector3f(1, 0, 1));
        EntityId following = createCreature(KEEPER, new Vector3f(5, 0, 9));
        entityData.setComponent(following, new FollowTarget(leader));
        CreatureController controller = createController();

        controller.addPossessedFollower();
        controller.executePossessedAttack();

        assertNull(entityData.getComponent(enemy, FollowTarget.class));
        assertNull(entityData.getComponent(horny, FollowTarget.class));
        assertEquals(leader, entityData.getComponent(following, FollowTarget.class).entityId);
    }

    @Test
    void theGroupHasRoomForTheMaximumNumberOfFollowers() {
        gameSettings.put(Variable.MiscVariable.MiscType.MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS, 1));
        EntityId first = createCreature(KEEPER, new Vector3f(5, 0, 8));
        CreatureController controller = createController();
        controller.addPossessedFollower();
        controller.executePossessedAttack();

        EntityId second = createCreature(KEEPER, new Vector3f(5, 0, 7));
        controller.addPossessedFollower();
        controller.executePossessedAttack();

        assertEquals(us, entityData.getComponent(first, FollowTarget.class).entityId);
        assertNull(entityData.getComponent(second, FollowTarget.class));
    }

    @Test
    void hypnotisedCreatureIsNotInTheGroupAndDoesNotTakeASlot() {
        gameSettings.put(Variable.MiscVariable.MiscType.MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS,
                PossessionFixtures.miscVariable(Variable.MiscVariable.MiscType.MAXIMUM_GROUP_NUMBER_OF_FOLLOWERS, 1));
        EntityId hypnotised = createCreature(HERO, new Vector3f(1, 0, 1));
        entityData.setComponents(hypnotised, new Hypnotised(us, HERO), new FollowTarget(us));
        EntityId friend = createCreature(KEEPER, new Vector3f(5, 0, 8));
        CreatureController controller = createController();

        controller.addPossessedFollower();
        controller.executePossessedAttack();

        assertEquals(us, entityData.getComponent(friend, FollowTarget.class).entityId);
        assertFalse(createController(hypnotised).isInPossessedGroup());
        assertTrue(createController(friend).isInPossessedGroup());
    }

    @Test
    void endingThePossessionLetsGoOfTheHypnotisedOneThroughItsOwnRelease() {
        EntityId hypnotised = createCreature(KEEPER, new Vector3f(1, 0, 1));
        // Without health it is incapacitated, so the release does not need the idle state
        entityData.removeComponent(hypnotised, Health.class);
        entityData.setComponents(hypnotised, new Hypnotised(us, HERO), new FollowTarget(us));
        EntityId friend = createCreature(KEEPER, new Vector3f(5, 0, 8));
        CreatureController controller = createController();
        controller.addPossessedFollower();
        controller.executePossessedAttack();

        controller.setPossession(false);

        assertNull(entityData.getComponent(friend, FollowTarget.class));
        assertNull(entityData.getComponent(hypnotised, Hypnotised.class));
        assertEquals(HERO, entityData.getComponent(hypnotised, Owner.class).ownerId);
    }

    @Test
    void releasingTheButtonKeepsTheGroupRequest() {
        CreatureController controller = createController();

        controller.addPossessedFollower();
        controller.setPossessedAttack(false, 0);

        assertTrue(entityData.getComponent(us, Possessed.class).attackGroup);
    }

    @Test
    void theGroupBreaksUpWhenThePossessionEnds() {
        EntityId friend = createCreature(KEEPER, new Vector3f(5, 0, 8));
        CreatureController controller = createController();
        controller.addPossessedFollower();
        controller.executePossessedAttack();

        controller.setPossession(false);

        assertNull(entityData.getComponent(friend, FollowTarget.class));
        assertFalse(createController(friend).isInPossessedGroup());
    }

    @Test
    void possessedWorkerDigsTheTileItFaces() {
        makeWorker();
        terrain.put(new Point(6, 5), DIRT);

        // Facing mostly +x
        entityData.setComponent(us, new Position(1.2f, new Vector3f(5, 0, 5)));
        createController().executePossessedAttack();

        assertEquals(List.of(new Point(6, 5)), dugTiles);
        assertEquals(gameTime, entityData.getComponent(us, CreatureMeleeAttack.class).attactStartTime);
    }

    @Test
    void possessedWorkerCannotDigImpenetrableRock() {
        makeWorker();
        terrain.put(new Point(5, 6), ROCK);

        createController().executePossessedAttack();

        assertTrue(dugTiles.isEmpty());
        assertNull(entityData.getComponent(us, CreatureMeleeAttack.class).attactStartTime);
    }

    @Test
    void possessedWorkerFightsRatherThanDigs() {
        makeWorker();
        terrain.put(new Point(5, 6), DIRT);
        EntityId enemy = createCreature(HERO, new Vector3f(5, 0, 5.4f));

        createController().executePossessedAttack();

        assertEquals(DAMAGE, entityData.getComponent(enemy, Damage.class).damage);
        assertTrue(dugTiles.isEmpty());
    }

    private void makeWorker() {
        CreatureComponent worker = entityData.getComponent(us, CreatureComponent.class);
        worker.worker = true;
        entityData.setComponents(us, worker, new Gold(0, 100));
    }

    private EntityId createCreature(short ownerId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(0, position), new CreatureMeleeAttack(0, DAMAGE, RECHARGE, RANGE));
        return entity;
    }

    private EntityId createTrap(short ownerId, Vector3f position, boolean hidden) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new TrapViewState((short) 1, false, hidden), new Owner(ownerId, ownerId),
                new Health(2000, 2000), new Position(0, position));
        return entity;
    }

    private EntityId createDoor(short ownerId, Vector3f position) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new DoorComponent((short) 1, false, false), new Owner(ownerId, ownerId),
                new Health(2000, 2000), new Position(0, position));
        return entity;
    }

    private List<EntityId> getEntitiesInLocation(Point tile) {
        List<EntityId> entities = new ArrayList<>();
        for (Entity entity : entityData.getEntities(Position.class)) {
            if (WorldUtils.vectorToPoint(entity.get(Position.class).position).equals(tile)) {
                entities.add(entity.getId());
            }
        }
        return entities;
    }

    private IMapTileInformation getTile(int x, int y) {
        if (x < 0 || y < 0 || x > 19 || y > 19) {
            return null;
        }
        Terrain tileTerrain = terrain.getOrDefault(new Point(x, y), FLOOR);
        return stub(IMapTileInformation.class, (name, args) -> switch (name) {
            case "getTerrainId" -> tileTerrain.getTerrainId();
            case "getGold" -> 0;
            case "getLocation" -> new Point(x, y);
            default -> null;
        });
    }

    private Terrain getTerrain(IMapTileInformation tile) {
        for (Terrain candidate : List.of(DIRT, ROCK)) {
            if (candidate.getTerrainId() == tile.getTerrainId()) {
                return candidate;
            }
        }
        return FLOOR;
    }

    private CreatureController createController() {
        return createController(us);
    }

    private CreatureController createController(EntityId entity) {
        CreatureController controller = controllers.get(entity);
        if (controller == null) {
            controller = newController(entity);
            controllers.put(entity, controller);
        }
        return controller;
    }

    private CreatureController newController(EntityId entity) {
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getEntitiesInLocation".equals(name) ? getEntitiesInLocation((Point) args[0]) : null);
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> entityData.getComponent((EntityId) args[0], CreatureComponent.class) != null;
            case "createController" -> createController((EntityId) args[0]);
            default -> null;
        });
        IMapDataInformation<?> mapData = stub(IMapDataInformation.class, (name, args) -> {
            if (!"getTile".equals(name)) {
                return null;
            }
            return args.length == 1 ? getTile(((Point) args[0]).x, ((Point) args[0]).y) : getTile((Integer) args[0], (Integer) args[1]);
        });
        IMapController mapController = stub(IMapController.class, (name, args) -> switch (name) {
            case "getMapData" -> mapData;
            case "getTerrain" -> getTerrain((IMapTileInformation) args[0]);
            case "damageTile" -> {
                dugTiles.add((Point) args[0]);
                yield 0;
            }
            default -> null;
        });

        IKwdFile kwdFile = stub(IKwdFile.class, (kwdName, kwdArgs) -> switch (kwdName) {
            case "getCreatureFirstPersonStats" -> firstPersonStats;
            case "getCreature" -> (Short) kwdArgs[0] == HORNY
                    ? TrapFiringFixtures.creature(HORNY, Creature.CreatureFlag.IS_HORNY) : TrapFiringFixtures.creature((Short) kwdArgs[0]);
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> name.equals("getLevelData") ? kwdFile : null);
        return new CreatureController(entity, entityData, new Creature(), null, null, gameTimer, gameSettings, creaturesController,
                positionLookup, mapController, levelInfo, null, null, stub(GameEventService.class, (name, args) -> null));
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
