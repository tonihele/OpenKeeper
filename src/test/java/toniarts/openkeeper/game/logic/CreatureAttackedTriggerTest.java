/*
 * Copyright (C) 2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper. If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.logic;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import toniarts.openkeeper.game.component.*;
import toniarts.openkeeper.game.controller.*;
import toniarts.openkeeper.game.controller.creature.CreatureController;
import toniarts.openkeeper.game.controller.creature.CreatureState;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.event.GameEventService;
import toniarts.openkeeper.game.navigation.INavigationService;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerEventQueue;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureImprisonedFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import static org.junit.jupiter.api.Assertions.*;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;

class CreatureAttackedTriggerTest {

    @Test
    void realTargetSelectionQueuesTheVictimAfterStoringTheTargetBeforeAnyHit() {
        try(Fixture f=new Fixture(255,true)) {
            assertEquals(f.victim,f.controller(f.attacker).getAttackTarget().getEntityId());
            assertEquals(f.victim,f.lastNotified);
            assertEquals(f.victim,f.data.getComponent(f.attacker,AttackTarget.class).entityId);
            assertEquals(100,f.data.getComponent(f.victim,Health.class).health);
            assertNull(f.data.getComponent(f.victim,Damage.class));assertEquals(0,f.flags[0]);
            f.tick();assertEquals(1,f.flags[0]);assertEquals(0,f.flags[2]);
        }
    }

    @Test
    void retainedTargetQueriesDoNotRepeatSetupEvents() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.tick();
            for(int i=0;i<10;i++) {f.select();f.tick();}
            assertEquals(1,f.notifications);assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void actualMeleeHitsAgainstRetainedTargetDoNotEmitAdditionalSetupEvents() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.tick();
            f.controller(f.attacker).executeAttack(f.victim);
            f.controller(f.attacker).executeAttack(f.victim);
            assertEquals(10,f.data.getComponent(f.victim,Damage.class).damage);
            f.tick();assertEquals(1,f.flags[0]);assertEquals(1,f.notifications);
        }
    }

    @Test
    void aHitCanOccurBeforeQueuedSetupDelivery() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.controller(f.attacker).executeAttack(f.victim);
            assertEquals(5,f.data.getComponent(f.victim,Damage.class).damage);
            assertEquals(0,f.flags[0]);f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void rawDamageDoesNotCreateAnAttackSetup() {
        try(Fixture f=new Fixture(255,true)) {
            f.controller(f.attacker).setDamage(f.victim,9);f.tick();
            assertEquals(9,f.data.getComponent(f.victim,Damage.class).damage);
            assertEquals(0,f.flags[0]);assertEquals(0,f.notifications);
        }
    }

    @Test
    void reacquiringTheSameVictimCreatesASeparateEvent() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.data.removeComponent(f.attacker,AttackTarget.class);f.select();
            assertEquals(2,f.notifications);assertEquals(0,f.flags[0]);
            f.tick();assertEquals(2,f.flags[0]);
        }
    }

    @Test
    void invalidatingATargetNotifiesTheNewVictimOnly() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.tick();
            f.data.setComponent(f.victim,new Unconscious());f.sensed.add(f.otherVictim);
            assertEquals(f.otherVictim,f.controller(f.attacker).getAttackTarget().getEntityId());
            f.tick();assertEquals(1,f.flags[0]);assertEquals(1,f.flags[3]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"unconscious","dead","fleeing","imprisoned","tortured"})
    void unavailableVictimsDoNotCreateSetupEvents(String kind) {
        try(Fixture f=new Fixture(255,true)) {
            switch(kind) {
                case "unconscious" -> f.data.setComponent(f.victim,new Unconscious());
                case "dead" -> f.data.removeComponent(f.victim,Health.class);
                case "fleeing" -> f.data.setComponent(f.victim,new CreatureAi(0,CreatureState.FLEE,(short)6));
                case "imprisoned" -> f.data.setComponent(f.victim,new CreatureImprisoned(0,0));
                case "tortured" -> f.data.setComponent(f.victim,new CreatureTortured(0,0,0));
                default -> throw new AssertionError(kind);
            }
            assertNull(f.controller(f.attacker).getAttackTarget());f.tick();
            assertEquals(0,f.notifications);assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void noTargetClearsTheBindingWithoutNotifyingTheFormerVictim() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.tick();f.sensed.clear();f.data.setComponent(f.victim,new Unconscious());
            assertNull(f.controller(f.attacker).getAttackTarget());
            assertNull(f.data.getComponent(f.attacker,AttackTarget.class));
            f.tick();assertEquals(1,f.flags[0]);assertEquals(1,f.notifications);
        }
    }

    @Test
    void alliesDoNotCreateAttackSetupEvents() {
        try(Fixture f=new Fixture(255,true)) {
            f.data.setComponent(f.victim,new Owner((short)3,(short)3));
            assertNull(f.controller(f.attacker).getAttackTarget());f.tick();
            assertEquals(0,f.flags[0]);assertEquals(0,f.notifications);
        }
    }

    @Test
    void unreachableVictimsDoNotCreateSetupEvents() {
        try(Fixture f=new Fixture(255,true)) {
            f.data.setComponent(f.victim,new Position(0,new Vector3f(10,0,10)));
            assertNull(f.controller(f.attacker).getAttackTarget());f.tick();
            assertEquals(0,f.notifications);assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void twoAttackersSelectingTheSameVictimKeepBothNotifications() {
        try(Fixture f=new Fixture(255,true)) {
            EntityId second=f.creature((short)3,0);f.select();
            f.controller(second).getAttackTarget();f.tick();
            assertEquals(2,f.flags[0]);assertEquals(2,f.notifications);
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1","2,2","255,3"})
    void explicitReacquisitionsUseExistingBranchRepeatLimits(int repeats,int expected) {
        try(Fixture f=new Fixture(repeats,true)) {
            for(int i=0;i<3;i++) {f.data.removeComponent(f.attacker,AttackTarget.class);f.select();}
            assertEquals(3,f.notifications);f.tick();assertEquals(expected,f.flags[0]);
        }
    }

    @Test
    void targetSelectionOnAProducerThreadOnlyEnqueues() throws InterruptedException {
        try(Fixture f=new Fixture(255,true)) {
            AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread producer=new Thread(()->{try {f.select();} catch(Throwable t) {failure.set(t);}},"synthetic-attack-setup");
            producer.start();producer.join();
            assertNull(failure.get());assertEquals(0,f.flags[0]);assertEquals(1,f.notifications);
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void anAttackTargetComponentOrCombatStateAloneDoesNotPollAsAnEvent() {
        try(Fixture f=new Fixture(255,true)) {
            f.data.setComponent(f.attacker,new AttackTarget(f.victim));
            f.controller(f.attacker).getStateMachine().changeState(CreatureState.MELEE_ATTACK);
            for(int i=0;i<8;i++) f.tick();
            assertFalse(f.controller(f.victim).isAttacked());assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void earlyTargetSetupSurvivesUntilScriptControlsExist() {
        try(Fixture f=new Fixture(255,false)) {
            f.select();f.bindScripts();f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void mixedQueuedEventTypesRetainTheirOrder() {
        try(Fixture f=new Fixture(255,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_ATTACKED,-1,3,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,CREATURE_SLAPPED,-3,4,255));
            f.records.put(4,CreatureAngryFixtures.condition(4,CREATURE_STUNNED,-4,0,255));
            f.records.put(-3,CreatureAngryFixtures.action(-3,1));f.records.put(-4,CreatureAngryFixtures.action(-4,3));
            f.select();f.queue.enqueueSlapped(f.victim);f.queue.enqueueStunned(f.victim);
            f.bindScripts();f.tick();assertEquals(List.of(0,1,3),f.actionOrder);
        }
    }

    @Test
    void nestedConditionsObserveDeliveryTimeWithoutLatching() {
        try(Fixture f=new Fixture(255,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_ATTACKED,3,0,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,FLAG,-1,0,255));f.bindScripts();
            f.select();f.tick();assertEquals(0,f.flags[0]);
            f.flags[1]=1;for(int i=0;i<4;i++) f.tick();assertEquals(0,f.flags[0]);
            f.data.removeComponent(f.attacker,AttackTarget.class);f.select();f.flags[1]=0;
            f.tick();assertEquals(0,f.flags[0]);
            f.data.removeComponent(f.attacker,AttackTarget.class);f.select();f.flags[1]=1;
            f.tick();assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void nestedEventConditionsAreNotIndependentlyDispatched() {
        try(Fixture f=new Fixture(255,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,3,0,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,CREATURE_ATTACKED,-1,0,255));
            f.bindScripts();f.flags[1]=1;f.select();
            for(int i=0;i<5;i++) f.tick();assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void eventDeliveryDoesNotMoveTheOrdinaryTraversalCursor() {
        try(Fixture f=new Fixture(255,false)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,-3,3,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,CREATURE_ATTACKED,-1,0,255));
            f.records.put(-3,CreatureAngryFixtures.action(-3,2));f.bindScripts();
            f.flags[1]=1;f.tick();f.select();f.tick();
            assertEquals(1,f.flags[0]);assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void removedVictimsDiscardPendingEventsWithoutReplayingThemLater() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.data.removeComponent(f.victim,CreatureComponent.class);f.tick();
            CreatureComponent restored=new CreatureComponent();restored.creatureId=6;
            f.data.setComponent(f.victim,restored);f.tick();assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void stoppingClearsPendingTargetSetupEvents() {
        try(Fixture f=new Fixture(255,true)) {
            f.select();f.scripts.stop();f.tick();assertEquals(0,f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final Map<EntityId,CreatureController> controllers=new HashMap<>();
        final Set<EntityId> sensed=new LinkedHashSet<>();
        final Creature creature=CreatureImprisonedFixtures.creature();
        final EntityId attacker=creature((short)3,100),victim=creature((short)1,1),otherVictim=creature((short)1,200);
        final int[] flags=new int[4];
        final Thread simulation=Thread.currentThread();
        final List<Integer> actionOrder=new ArrayList<>();
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final CreatureTriggerEventQueue queue=new CreatureTriggerEventQueue();
        final IKwdFile kwd;
        final ILevelInfo level;
        final ICreaturesController creatures;
        final IEntityPositionLookup lookup;
        final INavigationService navigation;
        CreatureTriggerLogicController scripts;
        EntityId lastNotified;
        int notifications;

        Fixture(int repeats,boolean bind) {
            sensed.add(victim);
            records.put(1,CreatureAngryFixtures.condition(1,CREATURE_ATTACKED,-1,0,repeats));
            records.put(-1,CreatureAngryFixtures.action(-1,0));
            records.put(100,CreatureAngryFixtures.condition(100,CREATURE_ATTACKED,-100,0,255));
            records.put(-100,CreatureAngryFixtures.action(-100,2));
            records.put(200,CreatureAngryFixtures.condition(200,CREATURE_ATTACKED,-200,0,255));
            records.put(-200,CreatureAngryFixtures.action(-200,3));
            kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getTriggers" -> records;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getThings" -> args[0]==Thing.GoodCreature.class
                        ? List.of(CreatureAngryFixtures.thing(1),CreatureAngryFixtures.thing(100),CreatureAngryFixtures.thing(200)) : List.of();
                default -> throw new UnsupportedOperationException(name);
            });
            level=proxy(ILevelInfo.class,(name,args)->switch(name) {
                case "getLevelData" -> kwd;
                case "getPlayer" -> new Keeper(false,((Number)args[0]).shortValue());
                case "getFlag" -> {assertSame(simulation,Thread.currentThread());yield flags[((Number)args[0]).intValue()];}
                case "setFlag" -> {
                    assertSame(simulation,Thread.currentThread());int id=((Number)args[0]).intValue();
                    flags[id]=(int)args[1];actionOrder.add(id);yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            creatures=proxy(ICreaturesController.class,(name,args)->switch(name) {
                case "createController" -> controller((EntityId)args[0]);
                case "isValidEntity" -> data.getComponent((EntityId)args[0],CreatureComponent.class)!=null;
                case "onAttacked" -> {
                    EntityId id=(EntityId)args[0];
                    assertTrue(controllers.keySet().stream().anyMatch(actor->{
                        AttackTarget target=data.getComponent(actor,AttackTarget.class);
                        return target!=null && target.entityId.equals(id);
                    }));
                    lastNotified=id;notifications++;queue.enqueueAttacked(id);yield null;
                }
                default -> throw new UnsupportedOperationException(name);
            });
            lookup=proxy(IEntityPositionLookup.class,(name,args)->switch(name) {
                case "getSensedEntities" -> sensed;
                default -> throw new UnsupportedOperationException(name);
            });
            navigation=proxy(INavigationService.class,(name,args)->switch(name) {
                case "findPath" -> null;
                default -> throw new UnsupportedOperationException(name);
            });
            if(bind) bindScripts();
        }
        EntityId creature(short owner,int trigger) {
            EntityId id=data.createEntity();CreatureComponent component=new CreatureComponent();component.creatureId=6;
            data.setComponents(id,component,new Health(100,100),new Owner(owner,owner),
                    new CreatureAi(0,CreatureState.IDLE,(short)6),new Position(0,new Vector3f()),
                    new CreatureMeleeAttack(0,5,0,1));
            if(trigger!=0) data.setComponent(id,new Trigger(trigger));return id;
        }
        CreatureController controller(EntityId id) {
            return controllers.computeIfAbsent(id,key->new CreatureController(key,data,creature,navigation,null,()->20,
                    Map.of(),creatures,lookup,null,level,null,null,proxy(GameEventService.class,(name,args)->null),e->{ }));
        }
        void select() {assertNotNull(controller(attacker).getAttackTarget());}
        void bindScripts() {
            if(scripts!=null) scripts.stop();
            scripts=new CreatureTriggerLogicController(null,level,null,null,creatures,null,data,queue);
        }
        void tick() {scripts.processTick(.25f);}
        @Override public void close() {if(scripts!=null) scripts.stop();data.close();}
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->call.invoke(m.getName(),a)));
    }
}
