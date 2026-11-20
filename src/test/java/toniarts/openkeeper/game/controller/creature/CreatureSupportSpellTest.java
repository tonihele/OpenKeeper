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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Armour;
import toniarts.openkeeper.game.component.CastShot;
import toniarts.openkeeper.game.component.CombatPitFighter;
import toniarts.openkeeper.game.component.CreatureAi;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureFall;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Invisible;
import toniarts.openkeeper.game.component.Invulnerable;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.ShotFlight;
import toniarts.openkeeper.game.component.SpeedBlur;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.component.TaskComponent;
import toniarts.openkeeper.game.component.Teleport;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.controller.ShotsController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.task.ITaskManager;
import toniarts.openkeeper.game.task.Task;
import toniarts.openkeeper.game.task.TaskType;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.AlternativeShot;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature in a fight casting its spells for its own side, with the Heal
 * Creature of the Warlock in the original data, and an Imp speeding itself up
 * or teleporting on the way to a job
 */
class CreatureSupportSpellTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HEAL_SPELL_ID = 14;
    private static final short HEAL_SHOT_ID = 53;
    private static final short MODIFY_HEALTH_SHOT_ID = 4;
    private static final int HEAL_AMOUNT = 3000;
    private static final float RECHARGE = 2.5f;
    private static final float RANGE = 3f;
    private static final float SHOT_DELAY = 0.5f;

    private EntityData entityData;
    private List<EntityId> sensedEntities;
    private List<Object[]> shots;
    private List<Object[]> firedShots;
    private Shot shot;
    private List<Object[]> effects;
    private Task task;
    private boolean reachable;
    private double gameTime;
    private EntityId us;
    private EntityId spell;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        sensedEntities = new ArrayList<>();
        shots = new ArrayList<>();
        firedShots = new ArrayList<>();
        shot = ShotFixtures.healInstantShot(HEAL_SHOT_ID, MODIFY_HEALTH_SHOT_ID);
        effects = new ArrayList<>();
        task = stub(Task.class, (name, args) -> null);
        reachable = true;
        gameTime = 10;

        us = createFighter(KEEPER, new Vector3f(5, 0, 5), 1000);
        sensedEntities.remove(us);
        spell = entityData.createEntity();
        entityData.setComponent(spell, new CreatureSpell(HEAL_SPELL_ID, us, RECHARGE, RANGE));
        entityData.setComponent(us, new CreatureSpells(List.of(spell)));
    }

    @Test
    void healsABadlyHurtFriendInTheFight() {
        EntityId friend = createFighter(KEEPER, new Vector3f(5, 0, 7), 249);
        CreatureController controller = createController();

        assertTrue(controller.castSupportSpell());

        assertEquals(gameTime, entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
        assertEquals(CreatureState.CAST_SPELL, controller.getStateMachine().getCurrentState());
        assertEquals(friend, getCastTarget());
    }

    @Test
    void theShotLeavesAfterTheShotDelay() {
        EntityId friend = createFighter(KEEPER, new Vector3f(5, 0, 7), 249);
        CreatureController controller = createController();
        assertTrue(controller.castSupportSpell());

        gameTime += SHOT_DELAY / 2;
        controller.releaseCastShot();
        assertTrue(shots.isEmpty());

        gameTime += SHOT_DELAY / 2;
        controller.releaseCastShot();
        assertEquals(1, shots.size());
        assertArrayEquals(new Object[]{HEAL_SHOT_ID, HEAL_AMOUNT, 0, KEEPER, new Vector3f(5, 0, 5), friend, us}, shots.get(0));
        assertNull(entityData.getComponent(us, CastShot.class));

        // The heal is the shot's to do, the cast itself does no damage
        assertEquals(249, entityData.getComponent(friend, Health.class).health);
    }

    @Test
    void aShotThatFliesHitsItsOwnCasterAtOnce() {
        entityData.setComponent(us, new Health(100, 1000));
        shot = ShotFixtures.flyingSupportShot(HEAL_SHOT_ID, Shot.ProcessType.MODIFY_HEALTH);

        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.HEAL,
                createShotsController(shot));
        assertTrue(controller.castSupportSpell());

        gameTime += SHOT_DELAY;
        controller.releaseCastShot();

        // No shot is left to fly: a flying shot never hits the creature that shot it
        assertNull(entityData.findEntity(null, ShotFlight.class));
        assertEquals(-HEAL_AMOUNT, entityData.getComponent(us, Damage.class).damage);
    }

    @Test
    void anInstantShotOnItsOwnCasterIsFiredLikeAnyOther() {
        entityData.setComponent(us, new Health(100, 1000));
        CreatureController controller = createController();
        assertTrue(controller.castSupportSpell());

        gameTime += SHOT_DELAY;
        controller.releaseCastShot();

        assertEquals(1, firedShots.size());
        assertEquals(us, firedShots.get(0)[5]);
    }

    @Test
    void healsTheNearestOfThem() {
        createFighter(KEEPER, new Vector3f(5, 0, 7.5f), 100);
        EntityId near = createFighter(KEEPER, new Vector3f(6, 0, 5), 200);

        assertTrue(createController().castSupportSpell());

        assertEquals(near, getCastTarget());
    }

    @Test
    void healsItselfToo() {
        createFighter(KEEPER, new Vector3f(6, 0, 5), 200);
        entityData.setComponent(us, new Health(100, 1000));

        assertTrue(createController().castSupportSpell());

        assertEquals(us, getCastTarget());
    }

    @Test
    void leavesAFriendWithAQuarterOfItsHealthAlone() {
        createFighter(KEEPER, new Vector3f(5, 0, 7), 250);

        assertFalse(createController().castSupportSpell());

        assertNull(entityData.getComponent(us, CastShot.class));
        assertNull(entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
    }

    @Test
    void healsOnlyOwnFightersInRange() {
        createFighter(Player.GOOD_PLAYER_ID, new Vector3f(5, 0, 6), 100);
        createFighter(KEEPER, new Vector3f(5, 0, 8.5f), 100);
        EntityId idle = createFighter(KEEPER, new Vector3f(5, 0, 6), 100);
        entityData.setComponent(idle, new CreatureAi(0, CreatureState.IDLE, (short) 1));
        EntityId pitFighter = createFighter(KEEPER, new Vector3f(5, 0, 6), 100);
        entityData.setComponent(pitFighter, new CombatPitFighter());

        assertFalse(createController().castSupportSpell());
    }

    @Test
    void waitsForTheSpellToRecharge() {
        createFighter(KEEPER, new Vector3f(5, 0, 7), 100);
        CreatureController controller = createController();

        assertTrue(controller.castSupportSpell());
        gameTime += RECHARGE / 2;
        assertFalse(controller.castSupportSpell());
        gameTime += RECHARGE / 2;
        assertTrue(controller.castSupportSpell());

        assertNotNull(entityData.getComponent(us, CastShot.class));
    }

    @Test
    void attackSpellsAreNotCastOnFriends() {
        createFighter(KEEPER, new Vector3f(5, 0, 7), 100);

        assertFalse(createController(EnumSet.of(CreatureSpellFlag.IS_ATTACKING, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.NONE).castSupportSpell());
    }

    @Test
    void spellsWithoutAnEffectYetAreNotCast() {
        createFighter(KEEPER, new Vector3f(5, 0, 7), 100);

        assertFalse(createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.NONE).castSupportSpell());
    }

    @Test
    void castsArmourOnABadlyHurtFriendWithoutArmour() {
        EntityId friend = createFighter(KEEPER, new Vector3f(5, 0, 7), 100);
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.ARMOUR);

        assertTrue(controller.castSupportSpell());
        assertEquals(friend, getCastTarget());

        entityData.setComponent(friend, new Armour(30));
        gameTime += RECHARGE;
        assertFalse(controller.castSupportSpell());
    }

    @Test
    void armourWearsOff() {
        entityData.setComponents(us, new Armour(15), new CreatureFall());
        CreatureController controller = createController();

        controller.processTick(0.25f);
        assertEquals(15, entityData.getComponent(us, Armour.class).endTime);

        gameTime = 15;
        controller.processTick(0.25f);
        assertNull(entityData.getComponent(us, Armour.class));
    }

    @Test
    void castsInvulnerableOnABadlyHurtFriendWithoutIt() {
        EntityId friend = createFighter(KEEPER, new Vector3f(5, 0, 7), 100);
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.INVULNERABLE);

        assertTrue(controller.castSupportSpell());
        assertEquals(friend, getCastTarget());

        entityData.setComponent(friend, new Invulnerable(30));
        gameTime += RECHARGE;
        assertFalse(controller.castSupportSpell());
    }

    @Test
    void theInvisibleShotLandsOnItsCaster() {

        // The Thief's and the Rogue's Invisible, its shot flies
        shot = ShotFixtures.flyingSupportShot(HEAL_SHOT_ID, Shot.ProcessType.INVISIBLE);
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_SELF), AlternativeShot.INVISIBLE,
                createShotsController(shot));
        assertTrue(controller.castSupportSpell());

        gameTime += SHOT_DELAY;
        controller.releaseCastShot();

        assertEquals(gameTime + HEAL_AMOUNT, entityData.getComponent(us, Invisible.class).endTime);
    }

    @Test
    void turnsInvisibleInAFightUntilItWearsOff() {
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_SELF), AlternativeShot.INVISIBLE);

        assertTrue(controller.castSupportSpell());
        assertEquals(us, getCastTarget());

        entityData.setComponents(us, new Invisible(30), new CreatureFall(), new CreatureAi(0, CreatureState.FIGHT, (short) 1));
        gameTime += RECHARGE;
        assertFalse(controller.castSupportSpell());

        gameTime = 30;
        controller.processTick(0.25f);
        assertNull(entityData.getComponent(us, Invisible.class));
    }

    @Test
    void castsHasteOnABadlyHurtFriendWhoseSpeedIsNotChanged() {
        EntityId friend = createFighter(KEEPER, new Vector3f(5, 0, 7), 100);
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.HASTE);

        assertTrue(controller.castSupportSpell());
        assertEquals(friend, getCastTarget());

        entityData.setComponent(friend, new SpeedChange(30));
        gameTime += RECHARGE;
        assertFalse(controller.castSupportSpell());
    }

    @Test
    void theHasteShotLandsOnItsCaster() {
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.speed = 1f;
        entityData.setComponents(us, creatureComponent, new Mobile(false, false, false, 1f), new Health(100, 1000));

        // Haste Creature, its shot flies. We are the one of our fighters that needs it
        shot = ShotFixtures.flyingSupportShot(HEAL_SHOT_ID, Shot.ProcessType.MODIFY_SPEED);
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.HASTE,
                createShotsController(shot));
        assertTrue(controller.castSupportSpell());

        gameTime += SHOT_DELAY;
        controller.releaseCastShot();

        assertNotNull(entityData.getComponent(us, SpeedChange.class));
        assertEquals(4f, entityData.getComponent(us, Mobile.class).maxSpeed);
    }

    @Test
    void anImpSpeedsItselfUpWhenItSetsOffToAJob() {
        entityData.setComponent(us, new TaskComponent(1, null, null, TaskType.DIG_TILE, 0, false));
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_HASTE, CreatureSpellFlag.CAST_ON_OTHERS),
                AlternativeShot.NONE);

        controller.navigateToAssignedTask();

        // At once and without a cast
        assertEquals(1, shots.size());
        assertArrayEquals(new Object[]{HEAL_SHOT_ID, HEAL_AMOUNT, 0, KEEPER, new Vector3f(5, 0, 5), us, us}, shots.get(0));
        assertEquals(gameTime, entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
        assertNull(entityData.getComponent(us, CastShot.class));
    }

    @Test
    void anImpThatSpeedsItselfUpShowsTheSpeedBlurForThePercentageInGameTicks() {
        entityData.setComponent(us, new TaskComponent(1, null, null, TaskType.DIG_TILE, 0, false));
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_HASTE, CreatureSpellFlag.CAST_ON_OTHERS),
                AlternativeShot.NONE);

        controller.navigateToAssignedTask();

        // 0x4651A0 sets the blur timer to the spell's first value, 4 ticks a second here
        assertEquals(gameTime + HEAL_AMOUNT / 4.0, entityData.getComponent(us, SpeedBlur.class).endTime);
    }

    @Test
    void anImpSpeedsItselfUpAgainOnceTheSpellIsRecharged() {
        entityData.setComponent(us, new TaskComponent(1, null, null, TaskType.DIG_TILE, 0, false));
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_HASTE, CreatureSpellFlag.CAST_ON_OTHERS),
                AlternativeShot.NONE);

        controller.navigateToAssignedTask();
        gameTime += RECHARGE / 2;
        controller.navigateToAssignedTask();
        assertEquals(1, shots.size());

        gameTime += RECHARGE / 2;
        controller.navigateToAssignedTask();
        assertEquals(2, shots.size());
    }

    @Test
    void otherSpellsAreNotCastOnTheWayToAJob() {
        entityData.setComponent(us, new TaskComponent(1, null, null, TaskType.DIG_TILE, 0, false));

        createController().navigateToAssignedTask();

        assertTrue(shots.isEmpty());
        assertNull(entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
    }

    @Test
    void anImpTeleportsToAJobMoreThanTwoTilesAway() {
        assignJob(TaskType.DIG_TILE, new Vector2f(5, 8));
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_TELEPORT,
                CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.NONE);

        controller.navigateToAssignedTask();

        // It jumps out where it is instead of walking, and the spell recharges
        assertEquals(new Vector3f(5, 0, 8), entityData.getComponent(us, Teleport.class).destination);
        assertNull(entityData.getComponent(us, Navigation.class));
        assertEquals(gameTime, entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
        assertArrayEquals(new Object[]{321, new Vector3f(5, 0, 5), 0f}, effects.get(0));
    }

    @Test
    void theImpLandsOnTheJobAfterFourTicksAndCarriesOnTwoTicksLater() {

        // Gold for the Treasury is no reason to walk
        assignJob(TaskType.CARRY_GOLD_TO_TREASURY, new Vector2f(5, 8));
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_TELEPORT,
                CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.NONE);
        controller.navigateToAssignedTask();

        gameTime += 0.75;
        controller.processTick(0.25f);
        assertEquals(new Vector3f(5, 0, 5), entityData.getComponent(us, Position.class).position);

        gameTime += 0.25;
        controller.processTick(0.25f);
        assertEquals(new Vector3f(5, 0, 8), entityData.getComponent(us, Position.class).position);
        assertArrayEquals(new Object[]{322, new Vector3f(5, 0, 8), 0f}, effects.get(1));

        gameTime += 0.25;
        controller.processTick(0.25f);
        assertNotNull(entityData.getComponent(us, Teleport.class));

        gameTime += 0.25;
        controller.processTick(0.25f);
        assertNull(entityData.getComponent(us, Teleport.class));
    }

    @Test
    void anImpWalksToAJobTwoTilesAway() {
        assignJob(TaskType.DIG_TILE, new Vector2f(7, 7));

        createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_TELEPORT, CreatureSpellFlag.CAST_ON_OTHERS),
                AlternativeShot.NONE).navigateToAssignedTask();

        assertNull(entityData.getComponent(us, Teleport.class));
        assertNotNull(entityData.getComponent(us, Navigation.class));
        assertNull(entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
    }

    @Test
    void anImpWalksWhenItTakesSomethingSomewhere() {
        assignJob(TaskType.CARRY_CORPSE_TO_GRAVEYARD, new Vector2f(5, 8));

        createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_TELEPORT, CreatureSpellFlag.CAST_ON_OTHERS),
                AlternativeShot.NONE).navigateToAssignedTask();

        assertNull(entityData.getComponent(us, Teleport.class));
        assertNull(entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
    }

    @Test
    void anImpDoesNotTeleportWhereItCannotWalk() {
        assignJob(TaskType.DIG_TILE, new Vector2f(5, 8));
        reachable = false;

        createController(EnumSet.of(CreatureSpellFlag.IS_DEFENSIVE, CreatureSpellFlag.IMP_TELEPORT, CreatureSpellFlag.CAST_ON_OTHERS),
                AlternativeShot.NONE).navigateToAssignedTask();

        assertNull(entityData.getComponent(us, Teleport.class));
        assertNull(entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
    }

    @Test
    void hitsDoAnInvulnerableCreatureNoHarmUntilItWearsOff() {
        EntityId enemy = createFighter(Player.KEEPER2_ID, new Vector3f(5, 0, 6), 1000);
        entityData.setComponents(enemy, new Invulnerable(15), new CreatureFall());
        CreatureController controller = createController();

        controller.setDamage(enemy, 100);
        assertNull(entityData.getComponent(enemy, Damage.class));

        gameTime = 15;
        new CreatureController(enemy, entityData, new Creature(), null, null, stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null),
                new HashMap<>(), null, null, null, null, null, null, null).processTick(0.25f);
        controller.setDamage(enemy, 100);
        assertEquals(100, entityData.getComponent(enemy, Damage.class).damage);
    }

    private void assignJob(TaskType taskType, Vector2f target) {
        entityData.setComponent(us, new TaskComponent(1, null, null, taskType, 0, false));
        task = stub(Task.class, (name, args) -> switch (name) {
            case "getTarget" ->
                target;
            case "getTaskType" ->
                taskType;
            default ->
                null;
        });
    }

    private EntityId getCastTarget() {
        return entityData.getComponent(us, CastShot.class).target;
    }

    private EntityId createFighter(short ownerId, Vector3f position, int health) {
        EntityId entity = entityData.createEntity();
        entityData.setComponents(entity, new CreatureComponent(), new Owner(ownerId, ownerId), new Health(health, 1000),
                new Position(0, position), new CreatureAi(0, CreatureState.FIGHT, (short) 1));
        sensedEntities.add(entity);
        return entity;
    }

    private CreatureController createController() {
        return createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_OTHERS), AlternativeShot.HEAL);
    }

    private CreatureController createController(EnumSet<CreatureSpellFlag> flags, AlternativeShot alternativeShot) {
        return createController(flags, alternativeShot, stub(IShotsController.class, (name, args) -> {
            if (name.equals("createShot")) {
                shots.add(args);
            } else if (name.equals("fireShot")) {
                firedShots.add(args);
            }
            return null;
        }));
    }

    /**
     * The real shots, with the one shot there is
     */
    private IShotsController createShotsController(Shot shot) {
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getShotById" ->
                shot;
            case "getGameLevel" ->
                ShotFixtures.gameLevel(4);
            default ->
                null;
        });
        return new ShotsController(kwdFile, entityData, null, () -> gameTime, null, null, null, null, null, stub(GameEventService.class, (name, args) -> null));
    }

    private CreatureController createController(EnumSet<CreatureSpellFlag> flags, AlternativeShot alternativeShot, IShotsController shotsController) {
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = ShotFixtures.creatureSpell(HEAL_SPELL_ID, flags, alternativeShot, HEAL_SHOT_ID, HEAL_AMOUNT);
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getSensedEntities".equals(name) ? new HashSet<>(sensedEntities) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> name.equals("getLevelData")
                ? stub(IKwdFile.class, (kwdName, kwdArgs) -> switch (kwdName) {
                    case "getCreatureSpellById" ->
                        creatureSpell;
                    case "getShotById" ->
                        shot;
                    case "getGameLevel" ->
                        ShotFixtures.gameLevel(4);
                    default ->
                        null;
                }) : null);
        ITaskManager taskManager = stub(ITaskManager.class, (name, args) -> "getTaskById".equals(name) ? task : null);
        INavigationService navigationService = stub(INavigationService.class,
                (name, args) -> "findPath".equals(name) && reachable ? new DefaultGraphPath<>() : null);
        GameEventService gameEventService = stub(GameEventService.class, (name, args) -> {
            if (name.equals("emitEffect")) {
                effects.add(args);
            }
            return null;
        });
        return new CreatureController(us, entityData, ShotFixtures.caster(HEAL_SPELL_ID, SHOT_DELAY), navigationService, taskManager, gameTimer,
                new HashMap<>(), null, positionLookup, null, levelInfo, null, shotsController, gameEventService);
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
