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

import com.badlogic.gdx.ai.pfa.GraphPath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Navigation;
import toniarts.openkeeper.game.component.Peaceful;
import toniarts.openkeeper.game.component.TrapViewState;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.tools.convert.map.CombatCreatureFixtures;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureCombatTest {

    private static final short FIGHTER = 1;
    private static final short HORNY = 2;
    private static final short GOOD = 3;
    private static final short STYLED = 4;
    private static final short SUPPORT = 5;
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;

    private EntityData entityData;
    private Map<Short, Creature> creatures;
    private Map<EntityId, Set<EntityId>> sensed;
    private EntityId us;
    private EntityId blockingDoor;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        creatures = new HashMap<>();
        creatures.put(FIGHTER, CombatCreatureFixtures.withSightDistance(
                CombatCreatureFixtures.fighter(FIGHTER, Creature.FightStyle.BLOCKER), 10));
        creatures.put(HORNY, CombatCreatureFixtures.fighter(HORNY, Creature.FightStyle.BLITZER, Creature.CreatureFlag.IS_HORNY));
        sensed = new HashMap<>();
        us = createCreature(KEEPER, FIGHTER, new Vector3f(5, 0, 4), 0);
    }

    @Test
    void enemyAttackingUsIsPreferredOverANearerOne() {
        EntityId nearer = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        EntityId attacker = createCreature(HERO, FIGHTER, new Vector3f(8, 0, 4), 0);
        entityData.setComponent(attacker, new AttackTarget(us));
        sensed.put(us, new LinkedHashSet<>(List.of(nearer, attacker)));

        assertEquals(attacker, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void enemyWithAFullSetOfAttackersIsNotChosen() {
        EntityId crowded = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        EntityId free = createCreature(HERO, FIGHTER, new Vector3f(8, 0, 4), 0);
        for (int i = 0; i < CombatRules.MAX_MELEE_ATTACKERS; i++) {
            entityData.setComponent(entityData.createEntity(), new AttackTarget(crowded));
        }
        sensed.put(us, new LinkedHashSet<>(List.of(crowded, free)));

        assertEquals(free, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void naturalEnemyIsPreferredOverASlightlyNearerOne() {
        creatures.put(GOOD, CombatCreatureFixtures.fighter(GOOD, Creature.FightStyle.BLOCKER, Creature.CreatureFlag.IS_GOOD));
        EntityId nearer = createCreature(HERO, FIGHTER, new Vector3f(7, 0, 4), 0);
        EntityId good = createCreature(HERO, GOOD, new Vector3f(7.1f, 0, 4), 0);
        sensed.put(us, new LinkedHashSet<>(List.of(nearer, good)));

        assertEquals(good, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void flankersAndSupportGoForTheEnemyABlockerHolds() {
        for (Creature.FightStyle fightStyle : List.of(Creature.FightStyle.FLANKER, Creature.FightStyle.SUPPORT)) {
            creatures.put(STYLED, CombatCreatureFixtures.fighter(STYLED, fightStyle));
            EntityId styled = createCreature(KEEPER, STYLED, new Vector3f(5, 0, 4), 0);
            EntityId free = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
            EntityId held = createCreature(HERO, FIGHTER, new Vector3f(8, 0, 4), 0);
            entityData.setComponent(createCreature(KEEPER, FIGHTER, new Vector3f(9, 0, 4), 0), new AttackTarget(held));
            sensed.put(styled, new LinkedHashSet<>(List.of(free, held)));

            assertEquals(held, createController(styled).getAttackTarget().getEntityId(), fightStyle.name());
        }
    }

    @Test
    void blitzerGoesPastAFighterForASupportCreature() {
        creatures.put(STYLED, CombatCreatureFixtures.fighter(STYLED, Creature.FightStyle.BLITZER));
        creatures.put(SUPPORT, CombatCreatureFixtures.fighter(SUPPORT, Creature.FightStyle.SUPPORT));
        EntityId blitzer = createCreature(KEEPER, STYLED, new Vector3f(5, 0, 4), 0);
        EntityId fighter = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        EntityId support = createCreature(HERO, SUPPORT, new Vector3f(8, 0, 4), 0);
        sensed.put(blitzer, new LinkedHashSet<>(List.of(fighter, support)));

        assertEquals(support, createController(blitzer).getAttackTarget().getEntityId());
    }

    @Test
    void creatureThatWontFightPicksNoEnemyUntilItMayFightAgain() {
        EntityId enemy = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        sensed.put(us, Set.of(enemy));
        CreatureController controller = createController(us);

        controller.setWillFight(false);
        assertNull(controller.getAttackTarget());

        controller.setWillFight(true);
        assertEquals(enemy, controller.getAttackTarget().getEntityId());
    }

    @Test
    void creatureThatStopsFightingDropsItsTarget() {
        EntityId enemy = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        entityData.setComponent(us, new AttackTarget(enemy));
        CreatureController controller = createController(us);

        controller.setWillFight(false);

        assertNull(controller.getAttackTarget());
        assertNull(entityData.getComponent(us, AttackTarget.class));
    }

    @Test
    void peacefulCreatureLeavesAnEnemyTrapAlone() {
        EntityId trap = createTrap(new Vector3f(5.5f, 0, 4));
        entityData.setComponent(us, new Peaceful());

        assertFalse(createController(us).attackVisibleTrap());
        assertNull(entityData.getComponent(trap, Damage.class));
        assertNull(entityData.getComponent(us, Navigation.class));
        assertNull(entityData.getComponent(us, AttackTarget.class));
    }

    @Test
    void peacefulCreatureDoesNotNavigateTowardsADistantTrap() {
        EntityId trap = createTrap(new Vector3f(8, 0, 4));
        entityData.setComponent(us, new Peaceful());

        assertFalse(createController(us).attackVisibleTrap());
        assertNull(entityData.getComponent(us, Navigation.class));
        assertNull(entityData.getComponent(trap, Damage.class));
    }

    @Test
    void peacefulWorkerLeavesATrapWithinWorkerReachAlone() {
        EntityId trap = createTrap(new Vector3f(5.5f, 0, 4));
        entityData.getComponent(us, CreatureComponent.class).worker = true;
        CreatureController controller = createController(us);
        controller.setWillFight(false);

        assertFalse(controller.attackVisibleTrap());
        assertNull(entityData.getComponent(trap, Damage.class));

        controller.setWillFight(true);
        assertTrue(controller.attackVisibleTrap());
        assertEquals(100, entityData.getComponent(trap, Damage.class).damage);
    }

    @Test
    void fighterAttacksATrapUntilItsFightFlagIsCleared() {
        EntityId trap = createTrap(new Vector3f(5.5f, 0, 4));
        CreatureController controller = createController(us);
        assertTrue(controller.attackVisibleTrap());
        assertEquals(100, entityData.getComponent(trap, Damage.class).damage);
        entityData.removeComponent(trap, Damage.class);

        controller.setWillFight(false);
        assertFalse(controller.attackVisibleTrap());
        assertNull(entityData.getComponent(trap, Damage.class));

        // Changing willingness must not leave a retained combat target behind either.
        entityData.setComponent(us, new AttackTarget(trap));
        assertNull(controller.getAttackTarget());
        assertNull(entityData.getComponent(us, AttackTarget.class));
    }

    @Test
    void peacefulCreatureDoesNotStartAnIdleDoorAttack() {
        EntityId door = entityData.createEntity();
        entityData.setComponents(door, new DoorComponent((short) 1, false, false),
                new Owner(HERO, HERO), new Health(1000, 1000),
                new Position(0, new Vector3f(6, 0, 4)));
        blockingDoor = door;
        CreatureController controller = createController(us);
        controller.setWillFight(false);

        assertFalse(controller.attackAdjacentDoor());
        assertNull(entityData.getComponent(door, Damage.class));

        controller.setWillFight(true);
        assertTrue(controller.attackAdjacentDoor());
        assertEquals(100, entityData.getComponent(door, Damage.class).damage);
    }

    private EntityId createTrap(Vector3f position) {
        EntityId trap = entityData.createEntity();
        entityData.setComponents(trap, new TrapViewState((short) 8, false), new Owner(HERO, HERO),
                new Health(1000, 1000), new Position(0, position));
        sensed.put(us, Set.of(trap));
        return trap;
    }

    @Test
    void hitInTheBackGetsTheRearAttackBonus() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 5), 0);

        createController(us).executeAttack(target);

        assertEquals(151, entityData.getComponent(target, Damage.class).damage);
    }

    @Test
    void onlyCreaturesGetTheRearAttackBonus() {
        EntityId thing = entityData.createEntity();
        entityData.setComponents(thing, new Owner(HERO, HERO), new Health(1000, 1000),
                new Position(0, new Vector3f(5, 0, 5)));

        createController(us).executeAttack(thing);

        assertEquals(100, entityData.getComponent(thing, Damage.class).damage);
    }

    @Test
    void hornedReaperTakesNoDamage() {
        EntityId target = createCreature(HERO, HORNY, new Vector3f(5, 0, 5), 0);

        createController(us).executeAttack(target);

        assertNull(entityData.getComponent(target, Damage.class));
    }

    @Test
    void attackersWithSpellsLeaveRoomForMeleeAttackers() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        EntityId other = createCreature(HERO, FIGHTER, new Vector3f(8, 0, 4), 0);
        for (int i = 0; i < CombatRules.MAX_RANGED_ATTACKERS; i++) {
            EntityId caster = entityData.createEntity();
            giveAttackSpell(caster);
            entityData.setComponent(caster, new AttackTarget(target));
        }
        sensed.put(us, new LinkedHashSet<>(List.of(target, other)));

        assertEquals(target, createController(us).getAttackTarget().getEntityId());
    }

    @Test
    void attackerWithSpellsJoinsAFullSetOfMeleeAttackers() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(6, 0, 4), 0);
        EntityId other = createCreature(HERO, FIGHTER, new Vector3f(8, 0, 4), 0);
        for (int i = 0; i < CombatRules.MAX_MELEE_ATTACKERS; i++) {
            entityData.setComponent(entityData.createEntity(), new AttackTarget(target));
        }
        giveAttackSpell(us);
        sensed.put(us, new LinkedHashSet<>(List.of(target, other)));

        assertEquals(target, createController(us).getAttackTarget().getEntityId());
    }

    private EntityId createCreature(short ownerId, short creatureId, Vector3f position, float rotation) {
        EntityId entity = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = creatureId;
        entityData.setComponents(entity, creatureComponent, new Owner(ownerId, ownerId), new Health(1000, 1000),
                new Position(rotation, position), new CreatureMeleeAttack(0, 100, 1, 1.5f));
        return entity;
    }

    private void giveAttackSpell(EntityId creature) {
        EntityId spell = entityData.createEntity();
        entityData.setComponent(spell, new CreatureSpell((short) 1, creature, 1, 5));
        entityData.setComponent(creature, new CreatureSpells(List.of(spell)));
    }

    private CreatureController createController(EntityId entity) {
        Map<Short, Keeper> keepers = Map.of(KEEPER, new Keeper(false, KEEPER), HERO, new Keeper(false, HERO));
        IKwdFile kwdFile = stub(IKwdFile.class, (name, args) -> switch (name) {
            case "getCreature" -> creatures.get((Short) args[0]);
            case "getCreatureSpellById" -> CombatCreatureFixtures.attackSpell();
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwdFile;
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> null;
        });
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? 10.0 : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> switch (name) {
                    case "getSensedEntities" -> sensed.getOrDefault((EntityId) args[0], Set.of());
                    case "getBlockingDoor" -> blockingDoor;
                    default -> null;
                });
        ICreaturesController creaturesController = stub(ICreaturesController.class, (name, args) -> switch (name) {
            case "isValidEntity" -> true;
            case "createController" -> createController((EntityId) args[0]);
            default -> null;
        });
        GraphPath<?> path = stub(GraphPath.class, (name, args) -> null);
        INavigationService navigationService = stub(INavigationService.class, (name, args) -> "findPath".equals(name) ? path : null);
        CreatureComponent creatureComponent = entityData.getComponent(entity, CreatureComponent.class);

        return new CreatureController(entity, entityData, creatures.get(creatureComponent.creatureId), navigationService,
                null, gameTimer, Map.of(), creaturesController, positionLookup, null, levelInfo, null, null, stub(toniarts.openkeeper.game.event.GameEventService.class, (name, args) -> null));
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
