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

import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import toniarts.openkeeper.game.event.ServerEventDispatcher;
import toniarts.openkeeper.game.event.ServerEvents;
import toniarts.openkeeper.game.event.ServerEvents.CreatureEvent;
import toniarts.openkeeper.game.component.CreatureComponent;
import toniarts.openkeeper.game.component.CreatureMood;
import toniarts.openkeeper.game.component.Health;
import toniarts.openkeeper.game.component.ObjectComponent;
import toniarts.openkeeper.game.component.Owner;
import toniarts.openkeeper.game.component.Slapped;
import toniarts.openkeeper.game.component.Trigger;
import toniarts.openkeeper.game.controller.ICreaturesController;
import toniarts.openkeeper.game.controller.ILevelInfo;
import toniarts.openkeeper.game.controller.IPlayerController;
import toniarts.openkeeper.game.controller.creature.ICreatureController;
import toniarts.openkeeper.game.controller.player.PlayerStatsControl;
import toniarts.openkeeper.game.data.Keeper;
import toniarts.openkeeper.game.trigger.creature.CreatureTriggerLogicController;
import toniarts.openkeeper.tools.convert.map.Creature;
import toniarts.openkeeper.tools.convert.map.CreatureAngryFixtures;
import toniarts.openkeeper.tools.convert.map.CreatureSleepFixtures;
import toniarts.openkeeper.tools.convert.map.IKwdFile;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.tools.convert.map.Variable.MiscVariable.MiscType;
import static toniarts.openkeeper.tools.convert.map.TriggerGeneric.TargetType.*;
import static org.junit.jupiter.api.Assertions.*;

class CreatureSlappedTriggerTest {

