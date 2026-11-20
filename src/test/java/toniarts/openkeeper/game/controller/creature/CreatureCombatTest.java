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
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.awt.Point;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.AttackTarget;
import toniarts.openkeeper.game.component.CastShot;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMeleeAttack;
import toniarts.openkeeper.game.component.CreatureSpell;
import toniarts.openkeeper.game.component.CreatureSpells;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.DoorComponent;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Navigation;
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
import toniarts.openkeeper.tools.convert.map.DoorFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreatureCombatTest {

    private static final short FIGHTER = 1;
    private static final short HORNY = 2;
    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HERO = Player.GOOD_PLAYER_ID;
    private static final short WOODEN_DOOR = 1;

    private EntityData entityData;
    private Map<Short, Creature> creatures;
    private Map<EntityId, Set<EntityId>> sensed;
    private Map<Point, EntityId> doors;
    private EntityId us;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        creatures = new HashMap<>();
        creatures.put(FIGHTER, CombatCreatureFixtures.fighter(FIGHTER, Creature.FightStyle.BLOCKER));
        creatures.put(HORNY, CombatCreatureFixtures.fighter(HORNY, Creature.FightStyle.BLITZER, Creature.CreatureFlag.IS_HORNY));
        sensed = new HashMap<>();
        doors = new HashMap<>();
        us =createCreature(KEEPER, FIGHTER, new Vector3f(5, 0, 4), 0);
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
    void hitInTheBackGetsTheRearAttackBonus() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 5), 0);

        createController(us).executeAttack(target);

        assertEquals(151, entityData.getComponent(target, Damage.class).damage);
    }

    @Test
    void creatureTurnsToItsTargetBeforeItSwings() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 5), 0);

        // In reach but facing away. DK II's fight state turns the creature first and only swings once no turn is
        // left (0x45F030, 0x4B2AE0)
        entityData.setComponent(us, new Position(FastMath.PI, new Vector3f(5, 0, 4)));
        createController(us).executeAttack(target);

        assertNull(entityData.getComponent(target, Damage.class));
        assertTurnsTo(entityData.getComponent(us, Navigation.class), 0);

        // Turned, it swings
        entityData.removeComponent(us, Navigation.class);
        entityData.setComponent(us, new Position(0, new Vector3f(5, 0, 4)));
        createController(us).executeAttack(target);

        assertEquals(151, entityData.getComponent(target, Damage.class).damage);
        assertNull(entityData.getComponent(us, Navigation.class));
    }

    @Test
    void creatureTurnsToItsTargetBeforeItCasts() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 7), 0);
        giveAttackSpells(us, castAt((short) 1, 5, null));

        // The turn comes before the attack in DK II's fight state, a spell's too (0x45F030)
        entityData.setComponent(us, new Position(FastMath.HALF_PI, new Vector3f(5, 0, 4)));
        createController(us).executeAttack(target);

        assertNull(entityData.getComponent(us, CastShot.class));
        assertTurnsTo(entityData.getComponent(us, Navigation.class), 0);

        entityData.removeComponent(us, Navigation.class);
        entityData.setComponent(us, new Position(0, new Vector3f(5, 0, 4)));
        createController(us).executeAttack(target);

        assertEquals(1, entityData.getComponent(us, CastShot.class).creatureSpellId);
    }

    @Test
    void creatureTurnsToADoorBeforeItHitsIt() {
        EntityId door = entityData.createEntity();
        entityData.setComponents(door, new DoorComponent(WOODEN_DOOR, false, false), new Owner(HERO, HERO),
                new Health(1000, 1000), new Position(0, new Vector3f(5, 0, 5)));
        doors.put(new Point(5, 5), door);

        // A door is a target of the fight state like any other, so the creature turns to it before every hit
        // too (0x481400 makes the door the target, 0x45F030 swings once 0x4B2AE0 leaves no turn)
        entityData.setComponent(us, new Position(FastMath.PI, new Vector3f(5, 0, 4)));
        createController(us).attackAdjacentDoor();

        assertNull(entityData.getComponent(door, Damage.class));
        assertTurnsTo(entityData.getComponent(us, Navigation.class), 0);

        entityData.removeComponent(us, Navigation.class);
        entityData.setComponent(us, new Position(0, new Vector3f(5, 0, 4)));
        createController(us).attackAdjacentDoor();

        assertEquals(100, entityData.getComponent(door, Damage.class).damage);
        assertNull(entityData.getComponent(us, Navigation.class));
    }

    /**
     * The steering turns the creature from the centre of the navigation's tile
     * to the centre of its face target tile
     */
    private static void assertTurnsTo(Navigation navigation, double rotation) {
        assertNotNull(navigation, "no turn");
        double steered = Math.atan2(navigation.faceTarget.x - navigation.target.x, navigation.faceTarget.y - navigation.target.y);
        assertTrue(Math.abs(Math.IEEEremainder(steered - rotation, 2 * Math.PI)) < 0.05,
                "turns to " + Math.toDegrees(steered) + " degrees");
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

    @Test
    void castsTheAttackSpellThatWasReadyFirst() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 7), 0);

        // At 10 seconds all three are ready: since 7, since 9 and since 8
        giveAttackSpells(us, castAt((short) 1, 5, 2.0), castAt((short) 2, 1, 8.0), castAt((short) 3, 3, 5.0));

        createController(us).executeAttack(target);

        assertEquals(1, entityData.getComponent(us, CastShot.class).creatureSpellId);
    }

    @Test
    void spellNeverCastComesBeforeTheOnesThatWere() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 7), 0);
        giveAttackSpells(us, castAt((short) 1, 5, 2.0), castAt((short) 2, 6, null), castAt((short) 3, 6, null));

        createController(us).executeAttack(target);

        // The earlier slot of the two that tie
        assertEquals(2, entityData.getComponent(us, CastShot.class).creatureSpellId);
    }

    @Test
    void spellStillRechargingOrOutOfRangeIsNotCast() {
        EntityId target = createCreature(HERO, FIGHTER, new Vector3f(5, 0, 7), 0);
        CreatureSpell shortRanged = new CreatureSpell(new CreatureSpell((short) 2, us, 1, 2), 1.0);
        giveAttackSpells(us, castAt((short) 1, 5, 6.0), shortRanged, castAt((short) 3, 3, 6.5));

        createController(us).executeAttack(target);

        assertEquals(3, entityData.getComponent(us, CastShot.class).creatureSpellId);
    }

    private CreatureSpell castAt(short creatureSpellId, float rechargeTime, Double lastCast) {
        CreatureSpell spell = new CreatureSpell(creatureSpellId, us, rechargeTime, 5);
        return lastCast != null ? new CreatureSpell(spell, lastCast) : spell;
    }

    private void giveAttackSpells(EntityId creature, CreatureSpell... creatureSpells) {
        List<EntityId> spells = new ArrayList<>();
        for (CreatureSpell creatureSpell : creatureSpells) {
            EntityId spell = entityData.createEntity();
            entityData.setComponent(spell, creatureSpell);
            spells.add(spell);
        }
        entityData.setComponent(creature, new CreatureSpells(spells));
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
            case "getDoorById" -> DoorFixtures.door(WOODEN_DOOR, 0);
            default -> null;
        });
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> switch (name) {
            case "getLevelData" -> kwdFile;
            case "getPlayer" -> keepers.get((Short) args[0]);
            default -> null;
        });
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? 10.0 : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class, (name, args) -> switch (name) {
            case "getSensedEntities" -> sensed.getOrDefault((EntityId) args[0], Set.of());
            case "getBlockingDoor" -> doors.get(new Point((Integer) args[0], (Integer) args[1]));
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
