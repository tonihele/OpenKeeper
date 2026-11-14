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
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.component.SpeedChange;
import toniarts.openkeeper.game.controller.IGameTimer;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IShotsController;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.logic.IEntityPositionLookup;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.AlternativeShot;
import toniarts.openkeeper.tools.convert.map.CreatureSpell.CreatureSpellFlag;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.ShotFixtures;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A creature in a fight casting its spells for its own side, with the Heal
 * Creature of the Warlock in the original data
 */
class CreatureSupportSpellTest {

    private static final short KEEPER = Player.KEEPER1_ID;
    private static final short HEAL_SPELL_ID = 14;
    private static final short HEAL_SHOT_ID = 53;
    private static final int HEAL_AMOUNT = 3000;
    private static final float RECHARGE = 2.5f;
    private static final float RANGE = 3f;

    private EntityData entityData;
    private List<EntityId> sensedEntities;
    private List<Object[]> shots;
    private double gameTime;
    private EntityId us;
    private EntityId spell;

    @BeforeEach
    void setUp() {
        entityData = new DefaultEntityData();
        sensedEntities = new ArrayList<>();
        shots = new ArrayList<>();
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

        assertEquals(1, shots.size());
        assertArrayEquals(new Object[]{HEAL_SHOT_ID, HEAL_AMOUNT, 0, KEEPER, new Vector3f(5, 0, 5), friend, us}, shots.get(0));
        assertEquals(gameTime, entityData.getComponent(spell, CreatureSpell.class).attactStartTime);
        assertEquals(CreatureState.CAST_SPELL, controller.getStateMachine().getCurrentState());
    }

    @Test
    void healsTheNearestOfThem() {
        createFighter(KEEPER, new Vector3f(5, 0, 7.5f), 100);
        EntityId near = createFighter(KEEPER, new Vector3f(6, 0, 5), 200);

        assertTrue(createController().castSupportSpell());

        assertEquals(near, shots.get(0)[5]);
    }

    @Test
    void healsItselfToo() {
        createFighter(KEEPER, new Vector3f(6, 0, 5), 200);
        entityData.setComponent(us, new Health(100, 1000));

        assertTrue(createController().castSupportSpell());

        assertEquals(us, shots.get(0)[5]);
    }

    @Test
    void leavesAFriendWithAQuarterOfItsHealthAlone() {
        createFighter(KEEPER, new Vector3f(5, 0, 7), 250);

        assertFalse(createController().castSupportSpell());

        assertTrue(shots.isEmpty());
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

        assertEquals(2, shots.size());
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
        assertEquals(friend, shots.get(0)[5]);

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
        assertEquals(friend, shots.get(0)[5]);

        entityData.setComponent(friend, new Invulnerable(30));
        gameTime += RECHARGE;
        assertFalse(controller.castSupportSpell());
    }

    @Test
    void turnsInvisibleInAFightUntilItWearsOff() {
        CreatureController controller = createController(EnumSet.of(CreatureSpellFlag.CREATURE, CreatureSpellFlag.CAST_ON_SELF), AlternativeShot.INVISIBLE);

        assertTrue(controller.castSupportSpell());
        assertEquals(us, shots.get(0)[5]);

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
        assertEquals(friend, shots.get(0)[5]);

        entityData.setComponent(friend, new SpeedChange(30));
        gameTime += RECHARGE;
        assertFalse(controller.castSupportSpell());
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
        toniarts.openkeeper.tools.convert.map.CreatureSpell creatureSpell = ShotFixtures.creatureSpell(HEAL_SPELL_ID, flags, alternativeShot, HEAL_SHOT_ID, HEAL_AMOUNT);
        IGameTimer gameTimer = stub(IGameTimer.class, (name, args) -> "getGameTime".equals(name) ? gameTime : null);
        IEntityPositionLookup positionLookup = stub(IEntityPositionLookup.class,
                (name, args) -> "getSensedEntities".equals(name) ? new HashSet<>(sensedEntities) : null);
        ILevelInfo levelInfo = stub(ILevelInfo.class, (name, args) -> name.equals("getLevelData")
                ? stub(IKwdFile.class, (kwdName, kwdArgs) -> kwdName.equals("getCreatureSpellById") ? creatureSpell : null) : null);
        IShotsController shotsController = stub(IShotsController.class, (name, args) -> {
            shots.add(args);
            return null;
        });
        return new CreatureController(us, entityData, new Creature(), null, null, gameTimer, new HashMap<>(), null,
                positionLookup, null, levelInfo, null, shotsController, stub(GameEventService.class, (name, args) -> null));
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