    @Test
    void acceptedSlapIsQueuedUntilTheSimulationTickAndNeverLatched() {
        try (Fixture f=new Fixture(255)) {
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            assertEquals(0,f.flags[0]);
            f.tick();
            assertEquals(1,f.flags[0]);
            for (int i=0;i<10;i++) f.tick();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void lingeringSlapComponentAloneNeverEmits() {
        try (Fixture f=new Fixture(255)) {
            f.data.setComponent(f.entity,new Slapped(1));
            for (int i=0;i<10;i++) f.tick();
            assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void twoAcceptedRequestsWithTheSameTimestampRemainTwoEvents() {
        try (Fixture f=new Fixture(255)) {
            for (int i=0;i<2;i++) {
                f.data.setComponent(f.entity,new Slapped(1));
                f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            }
            f.tick();
            assertEquals(2,f.flags[0]);
        }
    }

    @Test
    void removingTheTemporaryEffectDoesNotEraseAnAcceptedEvent() {
        try (Fixture f=new Fixture(255)) {
            f.data.setComponent(f.entity,new Slapped(1));
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.data.removeComponent(f.entity,Slapped.class);
            f.tick();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void realSlapEffectProcessingDoesNotDuplicateScriptDelivery() {
        try (Fixture f=new Fixture(255)) {
            PlayerStatsControl stats=new PlayerStatsControl();
            IPlayerController player=proxy(IPlayerController.class,(name,args)->switch(name) {
                case "getKeeper" -> new Keeper(false,(short)3);
                case "getStatsControl" -> stats;
                default -> throw new UnsupportedOperationException(name);
            });
            var settings=Map.of(MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS,
                    CreatureSleepFixtures.variable(MiscType.INCREASED_WORK_RATE_DURATION_FROM_SLAPPING_SECONDS,5));
            SlapSystem slaps=new SlapSystem(f.data,f.kwd,List.of(player),settings,f.events);
            try {
                f.data.setComponent(f.entity,new Slapped(0));
                f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
                slaps.processTick(.25f);
                f.tick();
                assertEquals(1,f.flags[0]);
                assertTrue(stats.hasSlapped());
                assertNotNull(f.data.getComponent(f.entity,Slapped.class));
                for (int i=0;i<10;i++) {slaps.processTick(.25f);f.tick();}
                assertEquals(1,f.flags[0]);
            } finally {slaps.stop();}
        }
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "255,3"})
    void queuedSlapsUseExistingRepeatLimits(int repeats,int events) {
        try (Fixture f=new Fixture(repeats)) {
            for (int i=0;i<3;i++) f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.tick();
            assertEquals(events,f.flags[0]);
        }
    }

    @Test
    void allMatchingRootBranchesRunForEachDeliveredSlap() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_SLAPPED,-1,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_SLAPPED,-2,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.tick();
            assertEquals(1,f.flags[0]);assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void nestedConditionsObserveDeliveryTimeAndDoNotLatchARejectedEvent() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_SLAPPED,2,0,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,FLAG,-1,0,255));
            f.bindScripts();
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.tick();
            f.flags[1]=1;
            for (int i=0;i<5;i++) f.tick();
            assertEquals(0,f.flags[0]);
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.tick();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void stateChangedBeforeDeliveryIsVisibleToTheNestedCondition() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,CREATURE_SLAPPED,2,0,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,FLAG,-1,0,255));
            f.bindScripts();
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.flags[1]=1;
            f.tick();
            assertEquals(1,f.flags[0]);
        }
    }

    @Test
    void unrelatedAndNestedEventsDoNotReceiveTheSlap() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,2,3,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_SLAPPED,-1,0,255));
            f.records.put(3,CreatureAngryFixtures.condition(3,CREATURE_ANGRY,-2,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();f.flags[1]=1;
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            for (int i=0;i<6;i++) f.tick();
            assertEquals(0,f.flags[0]);assertEquals(0,f.flags[2]);
        }
    }

    @Test
    void eventDoesNotMoveTheOrdinaryTraversalCursor() {
        try (Fixture f=new Fixture(255)) {
            f.records.put(1,CreatureAngryFixtures.condition(1,FLAG,-2,2,255));
            f.records.put(2,CreatureAngryFixtures.condition(2,CREATURE_SLAPPED,-1,0,255));
            f.records.put(-2,CreatureAngryFixtures.action(-2,2));
            f.bindScripts();f.flags[1]=1;
            f.tick();
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.tick();
            assertEquals(1,f.flags[0]);assertEquals(1,f.flags[2]);
        }
    }

    @Test
    void queueProducerDoesNotTouchScriptStateOnTheSessionThread() throws InterruptedException {
        try (Fixture f=new Fixture(255)) {
            AtomicReference<Throwable> failure=new AtomicReference<>();
            Thread session=new Thread(()->{
                try {for (int i=0;i<20;i++) f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));}
                catch(Throwable t) {failure.set(t);}
            },"synthetic-hand-session");
            session.start();session.join();
            assertNull(failure.get());assertEquals(0,f.flags[0]);
            f.tick();assertEquals(20,f.flags[0]);
        }
    }

    @Test
    void unboundCreaturesAndObjectsDoNotRunACreatureScript() {
        try (Fixture f=new Fixture(255)) {
            EntityId object=f.data.createEntity();
            f.data.setComponents(object,new ObjectComponent((short)1,null),new Trigger(1));
            EntityId unbound=f.data.createEntity();
            f.data.setComponent(unbound,new CreatureComponent());
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(object));f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(unbound));
            f.tick();assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void stopDiscardsPendingSlaps() {
        try (Fixture f=new Fixture(255)) {
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));f.events.close();f.scripts.stop();
            f.tick();assertEquals(0,f.flags[0]);
        }
    }

    @Test
    void stoppedScriptConsumerReceivesNoFurtherEvents() {
        try (Fixture f = new Fixture(255)) {
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.scripts.stop();
            f.events.processTick(.25f);
            assertEquals(0, f.flags[0]);
        }
    }

    @Test
    void bufferedInputSurvivesUntilScriptConsumersAreRegistered() {
        try (Fixture f = new Fixture(255)) {
            f.scripts.stop();
            f.events.enqueue(ServerEvents.CREATURE_SLAPPED, new CreatureEvent(f.entity));
            f.bindScripts();
            f.tick();
            assertEquals(1, f.flags[0]);
        }
    }

    private static final class Fixture implements AutoCloseable {
        final EntityData data=new DefaultEntityData();
        final EntityId entity=data.createEntity();
        final int[] flags=new int[3];
        final Thread simulation=Thread.currentThread();
        final Map<Integer,toniarts.openkeeper.tools.convert.map.Trigger> records=new HashMap<>();
        final Creature creature=CreatureAngryFixtures.creature(false);
        final IKwdFile kwd;
        final ILevelInfo level;
        final ICreaturesController creatures;
        final ServerEventDispatcher events = new ServerEventDispatcher();
        CreatureTriggerLogicController scripts;

        Fixture(int repeats) {
            records.put(1,CreatureAngryFixtures.condition(1,CREATURE_SLAPPED,-1,0,repeats));
            records.put(-1,CreatureAngryFixtures.action(-1,0));
            CreatureComponent component=new CreatureComponent();component.creatureId=6;
            data.setComponents(entity,component,new Trigger(1),new Health(100,100),
                    new Owner((short)3,(short)3),new CreatureMood(0,0,0,0,0,0));
            kwd=proxy(IKwdFile.class,(name,args)->switch(name) {
                case "getTriggers" -> records;
                case "getCreature" -> creature;
                case "getGameLevel" -> CreatureSleepFixtures.level(4);
                case "getThings" -> args[0]==Thing.GoodCreature.class
                        ? List.of(CreatureAngryFixtures.thing(1)) : List.of();
                default -> throw new UnsupportedOperationException(name);
            });
            level=proxy(ILevelInfo.class,(name,args)->{
                assertSame(simulation,Thread.currentThread());
                return switch(name) {
                    case "getLevelData" -> kwd;
                    case "getFlag" -> flags[((Number)args[0]).intValue()];
                    case "setFlag" -> {flags[((Number)args[0]).intValue()]=(int)args[1];yield null;}
                    default -> throw new UnsupportedOperationException(name);
                };
            });
            creatures=proxy(ICreaturesController.class,(name,args)->switch(name) {
                case "createController" -> proxy(ICreatureController.class,(method,a)->switch(method) {
                    case "getEntityId" -> args[0];
                    default -> throw new UnsupportedOperationException(method);
                });
                default -> throw new UnsupportedOperationException(name);
            });
            bindScripts();
        }
        void bindScripts() {
            if(scripts!=null) scripts.stop();
            scripts=new CreatureTriggerLogicController(null,level,null,null,creatures,null,data,events);
            events.start();
        }
        void tick() {events.processTick(.25f);scripts.processTick(.25f);}
        @Override public void close() {events.close();scripts.stop();data.close();}
    }

    @FunctionalInterface private interface Call {Object invoke(String name,Object[] args);}
    private static <T>T proxy(Class<T> type,Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (p,m,a)->call.invoke(m.getName(),a)));
    }
}
