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
package toniarts.openkeeper.game.controller.trap;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.EntitySet;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Alarm;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.Damage;
import toniarts.openkeeper.game.component.Decay;
import toniarts.openkeeper.game.component.Frozen;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Shot;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.tools.convert.map.TrapFiringFixtures;
import toniarts.openkeeper.utils.WorldUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrapEffectsTest {

    private static final short KEEPER = 3;
    private static final short ENEMY = 4;
    private static final short IMP = 1;
    private static final short REAPER = 2;
    private static final Vector3f TRAP_POSITION = new Vector3f(10, WorldUtils.FLOOR_HEIGHT, 10);

    private final EntityData entityData = new DefaultEntityData();
    private final Map<Short, Shot> shots = new HashMap<>();
    private final Map<Short, Creature> creatures = new HashMap<>();
    private final TrapEffects trapEffects;
    private double gameTime = 5;

    TrapEffectsTest() {
        creatures.put(IMP, TrapFiringFixtures.creature(IMP, Creature.CreatureFlag.IS_WORKER));
        creatures.put(REAPER, TrapFiringFixtures.creature(REAPER, Creature.CreatureFlag.IS_HORNY));
        trapEffects = new TrapEffects(entityData, kwdFile(), () -> gameTime);
    }

    @Test
    void cannonBallHitsTheTarget() {
        Trap sentry = trap((short) 3, Shot.ProcessType.NONE, Shot.DamageType.HIT_THING, 250, 0);
        EntityId target = addCreature(ENEMY, 13, 10);
        EntityId bystander = addCreature(ENEMY, 13, 10);

        trapEffects.fire(sentry, KEEPER, TRAP_POSITION, target);

        assertEquals(250, damage(target));
        assertEquals(0, damage(bystander));
    }

    @Test
    void cannonBallWithoutTargetHitsNobody() {
        Trap sentry = trap((short) 3, Shot.ProcessType.NONE, Shot.DamageType.HIT_THING, 250, 0);
        EntityId bystander = addCreature(ENEMY, 10, 10);

        trapEffects.fire(sentry, KEEPER, TRAP_POSITION, null);

        assertEquals(0, damage(bystander));
    }

    @Test
    void spikesHitEveryoneOnTheTileButTheOwnersCreatures() {
        Trap spike = trap((short) 10, Shot.ProcessType.NONE, Shot.DamageType.CONSTANT, 750, 1);
        EntityId target = addCreature(ENEMY, 10, 10);
        EntityId other = addCreature(ENEMY, 10.3f, 9.8f);
        EntityId own = addCreature(KEEPER, 10, 10);
        EntityId nextTile = addCreature(ENEMY, 11, 10);

        trapEffects.fire(spike, KEEPER, TRAP_POSITION, target);

        assertEquals(750, damage(target));
        assertEquals(750, damage(other));
        assertEquals(0, damage(own));
        assertEquals(0, damage(nextTile));
    }

    @Test
    void fireburstHitsEveryoneWithinItsRadiusButTheOwnersCreatures() {
        Trap fireburst = trap((short) 5, Shot.ProcessType.NONE, Shot.DamageType.RANGE_FROM_ORIGIN, 2000, 1);
        EntityId target = addCreature(ENEMY, 11, 10);
        EntityId neutral = addCreature((short) 2, 10, 9);
        EntityId own = addCreature(KEEPER, 10, 11);
        EntityId farAway = addCreature(ENEMY, 12, 10);

        trapEffects.fire(fireburst, KEEPER, TRAP_POSITION, target);

        assertEquals(2000, damage(target));
        assertEquals(2000, damage(neutral));
        assertEquals(0, damage(own));
        assertEquals(0, damage(farAway));
    }

    @Test
    void gasHurtsAroundTheTrap() {
        Trap gas = trap((short) 1, Shot.ProcessType.GAS, Shot.DamageType.RANGE_FROM_ORIGIN, 750, 1);
        EntityId target = addCreature(ENEMY, 10, 10);
        EntityId nextTile = addCreature(ENEMY, 10, 11);

        trapEffects.fire(gas, KEEPER, TRAP_POSITION, target);

        assertEquals(750, damage(target));
        assertEquals(750, damage(nextTile));
    }

    @Test
    void damageAddsUp() {
        Trap sentry = trap((short) 3, Shot.ProcessType.NONE, Shot.DamageType.HIT_THING, 250, 0);
        EntityId target = addCreature(ENEMY, 13, 10);

        trapEffects.fire(sentry, KEEPER, TRAP_POSITION, target);
        trapEffects.fire(sentry, KEEPER, TRAP_POSITION, target);

        assertEquals(500, damage(target));
    }

    @Test
    void triggerTrapShotDoesNothing() {
        Trap trigger = trap((short) 9, Shot.ProcessType.NONE, Shot.DamageType.NONE, 0, 1);
        EntityId target = addCreature(ENEMY, 10, 10);

        trapEffects.fire(trigger, KEEPER, TRAP_POSITION, target);

        assertEquals(0, damage(target));
    }

    @Test
    void freezeTrapFreezesItsTarget() {
        Trap freeze = trap((short) 7, Shot.ProcessType.FREEZE, Shot.DamageType.NONE, 20, 1);
        EntityId target = addCreature(ENEMY, 10, 10);
        EntityId bystander = addCreature(ENEMY, 10, 10);

        trapEffects.fire(freeze, KEEPER, TRAP_POSITION, target);

        Frozen frozen = entityData.getComponent(target, Frozen.class);
        assertEquals(5, frozen.startTime);
        assertEquals(20, frozen.duration);
        assertNull(entityData.getComponent(bystander, Frozen.class));
    }

    @Test
    void freezingAgainDoesNotProlongTheFreeze() {
        Trap freeze = trap((short) 7, Shot.ProcessType.FREEZE, Shot.DamageType.NONE, 20, 1);
        EntityId target = addCreature(ENEMY, 10, 10);
        entityData.setComponent(target, new Frozen(0, 20));

        trapEffects.fire(freeze, KEEPER, TRAP_POSITION, target);

        assertEquals(0, entityData.getComponent(target, Frozen.class).startTime);
    }

    @Test
    void hornedReaperDoesNotFreeze() {
        Trap freeze = trap((short) 7, Shot.ProcessType.FREEZE, Shot.DamageType.NONE, 20, 1);
        EntityId reaper = addCreature(ENEMY, 10, 10);
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = REAPER;
        entityData.setComponent(reaper, creatureComponent);

        trapEffects.fire(freeze, KEEPER, TRAP_POSITION, reaper);

        assertNull(entityData.getComponent(reaper, Frozen.class));
    }

    @Test
    void alarmTrapRaisesAnAlarmForItsOwnerAtTheTrap() {
        Trap alarm = trap((short) 4, Shot.ProcessType.ALARM, Shot.DamageType.NONE, 0, 5);
        EntityId target = addCreature(ENEMY, 12, 10);

        trapEffects.fire(alarm, KEEPER, TRAP_POSITION, target);

        EntitySet alarms = entityData.getEntities(Alarm.class, Owner.class, Position.class, Decay.class);
        assertEquals(1, alarms.size());
        EntityId alarmId = alarms.iterator().next().getId();
        assertEquals(KEEPER, entityData.getComponent(alarmId, Owner.class).ownerId);
        assertEquals(TRAP_POSITION, entityData.getComponent(alarmId, Position.class).position);
        assertEquals(5, entityData.getComponent(alarmId, Decay.class).startTime);
        assertEquals(50, entityData.getComponent(alarmId, Decay.class).duration);
        assertEquals(0, damage(target));
    }

    @Test
    void alarmIsNotRaisedAgainWhileTheOwnersLastAlarmLasts() {
        Trap alarm = trap((short) 4, Shot.ProcessType.ALARM, Shot.DamageType.NONE, 0, 5);
        EntityId target = addCreature(ENEMY, 12, 10);

        trapEffects.fire(alarm, KEEPER, TRAP_POSITION, target);
        gameTime = 20;
        trapEffects.fire(alarm, KEEPER, new Vector3f(20, WorldUtils.FLOOR_HEIGHT, 20), target);

        EntitySet alarms = entityData.getEntities(Alarm.class, Owner.class);
        assertEquals(1, alarms.size());
        assertEquals(TRAP_POSITION, entityData.getComponent(alarms.iterator().next().getId(), Position.class).position);
    }

    @Test
    void anotherKeepersAlarmDoesNotHoldBackTheAlarm() {
        Trap alarm = trap((short) 4, Shot.ProcessType.ALARM, Shot.DamageType.NONE, 0, 5);
        trapEffects.fire(alarm, ENEMY, TRAP_POSITION, addCreature(KEEPER, 12, 10));

        trapEffects.fire(alarm, KEEPER, TRAP_POSITION, addCreature(ENEMY, 12, 10));

        assertEquals(2, entityData.getEntities(Alarm.class, Owner.class).size());
    }

    private Trap trap(short trapId, Shot.ProcessType processType, Shot.DamageType damageType, int shotData1, int shotData2) {
        short shotId = (short) (trapId + 100);
        shots.put(shotId, TrapFiringFixtures.shot(shotId, processType, damageType));
        return TrapFiringFixtures.withShotData(TrapFiringFixtures.trap(trapId, Trap.TriggerType.PRESSURE, 1, 4, shotId),
                shotData1, shotData2);
    }

    private EntityId addCreature(short ownerId, float x, float y) {
        EntityId entityId = entityData.createEntity();
        CreatureComponent creatureComponent = new CreatureComponent();
        creatureComponent.creatureId = IMP;
        entityData.setComponent(entityId, creatureComponent);
        entityData.setComponent(entityId, new Owner(ownerId, ownerId));
        entityData.setComponent(entityId, new Health(100, 100));
        entityData.setComponent(entityId, new Position(0, new Vector3f(x, WorldUtils.FLOOR_HEIGHT, y)));
        return entityId;
    }

    private int damage(EntityId entityId) {
        Damage damage = entityData.getComponent(entityId, Damage.class);
        return damage != null ? damage.damage : 0;
    }

    private IKwdFile kwdFile() {
        return (IKwdFile) Proxy.newProxyInstance(IKwdFile.class.getClassLoader(), new Class<?>[]{IKwdFile.class},
                (proxy, method, args) -> switch (method.getName()) {
            case "getShotById" ->
                shots.get((Short) args[0]);
            case "getCreature" ->
                creatures.get((Short) args[0]);
            default ->
                throw new UnsupportedOperationException(method.getName());
        });
    }

}
